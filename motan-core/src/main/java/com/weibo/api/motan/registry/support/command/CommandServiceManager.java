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

import com.weibo.api.motan.common.MotanConstants;
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
    private static int DEFAULT_WEIGHT = 1;
    private static int MAX_WEIGHT = 100;

    static {
        MotanSwitcherUtil.initSwitcher(MOTAN_COMMAND_SWITCHER, true);
    }

    private URL refUrl;
    private ConcurrentHashSet<NotifyListener> notifySet;
    private CommandFailbackRegistry registry;

    // service cache
    private Map<String, List<URL>> groupServiceCache;
    // command cache，保存最近一次指令通知的内容（有可能是无效指令串），仅用来判断新指令是否与上次指令内容相同。
    private String commandStringCache = "";
    private volatile RpcCommand commandCache;
    private RpcCommand staticCommand;
    private Map<String, Integer> weights;

    public CommandServiceManager(URL refUrl) {
        LoggerUtil.info("CommandServiceManager init url:" + refUrl.toFullStr());
        this.refUrl = refUrl;
        notifySet = new ConcurrentHashSet<>();
        groupServiceCache = new ConcurrentHashMap<>();
        weights = new ConcurrentHashMap<>();
        // 从url里处理静态指令。仅处理流控指令
        String mixGroupsString = refUrl.getParameter(URLParamType.mixGroups.getName());
        if (StringUtils.isNotBlank(mixGroupsString)) {
            LoggerUtil.info("CommandServiceManager process mixGroups:" + mixGroupsString);
            List<String> mergeGroups = new ArrayList<>();
            mergeGroups.add(refUrl.getGroup());
            String[] groups = mixGroupsString.split(MotanConstants.COMMA_SEPARATOR);
            for (String group : groups) {
                if (refUrl.getGroup().equals(group.trim())) {
                    continue;
                }
                mergeGroups.add(group.trim());
            }
            if (mergeGroups.size() > 1) { //有除自身group之外要混打的分组
                staticCommand = new RpcCommand();
                List<RpcCommand.ClientCommand> clientCommandList = new ArrayList<>();
                RpcCommand.ClientCommand clientCommand = new RpcCommand.ClientCommand();
                clientCommand.setPattern(refUrl.getPath());
                clientCommand.setCommandType(0);
                clientCommand.setIndex(1);
                clientCommand.setMergeGroups(mergeGroups);
                clientCommand.setRemark("static command of mix groups");
                clientCommand.setVersion("1.0");
                clientCommandList.add(clientCommand);
                staticCommand.setClientCommandList(clientCommandList);
                LoggerUtil.info("set static command. url: " + refUrl.toSimpleString() + ", merge group: " + mergeGroups);
            }

        }
    }

    @Override
    public void notifyService(URL serviceUrl, URL registryUrl, List<URL> urls) {
        if (registry == null) {
            throw new MotanFrameworkException("registry must be set.");
        }
        // 更新对应分组节点缓存
        groupServiceCache.put(serviceUrl.getGroup(), urls);
        notifyListeners();
    }

    @Override
    // 仅处理流控指令
    synchronized public void notifyCommand(URL serviceUrl, String commandString) {
        LoggerUtil.info("CommandServiceManager notify command. service:" + serviceUrl.toSimpleString() + ", command:" + commandString);

        if (!MotanSwitcherUtil.isOpen(MOTAN_COMMAND_SWITCHER) || commandString == null) {
            LoggerUtil.info("command reset empty since switcher is close.");
            commandString = ""; // 降级状态下相当于清空了所有动态指令
        }

        if (!StringUtils.equals(commandString, commandStringCache)) {
            commandStringCache = commandString;
            commandCache = RpcCommandUtil.stringToCommand(commandString);
            if (commandCache == null && StringUtils.isNotBlank(commandString)) {
                LoggerUtil.warn("command parse fail, ignored! command:" + commandString);
            }

            notifyListeners();

            // 指令变化时，删除不再有效的缓存，取消订阅不再有效的group
            Set<String> groupKeys = groupServiceCache.keySet();
            for (String gk : groupKeys) {
                if (!weights.containsKey(gk)) {
                    groupServiceCache.remove(gk);
                    URL urlTemp = refUrl.createCopy();
                    urlTemp.addParameter(URLParamType.group.getName(), gk);
                    registry.unsubscribeService(urlTemp, this);
                }
            }
            // 当指令从有改到无时，或者没有流量切换指令时，会触发取消订阅所有的group，需要重新订阅本组的service
            // 缓存中有或者没有节点并不能代表服务是否订阅
            if (commandCache == null || weights.isEmpty()) {
                LoggerUtil.info("reSub service" + refUrl.toSimpleString());
                registry.subscribeService(refUrl, this);
                discoverOneGroup(refUrl);// 缓存如果没有则更新
            }
        } else {
            LoggerUtil.info("command not change. url:" + serviceUrl.toSimpleString());
        }
    }

    synchronized private void notifyListeners() {
        Map<String, Integer> tempWeights = new ConcurrentHashMap<>();
        List<URL> finalResult = discoverServiceWithCommand(tempWeights, commandCache);
        weights = tempWeights;

        for (NotifyListener notifyListener : notifySet) {
            try {
                notifyListener.notify(registry.getUrl(), finalResult);
            } catch (Exception e) {
                LoggerUtil.error("CommandServiceManager notify listener fail. listener:" + notifyListener.toString(), e);
            }
        }
    }

    List<URL> discoverServiceWithCommand(Map<String, Integer> weights, RpcCommand rpcCommand) {
        String localIP = NetUtils.getLocalAddress().getHostAddress();
        return this.discoverServiceWithCommand(weights, rpcCommand, localIP);
    }

    List<URL> discoverServiceWithCommand(Map<String, Integer> weights, RpcCommand rpcCommand, String localIP) {
        List<URL> mergedResult = new LinkedList<>();
        boolean hit;
        // 优先处理动态指令
        if (rpcCommand != null && !CollectionUtil.isEmpty(rpcCommand.getClientCommandList())) {
            for (RpcCommand.ClientCommand command : rpcCommand.getClientCommandList()) {
                hit = processTrafficCommand(command, weights, localIP, mergedResult);
                if (hit) { //仅支持一条流量指令，指令生效就返回结果
                    LoggerUtil.info("discoverServiceWithCommand: hit with dynamic command. result size: " + mergedResult.size() + ", remark: " + command.getRemark());
                    return mergedResult;
                }
            }
        }

        // 动态指令无效时，静态指令生效
        if (staticCommand != null) {
            for (RpcCommand.ClientCommand command : staticCommand.getClientCommandList()) {
                hit = processTrafficCommand(command, weights, localIP, mergedResult);
                if (hit) {
                    LoggerUtil.info("discoverServiceWithCommand: hit with static command. result size: " + mergedResult.size() + ", remark: " + command.getRemark());
                    return mergedResult;
                }
            }
        }
        // 未名中流量指令时，返回默认分组结果
        LoggerUtil.info("discoverServiceWithCommand: not hit any command.");
        return discoverOneGroup(refUrl);
    }

    private boolean processTrafficCommand(RpcCommand.ClientCommand command, Map<String, Integer> weights, String localIP, List<URL> mergedResult) {
        boolean hit = false;
        if (command.getCommandType() == null || command.getCommandType() == 0) { //只处理流控指令。未指定类型时默认作为流控指令处理
            String path = refUrl.getPath();
            // 判断当前url是否符合过滤条件
            boolean match = RpcCommandUtil.match(command.getPattern(), path);
            if (match) {
                hit = true;
                if (!CollectionUtil.isEmpty(command.getMergeGroups())) {
                    boolean isMixMode;
                    // 计算出所有要合并的分组及权重
                    try {
                        isMixMode = buildWeightsMap(weights, command);
                    } catch (MotanFrameworkException e) {
                        LoggerUtil.warn("build weights map fail!" + e.getMessage());
                        weights.clear();//权重计算异常时，需要清空已记录的权重，避免发生不可预期的流量配比
                        return false;
                    }
                    // 根据计算结果，分别发现各个group的service，合并结果
                    mergedResult.addAll(mergeResult(refUrl, weights, isMixMode));
                } else {
                    mergedResult.addAll(discoverOneGroup(refUrl));
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
                        LoggerUtil.info("matchFrom: " + matchFrom + ", local ip:" + localIP + ", from:" + from);
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
            }
        }
        return hit;
    }

    private boolean buildWeightsMap(Map<String, Integer> weights, RpcCommand.ClientCommand command) {
        // 所有group都未指定权重时，使用mix模式，即不区分分组权重，所有节点流量混打
        boolean isMixMode = true;
        for (String rule : command.getMergeGroups()) {
            String[] gw = rule.split(":");
            int weight = DEFAULT_WEIGHT;
            if (gw.length > 1) {
                isMixMode = false;
                try {
                    weight = Integer.parseInt(gw[1]);
                } catch (NumberFormatException e) {
                    LoggerUtil.warn("parse weight fail, default weight 1 will be used. weight string : " + rule);
                }
                if (weight < DEFAULT_WEIGHT) {
                    weight = DEFAULT_WEIGHT;
                } else if (weight > MAX_WEIGHT) {
                    weight = MAX_WEIGHT;
                }
            }
            weights.put(gw[0], weight);
        }
        return isMixMode;
    }

    private List<URL> mergeResult(URL url, Map<String, Integer> weights, boolean isMixMode) {
        List<URL> finalResult = new ArrayList<>();

        if (!isMixMode && weights.size() > 1) { // 非混合模式生成权重规则URL
            // 将所有group及权重拼接成一个rule的URL，并作为第一个元素添加到最终结果中
            URL ruleUrl = new URL("rule", url.getHost(), url.getPort(), url.getPath());
            StringBuilder weightsBuilder = new StringBuilder(64);
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                weightsBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
            ruleUrl.addParameter(URLParamType.weights.getName(), weightsBuilder.deleteCharAt(weightsBuilder.length() - 1).toString());
            finalResult.add(ruleUrl);
            LoggerUtil.info("add weight rule url. weight: " + weightsBuilder.toString());
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
        return groupServiceCache.computeIfAbsent(urlCopy.getGroup(), k -> registry.discoverService(urlCopy));
    }

    void setCommandCache(String command) {
        commandStringCache = command;
        commandCache = RpcCommandUtil.stringToCommand(commandStringCache);
        LoggerUtil.info("CommandServiceManager set command cache. command string:" + commandStringCache + ", command cache "
                + (commandCache == null ? "is null." : "is not null."));
    }

    void addNotifyListener(NotifyListener notifyListener) {
        notifySet.add(notifyListener);
    }

    void removeNotifyListener(NotifyListener notifyListener) {
        notifySet.remove(notifyListener);
    }

    public void setRegistry(CommandFailbackRegistry registry) {
        this.registry = registry;
    }

    private void routeRuleConfigError() {
        LoggerUtil.warn("路由规则配置不合法");
    }

    // for test
    RpcCommand getStaticCommand() {
        return staticCommand;
    }

    Map<String, List<URL>> getGroupServiceCache() {
        return groupServiceCache;
    }

    RpcCommand getCommandCache() {
        return commandCache;
    }

    CommandFailbackRegistry getRegistry() {
        return registry;
    }
}
