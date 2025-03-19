/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.cluster.loadbalance;

import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MathUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load balancing based on group weight.
 *
 * @author zhanglei28
 * @date 2024/3/26.
 */
public class GroupWeightLoadBalanceWrapper<T> extends AbstractLoadBalance<T> {
    volatile Selector<T> selector;
    private String weightString;
    private final String loadBalanceName;

    public GroupWeightLoadBalanceWrapper(String loadBalanceName) {
        this.loadBalanceName = loadBalanceName;
    }

    @Override
    public void onRefresh(List<Referer<T>> referers) {
        // There is no need to execute the shuffle logic, the LB in the selector will execute it
        super.onRefresh(referers, false);
        Selector<T> oldSelector = null;
        boolean reuse = false;
        String selectorName;
        if (StringUtils.isEmpty(weightString) || referers.isEmpty()) { // single group or no referers
            selectorName = "SingleGroupSelector";
            if (selector instanceof SingleGroupSelector) { // reuse
                ((SingleGroupSelector<T>) selector).refresh(referers);
                reuse = true;
            } else {
                oldSelector = selector;
                selector = new SingleGroupSelector<>(clusterUrl, referers, loadBalanceName);
            }
        } else { // multi group
            selectorName = "MultiGroupSelector";
            if (selector instanceof MultiGroupSelector) { // reuse
                ((MultiGroupSelector<T>) selector).refresh(referers, weightString);
                reuse = true;
            } else {
                oldSelector = selector;
                selector = new MultiGroupSelector<>(clusterUrl, referers, loadBalanceName, weightString);
            }
        }
        LoggerUtil.info("GroupWeightLoadBalance onRefresh use " + selectorName + ". reuse selector: " + reuse + ", url:" + clusterUrl.toSimpleString());
        if (oldSelector != null) { // if selector is changed, destroy the old one
            oldSelector.destroy();
        }
    }

    @Override
    public boolean canSelectMulti() {
        return false;
    }

    @Override
    protected Referer<T> doSelect(Request request) {
        if (selector == null) {
            return null;
        }
        return selector.select(request);
    }

    @Override
    public void destroy() {
        if (selector != null) {
            selector.destroy();
            selector = null;
        }
    }

    @Override
    public void setWeightString(String weightString) {
        this.weightString = weightString;
    }

    static class SingleGroupSelector<T> implements Selector<T> {
        private final LoadBalance<T> loadBalance;

        @SuppressWarnings("unchecked")
        public SingleGroupSelector(URL clusterUrl, List<Referer<T>> referers, String loadBalanceName) {
            loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadBalanceName);
            loadBalance.init(clusterUrl);
            loadBalance.onRefresh(referers);
        }

        @Override
        public Referer<T> select(Request request) {
            return loadBalance.select(request);
        }

        public void refresh(List<Referer<T>> referers) {
            loadBalance.onRefresh(referers);
        }

        @Override
        public void destroy() {
            loadBalance.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    static class MultiGroupSelector<T> implements Selector<T> {
        private final URL clusterUrl;
        private final String loadBalanceName;
        private List<Referer<T>> referers;
        private String weightString;
        private volatile InnerSelector<T> innerSelector;

        public MultiGroupSelector(URL clusterUrl, List<Referer<T>> referers, String loadBalanceName, String weightString) {
            this.referers = referers;
            this.clusterUrl = clusterUrl;
            this.loadBalanceName = loadBalanceName;
            this.weightString = weightString;
            reBuildInnerSelector();
        }

        @Override
        public Referer<T> select(Request request) {
            if (innerSelector == null) {
                return null;
            }
            return innerSelector.select(request);
        }

        public synchronized void refresh(List<Referer<T>> referers, String weightString) {
            this.referers = referers;
            if (this.weightString.equals(weightString)) {
                // When weightString does not change, refresh each LB in innerSelector.
                Map<String, List<Referer<T>>> groupReferers = getGroupReferers(referers);
                for (String groupName : groupReferers.keySet()) {
                    LoadBalance<T> loadBalance = innerSelector.lbMap.get(groupName);
                    if (loadBalance != null) {
                        loadBalance.onRefresh(groupReferers.get(groupName));
                    } else {
                        LoggerUtil.warn("GroupWeightLoadBalance groupName:{} not exist in innerSelector.lbMap", groupName);
                    }
                }
            } else {
                this.weightString = weightString;
                reBuildInnerSelector();
            }
        }

        // depend on referers and weightString
        private void reBuildInnerSelector() {
            Map<String, List<Referer<T>>> groupReferers = getGroupReferers(referers);
            // CommandServiceManager ensures that there will be no duplicate groups in the weightString
            // and no abnormal weight value.
            // If it occurs, just throw an exception
            String[] groupsAndWeights = weightString.split(",");
            if (groupsAndWeights.length > 256) {
                throw new MotanFrameworkException("the group in weightString is greater than 256");
            }
            int[] weightsArr = new int[groupsAndWeights.length];
            LoadBalance<T>[] lbArray = new LoadBalance[groupsAndWeights.length];
            ConcurrentHashMap<String, LoadBalance<T>> lbMap = new ConcurrentHashMap<>();
            int totalWeight = 0;
            for (int i = 0; i < groupsAndWeights.length; i++) {
                String[] gw = groupsAndWeights[i].split(":");
                weightsArr[i] = Integer.parseInt(gw[1]);
                totalWeight += weightsArr[i];
                lbArray[i] = reuseOrCreateLB(gw[0], groupReferers.get(gw[0]));
                lbMap.put(gw[0], lbArray[i]);
            }

            // divide the weight by the gcd of all weights
            int weightGcd = MathUtil.findGCD(weightsArr);
            if (weightGcd > 1) {
                totalWeight = 0;
                for (int i = 0; i < weightsArr.length; i++) {
                    weightsArr[i] = weightsArr[i] / weightGcd;
                    totalWeight += weightsArr[i];
                }
            }

            // build weight ring
            byte[] groupWeightRing = new byte[totalWeight];
            int index = 0;
            for (int i = 0; i < weightsArr.length; i++) {
                for (int j = 0; j < weightsArr[i]; j++) {
                    groupWeightRing[index++] = (byte) i;
                }
            }
            CollectionUtil.shuffleByteArray(groupWeightRing);

            ConcurrentHashMap<String, LoadBalance<T>> remain = innerSelector == null ? null : innerSelector.lbMap;
            innerSelector = new InnerSelector<>(lbMap, groupWeightRing, lbArray);

            // Destroy the remaining LB
            if (remain != null) {
                for (LoadBalance<T> lb : remain.values()) {
                    try {
                        lb.destroy();
                    } catch (Exception e) {
                        LoggerUtil.warn("GroupWeightLoadBalance destroy lb fail. url:" + clusterUrl.toSimpleString() + " error: " + e.getMessage());
                    }
                }
            }
        }

        private LoadBalance<T> reuseOrCreateLB(String group, List<Referer<T>> groupReferers) {
            LoadBalance<T> loadBalance = null;
            if (innerSelector != null) { // Reuse LB by group name
                loadBalance = innerSelector.lbMap.remove(group);
            }
            if (loadBalance == null) { // create new LB
                loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension(loadBalanceName);
                loadBalance.init(clusterUrl);
            }
            loadBalance.onRefresh(groupReferers); // Referers in the same group can refresh first
            return loadBalance;
        }

        private Map<String, List<Referer<T>>> getGroupReferers(List<Referer<T>> referers) {
            Map<String, List<Referer<T>>> result = new HashMap<>();
            for (Referer<T> referer : referers) {
                result.computeIfAbsent(referer.getUrl().getGroup(), k -> new ArrayList<>()).add(referer);
            }
            return result;
        }

        @Override
        public void destroy() {
            referers = null;
            if (innerSelector != null) {
                innerSelector.destroy();
                innerSelector = null;
            }
        }
    }

    private static class InnerSelector<T> implements Selector<T> {
        final ConcurrentHashMap<String, LoadBalance<T>> lbMap; // LBs currently in use. just for reuse lb
        final byte[] groupWeightRing; // group weight ring. there won’t be many groups
        final LoadBalance<T>[] lbArray;
        final AtomicInteger index = new AtomicInteger(0);

        public InnerSelector(ConcurrentHashMap<String, LoadBalance<T>> lbMap, byte[] groupWeightRing, LoadBalance<T>[] lbArray) {
            this.lbMap = lbMap;
            this.groupWeightRing = groupWeightRing;
            this.lbArray = lbArray;
        }

        @Override
        public Referer<T> select(Request request) {
            return lbArray[getLBIndex(MathUtil.getNonNegative(index.getAndIncrement()))].select(request);
        }

        @Override
        public void destroy() {
            for (LoadBalance<T> lb : lbArray) {
                lb.destroy();
            }
        }

        private int getLBIndex(int ringIndex) {
            int lbIndex = groupWeightRing[ringIndex % groupWeightRing.length];
            if (lbIndex < 0) { // The java byte range is -128~127
                lbIndex += 256;
            }
            return lbIndex;
        }
    }
}
