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

package com.weibo.api.motan.cluster.loadbalance;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MathUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 权重可配置的负载均衡器
 *
 * @author chengya1
 */
@SpiMeta(name = "configurableWeight")
public class ConfigurableWeightLoadBalance<T> extends ActiveWeightLoadBalance<T> {

    @SuppressWarnings("rawtypes")
    private static final RefererListCacheHolder emptyHolder = new EmptyHolder();

    @SuppressWarnings("unchecked")
    private volatile RefererListCacheHolder<T> holder = emptyHolder;

    private String weightString;

    @SuppressWarnings("unchecked")
    @Override
    public void onRefresh(List<Referer<T>> referers) {
        super.onRefresh(referers);

        if (CollectionUtil.isEmpty(referers)) {
            holder = emptyHolder;
        } else if (StringUtils.isEmpty(weightString)) {
            holder = new SingleGroupHolder<T>(referers);
        } else {
            holder = new MultiGroupHolder<T>(weightString, referers);
        }
    }

    @Override
    protected Referer<T> doSelect(Request request) {
        if (holder == emptyHolder) {
            return null;
        }

        RefererListCacheHolder<T> h = this.holder;
        Referer<T> r = h.next();
        if (!r.isAvailable()) {
            int retryTimes = getReferers().size() - 1;
            for (int i = 0; i < retryTimes; i++) {
                r = h.next();
                if (r.isAvailable()) {
                    break;
                }
            }
        }
        if (r.isAvailable()) {
            return r;
        } else {
            noAvailableReferer();
            return null;
        }
    }

    @Override
    protected void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        if (holder == emptyHolder) {
            return;
        }

        RefererListCacheHolder<T> h = this.holder;
        int i = 0, j = 0;
        while (i++ < getReferers().size()) {
            Referer<T> r = h.next();
            if (r.isAvailable()) {
                refersHolder.add(r);
                if (++j == MAX_REFERER_COUNT) {
                    return;
                }
            }
        }
        if (refersHolder.isEmpty()) {
            noAvailableReferer();
        }
    }

    private void noAvailableReferer() {
        LoggerUtil.error(this.getClass().getSimpleName() + " 当前没有可用连接, pool.size=" + getReferers().size());
    }

    @Override
    public void setWeightString(String weightString) {
        this.weightString = weightString;
    }


    /*****************************************************************************************
     * ************************************************************************************* *
     *****************************************************************************************/
    static abstract class RefererListCacheHolder<T> {
        abstract Referer<T> next();
    }

    static class EmptyHolder<T> extends RefererListCacheHolder<T> {
        @Override
        Referer<T> next() {
            return null;
        }
    }

    @SuppressWarnings("hiding")
    class SingleGroupHolder<T> extends RefererListCacheHolder<T> {

        private int size;
        private List<Referer<T>> cache;

        SingleGroupHolder(List<Referer<T>> list) {
            cache = list;
            size = list.size();
            LoggerUtil.info("ConfigurableWeightLoadBalance build new SingleGroupHolder.");
        }

        @Override
        Referer<T> next() {
            return cache.get(ThreadLocalRandom.current().nextInt(size));
        }
    }

    @SuppressWarnings("hiding")
    class MultiGroupHolder<T> extends RefererListCacheHolder<T> {

        private int randomKeySize = 0;
        private List<String> randomKeyList = new ArrayList<String>();
        private Map<String, AtomicInteger> cursors = new HashMap<String, AtomicInteger>();
        private Map<String, List<Referer<T>>> groupReferers = new HashMap<String, List<Referer<T>>>();

        MultiGroupHolder(String weights, List<Referer<T>> list) {
            LoggerUtil.info("ConfigurableWeightLoadBalance build new MultiGroupHolder. weights:" + weights);
            String[] groupsAndWeights = weights.split(",");
            int[] weightsArr = new int[groupsAndWeights.length];
            Map<String, Integer> weightsMap = new HashMap<String, Integer>(groupsAndWeights.length);
            int i = 0;
            for (String groupAndWeight : groupsAndWeights) {
                String[] gw = groupAndWeight.split(":");
                if (gw.length == 2) {
                    Integer w = Integer.valueOf(gw[1]);
                    weightsMap.put(gw[0], w);
                    groupReferers.put(gw[0], new ArrayList<Referer<T>>());
                    weightsArr[i++] = w;
                }
            }

            // 求出最大公约数，若不为1，对权重做除法
            int weightGcd = findGcd(weightsArr);
            if (weightGcd != 1) {
                for(Map.Entry<String,Integer> entry: weightsMap.entrySet()) {
                    weightsMap.put(entry.getKey(),entry.getValue()/weightGcd);
                }
            }

            for (Map.Entry<String, Integer> entry : weightsMap.entrySet()) {
                for (int j = 0; j < entry.getValue(); j++) {
                    randomKeyList.add(entry.getKey());
                }
            }
            Collections.shuffle(randomKeyList);
            randomKeySize = randomKeyList.size();

            for (String key : weightsMap.keySet()) {
                cursors.put(key, new AtomicInteger(0));
            }

            for (Referer<T> referer : list) {
                groupReferers.get(referer.getServiceUrl().getGroup()).add(referer);
            }
        }

        @Override
        Referer<T> next() {
            String group = randomKeyList.get(ThreadLocalRandom.current().nextInt(randomKeySize));
            AtomicInteger ai = cursors.get(group);
            List<Referer<T>> referers = groupReferers.get(group);
            return referers.get(MathUtil.getNonNegative(ai.getAndIncrement()) % referers.size());
        }

        // 求最大公约数
        private int findGcd(int n, int m) {
            return (n == 0 || m == 0) ? n + m : findGcd(m, n % m);
        }

        // 求最大公约数
        private int findGcd(int[] arr) {
            int i = 0;
            for (; i < arr.length - 1; i++) {
                arr[i + 1] = findGcd(arr[i], arr[i + 1]);
            }
            return findGcd(arr[i], arr[i - 1]);
        }
    }

}
