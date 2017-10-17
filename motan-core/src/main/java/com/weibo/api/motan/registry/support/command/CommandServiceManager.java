/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.registry.support.command;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.*;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;



public class CommandServiceManager implements CommandListener, ServiceListener {

    public static final String MOTAN_COMMAND_SWITCHER = "feature.motanrpc.command.enable";
    private static Pattern IP_PATTERN = Pattern.compile("^!?[0-9.]*\\*?$");

    static {
        MotanSwitcherUtil.initSwitcher(MOTAN_COMMAND_SWITCHER, true);
    }

    private URL refUrl;
    private ConcurrentHashSet<NotifyListener> notifySet;
    private CommandFailbackRegistry registry;
    // service cache
    private Map<String, List<URL>> groupServiceCache;
    // command cache
    private String commandStringCache = "";
    private volatile RpcCommand commandCache;

    public CommandServiceManager(URL refUrl) {
        LoggerUtil.info("CommandServiceManager init url:" + refUrl.toFullStr());
        this.refUrl = refUrl;
        notifySet = new ConcurrentHashSet<NotifyListener>();
        groupServiceCache = new ConcurrentHashMap<String, List<URL>>();

    }

    @Override
    public void notifyService(URL serviceUrl, URL registryUrl, List<URL> urls) {

        if (registry == null) {
            throw new MotanFrameworkException("registry must be set.");
        }

        URL urlCopy = serviceUrl.createCopy();
        String groupName = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        groupServiceCache.put(groupName, urls);

        List<URL> finalResult = new ArrayList<URL>();
        if (commandCache != null) {
            Map<String, Integer> weights = new HashMap<String, Integer>();
            finalResult = discoverServiceWithCommand(refUrl, weights, commandCache);
        } else {
            LoggerUtil.info("command cache is null. service:" + serviceUrl.toSimpleString());
            // 没有命令时，只返回这个manager实际group对应的结果
            finalResult.addAll(discoverOneGroup(refUrl));
        }

        for (NotifyListener notifyListener : notifySet) {
            notifyListener.notify(registry.getUrl(), finalResult);
        }

    }

    @Override
    public void notifyCommand(URL serviceUrl, String commandString) {
        LoggerUtil.info("CommandServiceManager notify command. service:" + serviceUrl.toSimpleString() + ", command:" + commandString);

        if (!MotanSwitcherUtil.isOpen(MOTAN_COMMAND_SWITCHER) || commandString == null) {
            LoggerUtil.info("command reset empty since swither is close.");
            commandString = "";
        }

        List<URL> finalResult = new ArrayList<URL>();
        URL urlCopy = serviceUrl.createCopy();

        if (!StringUtils.equals(commandString, commandStringCache)) {
            commandStringCache = commandString;
            commandCache = RpcCommandUtil.stringToCommand(commandStringCache);
            Map<String, Integer> weights = new HashMap<String, Integer>();

            if (commandCache != null && commandCache.getClientCommandList() != null && !commandCache.getClientCommandList().isEmpty()) {
                commandCache.sort();
                finalResult = discoverServiceWithCommand(refUrl, weights, commandCache);
            } else {
                // 如果是指令有异常时，应当按没有指令处理，防止错误指令导致服务异常
                if (StringUtils.isNotBlank(commandString)) {
                    LoggerUtil.warn("command parse fail, ignored! command:" + commandString);
                    commandString = "";
                }
                // 没有命令时，只返回这个manager实际group对应的结果
                finalResult.addAll(discoverOneGroup(refUrl));

            }

            // 指令变化时，删除不再有效的缓存，取消订阅不再有效的group
            Set<String> groupKeys = groupServiceCache.keySet();
            for (String gk : groupKeys) {
                if (!weights.containsKey(gk)) {
                    groupServiceCache.remove(gk);
                    URL urlTemp = urlCopy.createCopy();
                    urlTemp.addParameter(URLParamType.group.getName(), gk);
                    registry.unsubscribeService(urlTemp, this);
                }
            }
            // 当指令从有改到无时，或者没有流量切换指令时，会触发取消订阅所有的group，需要重新订阅本组的service
            if ("".equals(commandString) || weights.isEmpty()) {
                LoggerUtil.info("reSub service" + refUrl.toSimpleString());
                registry.subscribeService(refUrl, this);
            }
        } else {
            LoggerUtil.info("command not change. url:" + serviceUrl.toSimpleString());
            // 指令没有变化，什么也不做
            return;
        }

        for (NotifyListener notifyListener : notifySet) {
            notifyListener.notify(registry.getUrl(), finalResult);
        }
    }

    public List<URL> discoverServiceWithCommand(URL serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand) {
        String localIP = NetUtils.getLocalAddress().getHostAddress();
        return this.discoverServiceWithCommand(serviceUrl, weights, rpcCommand, localIP);
    }

    public List<URL> discoverServiceWithCommand(URL serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand, String localIP) {
        if (rpcCommand == null || CollectionUtil.isEmpty(rpcCommand.getClientCommandList())) {
            return discoverOneGroup(serviceUrl);
        }

        List<URL> mergedResult = new LinkedList<URL>();
        String path = serviceUrl.getPath();

        List<RpcCommand.ClientCommand> clientCommandList = rpcCommand.getClientCommandList();
        boolean hit = false;
        for (RpcCommand.ClientCommand command : clientCommandList) {
            mergedResult = new LinkedList<URL>();
            // 判断当前url是否符合过滤条件
            boolean match = RpcCommandUtil.match(command.getPattern(), path);
            if (match) {
                hit = true;
                if (!CollectionUtil.isEmpty(command.getMergeGroups())) {
                    // 计算出所有要合并的分组及权重
                    try {
                        buildWeightsMap(weights, command);
                    } catch (MotanFrameworkException e) {
                        LoggerUtil.warn("build weights map fail!" + e.getMessage());
                        continue;
                    }
                    // 根据计算结果，分别发现各个group的service，合并结果
                    mergedResult.addAll(mergeResult(serviceUrl, weights));
                } else {
                    mergedResult.addAll(discoverOneGroup(serviceUrl));
                }

                LoggerUtil.info("mergedResult: size-" + mergedResult.size() + " --- " + mergedResult.toString());

                if (!CollectionUtil.isEmpty(command.getRouteRules())) {
                    LoggerUtil.info("router: " + command.getRouteRules().toString());

                    for (String routeRule : command.getRouteRules()) {
                        String[] fromTo = routeRule.replaceAll("\\s+", "").split("to");

                        if (fromTo.length != 2) {
                            routeRuleConfigError();
                            continue;
                        }
                        String from = fromTo[0];
                        String to = fromTo[1];
                        if (from.length() < 1 || to.length() < 1 || !IP_PATTERN.matcher(from).find() || !IP_PATTERN.matcher(to).find()) {
                            routeRuleConfigError();
                            continue;
                        }
                        boolean oppositeFrom = from.startsWith("!");
                        boolean oppositeTo = to.startsWith("!");
                        if (oppositeFrom) {
                            from = from.substring(1);
                        }
                        if (oppositeTo) {
                            to = to.substring(1);
                        }
                        int idx = from.indexOf('*');
                        boolean matchFrom;
                        if (idx != -1) {
                            matchFrom = localIP.startsWith(from.substring(0, idx));
                        } else {
                            matchFrom = localIP.equals(from);
                        }

                        // 开头有!，取反
                        if (oppositeFrom) {
                            matchFrom = !matchFrom;
                        }
                        LoggerUtil.info("matchFrom: " + matchFrom + ", localip:" + localIP + ", from:" + from);
                        if (matchFrom) {
                            boolean matchTo;
                            Iterator<URL> iterator = mergedResult.iterator();
                            while (iterator.hasNext()) {
                                URL url = iterator.next();
                                if (url.getProtocol().equalsIgnoreCase("rule")) {
                                    continue;
                                }
                                idx = to.indexOf('*');
                                if (idx != -1) {
                                    matchTo = url.getHost().startsWith(to.substring(0, idx));
                                } else {
                                    matchTo = url.getHost().equals(to);
                                }
                                if (oppositeTo) {
                                    matchTo = !matchTo;
                                }
                                if (!matchTo) {
                                    iterator.remove();
                                    LoggerUtil.info("router To not match. url remove : " + url.toSimpleString());
                                }
                            }
                        }
                    }
                }
                // 只取第一个匹配的 TODO 考虑是否能满足绝大多数场景需求
                break;
            }
        }

        List<URL> finalResult = new ArrayList<URL>();
        if (!hit) {
            finalResult = discoverOneGroup(serviceUrl);
        } else {
            finalResult.addAll(mergedResult);
        }
        return finalResult;
    }

    private void buildWeightsMap(Map<String, Integer> weights, RpcCommand.ClientCommand command) {
        for (String rule : command.getMergeGroups()) {
            String[] gw = rule.split(":");
            int weight = 1;
            if (gw.length > 1) {
                try {
                    weight = Integer.parseInt(gw[1]);
                } catch (NumberFormatException e) {
                    weightConfigError();
                }
                if (weight < 0 || weight > 100) {
                    weightConfigError();
                }
            }
            weights.put(gw[0], weight);
        }
    }

    private List<URL> mergeResult(URL url, Map<String, Integer> weights) {
        List<URL> finalResult = new ArrayList<URL>();

        if (weights.size() > 1) {
            // 将所有group及权重拼接成一个rule的URL，并作为第一个元素添加到最终结果中
            URL ruleUrl = new URL("rule", url.getHost(), url.getPort(), url.getPath());
            StringBuilder weightsBuilder = new StringBuilder(64);
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                weightsBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
            ruleUrl.addParameter(URLParamType.weights.getName(), weightsBuilder.deleteCharAt(weightsBuilder.length() - 1).toString());
            finalResult.add(ruleUrl);
        }

        for (String key : weights.keySet()) {
            if (groupServiceCache.containsKey(key)) {
                finalResult.addAll(groupServiceCache.get(key));
            } else {
                URL urlTemp = url.createCopy();
                urlTemp.addParameter(URLParamType.group.getName(), key);
                finalResult.addAll(discoverOneGroup(urlTemp));
                registry.subscribeService(urlTemp, this);
            }
        }
        return finalResult;
    }

    private List<URL> discoverOneGroup(URL urlCopy) {
        LoggerUtil.info("CommandServiceManager discover one group. url:" + urlCopy.toSimpleString());
        String group = urlCopy.getParameter(URLParamType.group.getName(), URLParamType.group.getValue());
        List<URL> list = groupServiceCache.get(group);
        if (list == null) {
            list = registry.discoverService(urlCopy);
            groupServiceCache.put(group, list);
        }
        return list;
    }

    public void setCommandCache(String command) {
        commandStringCache = command;
        commandCache = RpcCommandUtil.stringToCommand(commandStringCache);
        LoggerUtil.info("CommandServiceManager set commandcache. commandstring:" + commandStringCache + ", comandcache "
                + (commandCache == null ? "is null." : "is not null."));
    }

    public void addNotifyListener(NotifyListener notifyListener) {
        notifySet.add(notifyListener);
    }

    public void removeNotifyListener(NotifyListener notifyListener) {
        notifySet.remove(notifyListener);
    }

    public void setRegistry(CommandFailbackRegistry registry) {
        this.registry = registry;
    }

    private void weightConfigError() {
        throw new MotanFrameworkException("权重比只能是[0,100]间的整数");
    }

    private void routeRuleConfigError() {
        LoggerUtil.warn("路由规则配置不合法");
    }

}
