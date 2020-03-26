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

package com.weibo.api.motan.cluster.support;

import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.cluster.HaStrategy;
import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.support.ProtocolFilterDecorator;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.rpc.Protocol;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.StringTools;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Notify cluster the referers have changed.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-31
 */

public class ClusterSupport<T> implements NotifyListener {

    private static ConcurrentHashMap<String, Protocol> protocols = new ConcurrentHashMap<>();
    private static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private static Set<ClusterSupport> refreshSet = new HashSet<>();

    static {
        executorService.scheduleAtFixedRate(() -> {
            for (ClusterSupport clusterSupport : refreshSet) {
                clusterSupport.refreshReferers();
            }
        }, MotanConstants.REFRESH_PERIOD, MotanConstants.REFRESH_PERIOD, TimeUnit.SECONDS);

        ShutDownHook.registerShutdownHook(() -> {
            if (!executorService.isShutdown()) {
                executorService.shutdown();
            }
        });
    }

    private Cluster<T> cluster;
    private List<URL> registryUrls;
    private URL url;
    private Class<T> interfaceClass;
    private Protocol protocol;
    private ConcurrentHashMap<URL, List<Referer<T>>> registryReferers = new ConcurrentHashMap<>();
    private int selectNodeCount;
    private ConcurrentHashMap<URL, Map<String, GroupUrlsSelector>> registryGroupUrlsSelectorMap = new ConcurrentHashMap<>();

    public ClusterSupport(Class<T> interfaceClass, List<URL> registryUrls) {
        this.registryUrls = registryUrls;
        this.interfaceClass = interfaceClass;
        String urlStr = StringTools.urlDecode(registryUrls.get(0).getParameter(URLParamType.embed.getName()));
        this.url = URL.valueOf(urlStr);
        protocol = getDecorateProtocol(url.getProtocol());
        int maxConnectionCount = this.url.getIntParameter(URLParamType.maxConnectionPerGroup.getName(), URLParamType.maxConnectionPerGroup.getIntValue());
        int maxClientConnection = this.url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());
        selectNodeCount = (int)Math.ceil(1.0 * maxConnectionCount / maxClientConnection);
    }

    public void init() {
        long start = System.currentTimeMillis();
        prepareCluster();

        URL subUrl = toSubscribeUrl(url);
        for (URL ru : registryUrls) {

            String directUrlStr = ru.getParameter(URLParamType.directUrl.getName());
            // 如果有directUrl，直接使用这些directUrls进行初始化，不用到注册中心discover
            if (StringUtils.isNotBlank(directUrlStr)) {
                List<URL> directUrls = parseDirectUrls(directUrlStr);
                if (!directUrls.isEmpty()) {
                    notify(ru, directUrls);
                    LoggerUtil.info("Use direct urls, refUrl={}, directUrls={}", url, directUrls);
                    continue;
                }
            }

            // client 注册自己，同时订阅service列表
            Registry registry = getRegistry(ru);
            registry.subscribe(subUrl, this);
        }

        boolean check = Boolean.parseBoolean(url.getParameter(URLParamType.check.getName(), URLParamType.check.getValue()));
        if (!CollectionUtil.isEmpty(cluster.getReferers()) || !check) {
            cluster.init();

            if (CollectionUtil.isEmpty(cluster.getReferers()) && !check) {
                LoggerUtil.warn(String.format("refer:%s", this.url.getPath() + "/" + this.url.getVersion()), "No services");
            }
            LoggerUtil.info("cluster init cost " + (System.currentTimeMillis() - start) + ", refer size:"
                    + (cluster.getReferers() == null ? 0 : cluster.getReferers().size()) + ", cluster:" + cluster.getUrl().toSimpleString());
            return;
        }

        throw new MotanFrameworkException(String.format("ClusterSupport No service urls for the refer:%s, registries:%s",
                this.url.getIdentity(), registryUrls), MotanErrorMsgConstant.SERVICE_UNFOUND);
    }

    public void destroy() {
        URL subscribeUrl = toSubscribeUrl(url);
        for (URL ru : registryUrls) {
            try {
                Registry registry = getRegistry(ru);
                registry.unsubscribe(subscribeUrl, this);
                if (!MotanConstants.NODE_TYPE_REFERER.equals(url.getParameter(URLParamType.nodeType.getName()))) {
                    registry.unregister(url);
                }
            } catch (Exception e) {
                LoggerUtil.warn(String.format("Unregister or unsubscribe false for url (%s), registry= %s", url, ru.getIdentity()), e);
            }

        }
        try {
            getCluster().destroy();
        } catch (Exception e) {
            LoggerUtil.warn(String.format("Exception when destroy cluster: %s", getCluster().getUrl()));
        }
    }

    protected Registry getRegistry(URL url) {
        RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(url.getProtocol());
        return registryFactory.getRegistry(url);
    }

    private URL toSubscribeUrl(URL url) {
        URL subUrl = url.createCopy();
        subUrl.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
        return subUrl;
    }

    /**
     * <pre>
     * 1 notify的执行需要串行
     * 2 notify通知都是全量通知，在设入新的referer后，cluster需要把不再使用的referer进行回收，避免资源泄漏;
     * 3 如果该registry对应的referer数量为0，而没有其他可用的referers，那就忽略该次通知；
     * 4 此处对protoco进行decorator处理，当前为增加filters
     * </pre>
     */
    @Override
    public synchronized void notify(URL registryUrl, List<URL> urls) {
        if (CollectionUtil.isEmpty(urls)) {
            onRegistryEmpty(registryUrl);
            LoggerUtil.warn("ClusterSupport config change notify, urls is empty: registry={} service={} urls=[]", registryUrl.getUri(),
                    url.getIdentity());
            return;
        }

        LoggerUtil.info("ClusterSupport config change notify: registry={} service={} urls={}", registryUrl.getUri(), url.getIdentity(),
                getIdentities(urls));

        // 通知都是全量通知，在设入新的referer后，cluster内部需要把不再使用的referer进行回收，避免资源泄漏
        // ////////////////////////////////////////////////////////////////////////////////

        // 判断urls中是否包含权重信息，并通知loadbalance。
        processWeights(urls);

        List<URL> serviceUrls = urls;
        if (selectNodeCount > 0 && MotanSwitcherUtil.switcherIsOpenWithDefault("feature.motan.partial.server", true)) {
            refreshSet.add(this);
            serviceUrls = selectUrls(registryUrl, urls);
        } else {
            refreshSet.remove(this);
        }
        doRefreshReferersByUrls(registryUrl, serviceUrls);
    }

    private void doRefreshReferersByUrls(URL registryUrl, List<URL> serviceUrls) {
        List<Referer<T>> newReferers = new ArrayList<>();
        for (URL u : serviceUrls) {
            if (!u.canServe(url)) {
                continue;
            }
            Referer<T> referer = getExistingReferer(u, registryReferers.get(registryUrl));
            if (referer == null) {
                // careful u: serverURL, refererURL的配置会被serverURL的配置覆盖
                URL refererURL = u.createCopy();
                mergeClientConfigs(refererURL);
                referer = protocol.refer(interfaceClass, refererURL, u);
            }
            if (referer != null) {
                newReferers.add(referer);
            }
        }

        if (CollectionUtil.isEmpty(newReferers)) {
            onRegistryEmpty(registryUrl);
            return;
        }

        // 此处不销毁referers，由cluster进行销毁
        registryReferers.put(registryUrl, newReferers);
        refreshCluster();
    }

    protected List<URL> selectUrls(URL registryUrl, List<URL> urls) {
        Map<String, List<URL>> groupUrlsMap = new HashMap<>();
        for (URL u : urls) {
            String group = u.getGroup();
            if (!groupUrlsMap.containsKey(group)) {
                groupUrlsMap.put(group, new ArrayList<URL>());
            }
            if (u.canServe(url)) {
                groupUrlsMap.get(group).add(u);
            }
        }
        Map<String, GroupUrlsSelector> selectorMap = registryGroupUrlsSelectorMap.computeIfAbsent(registryUrl, k -> new HashMap<>());

        for (Map.Entry<String, List<URL>> entry : groupUrlsMap.entrySet()) {
            GroupUrlsSelector groupUrlsSelector = selectorMap.computeIfAbsent(entry.getKey(), k -> new GroupUrlsSelector());
            if (entry.getValue().size() <= selectNodeCount) {
                LoggerUtil.info("ClusterSupport config change notify: registry={} service={} group={} size={} non increased",
                        registryUrl.getUri(), url.getIdentity(), entry.getKey(), entry.getValue().size());
            }
            groupUrlsSelector.updateBaseUrls(entry.getValue());
        }
        //去掉多余的group
        Set<String> removeGroups = new HashSet<>(selectorMap.keySet());
        removeGroups.removeAll(groupUrlsMap.keySet());
        if (!CollectionUtil.isEmpty(removeGroups)) {
            for (String removeGroup : removeGroups) {
                selectorMap.remove(removeGroup);
            }
        }

        return doSelectUrls(registryUrl);
    }

    private List<URL> doSelectUrls(URL registryUrl) {
        List<URL> result = new ArrayList<>();
        Map<String, GroupUrlsSelector> selectors = registryGroupUrlsSelectorMap.getOrDefault(registryUrl, Collections.emptyMap());
        for (Map.Entry<String, GroupUrlsSelector> entry : selectors.entrySet()) {
            List<URL> urls = entry.getValue().selectUrls();
            result.addAll(urls);

            LoggerUtil.info("ClusterSupport select group urls: registry={} service={} group={} expectSize={} size={} urls={}",
                    registryUrl.getUri(), url.getIdentity(), entry.getKey(), entry.getValue().getSelectSize(), urls.size(), getIdentities(urls));
        }

        return result;
    }

    protected void refreshReferers() {
        for (Map.Entry<URL, List<Referer<T>>> entry : registryReferers.entrySet()) {
            URL registryUrl = entry.getKey();
            LoggerUtil.info("ClusterSupport refreshReferers: registry={} service={}", registryUrl.getUri(), url.getIdentity());
            Map<String, GroupUrlsSelector> groupSelectorMap = registryGroupUrlsSelectorMap.get(registryUrl);
            if (groupSelectorMap == null || groupSelectorMap.size() == 0) {
                LoggerUtil.warn("ClusterSupport refreshReferers, groupSelectorMap is empty: registry={} service={}", registryUrl.getUri(), url.getIdentity());
                continue;
            }
            Map<String, Integer> groupAvailableCounter = new HashMap<>(groupSelectorMap.size());
            for (Referer<T> referer : entry.getValue()) {
                String group = referer.getServiceUrl().getGroup();
                if (referer.isAvailable()) {
                    groupAvailableCounter.put(group, groupAvailableCounter.getOrDefault(group, 0) + 1);
                }
            }

            boolean needRefresh = false;
            for (Map.Entry<String, Integer> counter : groupAvailableCounter.entrySet()) {
                String group = counter.getKey();
                int available = counter.getValue();

                GroupUrlsSelector selector = groupSelectorMap.get(group);
                if (selector == null) {
                    LoggerUtil.warn("ClusterSupport refreshReferers ,urls selector is null: registry={} service={} group={}", registryUrl.getUri(), url.getIdentity(), group);
                    continue;
                }
                int selectSize = selector.getSelectSize();

                int newSize = selectSize;
                //将有效referer的数量保持在一个范围内, 如果小于selectNodeCount的2/3或大于selectNodeCount的4/3
                // 则试图将可用数量恢复成selectNodeCount个
                if (available <= 1.0 * selectNodeCount * 2 / 3 && selector.getBaseUrlsSize() > selectSize) {
                    newSize = Math.min(selectSize + (selectNodeCount - available), selector.getBaseUrlsSize());
                } else if (available >= 1.0 * selectNodeCount * 4 / 3) {
                    newSize = selectSize - (available - selectNodeCount);
                }
                if (newSize != selectSize) {
                    needRefresh = true;
                    selector.setSelectSize(newSize);
                    LoggerUtil.info("ClusterSupport refreshReferers selectSize changed: registry={} service={} group={} newSize={} oldSize={}", registryUrl.getUri(), url.getIdentity(), group, newSize, selectSize);
                }
            }
            if (needRefresh) {
                List<URL> urls = doSelectUrls(registryUrl);
                doRefreshReferersByUrls(registryUrl, urls);
            }
        }
    }

    /**
     * 检查urls中的第一个url是否为权重信息。 如果是权重信息则把权重信息传递给loadbalance，并移除权重url。
     *
     * @param urls
     */
    private void processWeights(List<URL> urls) {
        if (urls != null && !urls.isEmpty()) {
            URL ruleUrl = urls.get(0);
            // 没有权重时需要传递默认值。因为可能是变更时去掉了权重
            String weights = URLParamType.weights.getValue();
            if ("rule".equalsIgnoreCase(ruleUrl.getProtocol())) {
                weights = ruleUrl.getParameter(URLParamType.weights.getName(), URLParamType.weights.getValue());
                urls.remove(0);
            }
            LoggerUtil.info("refresh weight. weight=" + weights);
            this.cluster.getLoadBalance().setWeightString(weights);
        }
    }

    private void onRegistryEmpty(URL excludeRegistryUrl) {
        boolean noMoreOtherRefers = registryReferers.size() == 1 && registryReferers.containsKey(excludeRegistryUrl);
        if (noMoreOtherRefers) {
            LoggerUtil.warn(String.format("Ignore notify for no more referers in this cluster, registry: %s, cluster=%s",
                    excludeRegistryUrl, getUrl()));
        } else {
            registryReferers.remove(excludeRegistryUrl);
            refreshCluster();
        }
    }

    protected Protocol getDecorateProtocol(String protocolName) {
        Protocol decorateProtocol = protocols.get(protocolName);
        if (decorateProtocol == null) {
            protocols.putIfAbsent(protocolName, new ProtocolFilterDecorator(ExtensionLoader.getExtensionLoader(Protocol.class)
                    .getExtension(protocolName)));
            decorateProtocol = protocols.get(protocolName);
        }
        return decorateProtocol;
    }

    private Referer<T> getExistingReferer(URL url, List<Referer<T>> referers) {
        if (referers == null) {
            return null;
        }
        for (Referer<T> r : referers) {
            if (ObjectUtils.equals(url, r.getUrl()) || ObjectUtils.equals(url, r.getServiceUrl())) {
                return r;
            }
        }
        return null;
    }

    /**
     * refererURL的扩展参数中，除了application、module外，其他参数被client覆盖， 如果client没有则使用referer的参数
     *
     * @param refererURL
     */
    private void mergeClientConfigs(URL refererURL) {
        String application = refererURL.getParameter(URLParamType.application.getName(), URLParamType.application.getValue());
        String module = refererURL.getParameter(URLParamType.module.getName(), URLParamType.module.getValue());
        refererURL.addParameters(this.url.getParameters());

        refererURL.addParameter(URLParamType.application.getName(), application);
        refererURL.addParameter(URLParamType.module.getName(), module);
    }

    private void refreshCluster() {
        List<Referer<T>> referers = new ArrayList<>();
        for (List<Referer<T>> refs : registryReferers.values()) {
            referers.addAll(refs);
        }
        cluster.onRefresh(referers);
    }

    public Cluster<T> getCluster() {
        return cluster;
    }

    public URL getUrl() {
        return url;
    }

    private String getIdentities(List<URL> urls) {
        if (urls == null || urls.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (URL u : urls) {
            builder.append(u.getIdentity()).append(",");
        }
        builder.setLength(builder.length() - 1);
        builder.append("]");

        return builder.toString();
    }

    @SuppressWarnings("unchecked")
    private void prepareCluster() {
        String clusterName = url.getParameter(URLParamType.cluster.getName(), URLParamType.cluster.getValue());
        String loadbalanceName = url.getParameter(URLParamType.loadbalance.getName(), URLParamType.loadbalance.getValue());
        String haStrategyName = url.getParameter(URLParamType.haStrategy.getName(), URLParamType.haStrategy.getValue());

        cluster = ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(clusterName);
        LoadBalance<T> loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadbalanceName);
        HaStrategy<T> ha = ExtensionLoader.getExtensionLoader(HaStrategy.class).getExtension(haStrategyName);
        ha.setUrl(url);
        cluster.setLoadBalance(loadBalance);
        cluster.setHaStrategy(ha);
        cluster.setUrl(url);
    }

    private List<URL> parseDirectUrls(String directUrlStr) {
        String[] durlArr = MotanConstants.COMMA_SPLIT_PATTERN.split(directUrlStr);
        List<URL> directUrls = new ArrayList<>();
        for (String dus : durlArr) {
            URL du = URL.valueOf(StringTools.urlDecode(dus));
            if (du != null) {
                directUrls.add(du);
            }
        }
        return directUrls;
    }

    private class GroupUrlsSelector {
        private List<URL> baseUrls;
        private int selectSize;

        GroupUrlsSelector(){
            baseUrls = new ArrayList<>();
            selectSize = selectNodeCount;
        }

        void updateBaseUrls(List<URL> newBaseUrls){
            baseUrls.retainAll(newBaseUrls);

            Set<URL> addedUrls = new HashSet<>(newBaseUrls);
            addedUrls.removeAll(baseUrls);

            for (URL addedUrl : addedUrls) {
                int addPosition = ThreadLocalRandom.current().nextInt(baseUrls.size() + 1);
                baseUrls.add(addPosition, addedUrl);
            }
        }

        List<URL> selectUrls() {
            List<URL> result = new ArrayList<>(selectSize);
            if (baseUrls.size() >= selectSize) {
                result.addAll(baseUrls.subList(0, selectSize));
            } else {
                result.addAll(baseUrls);
            }
            return result;
        }

        int getSelectSize() {
            return selectSize;
        }

        void setSelectSize(int selectSize) {
            this.selectSize = selectSize;
        }

        int getBaseUrlsSize(){
            return baseUrls.size();
        }
    }
}
