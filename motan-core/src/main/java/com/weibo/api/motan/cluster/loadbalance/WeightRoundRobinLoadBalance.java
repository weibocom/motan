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

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptive Weighted Round Robin Load Balancing
 * Use simple RoundRobin Load Balancing when all nodes have the same weight;
 * WeightRing Load Balancing are used when size of nodes is small and the sum of weights is small.
 * Otherwise, use SlidingWindowWeightedRoundRobin Load Balancing which is improved from Nginx SWRR.
 *
 * @author zhanglei28
 * @date 2024/3/19.
 */
@SpiMeta(name = "wrr")
public class WeightRoundRobinLoadBalance<T> extends AbstractWeightedLoadBalance<T> {
    volatile Selector<T> selector;

    @Override
    public boolean canSelectMulti() {
        return false;
    }

    @Override
    public Referer<T> doSelect(Request request) {
        if (selector == null) {
            return null;
        }
        return selector.select(request);
    }

    @Override
    synchronized void notifyWeightChange() {
        List<WeightedRefererHolder<T>> tempHolders = weightedRefererHolders;
        int[] weights = new int[tempHolders.size()];
        boolean haveSameWeight = true;
        int totalWeight = 0;
        for (int i = 0; i < tempHolders.size(); i++) {
            weights[i] = tempHolders.get(i).getWeight();
            totalWeight += weights[i];
            if (weights[i] != weights[0]) {
                haveSameWeight = false;
            }
        }
        // if all referers have the same weight, then use RoundRobinSelector
        if (haveSameWeight) { // use RoundRobinLoadBalance
            if (selector instanceof RoundRobinSelector) { // reuse the RoundRobinSelector
                // refresh the RoundRobinSelector
                ((RoundRobinSelector<T>) selector).refresh(tempHolders);
                return;
            }
            // new RoundRobinLoadBalance
            selector = new RoundRobinSelector<>(tempHolders);
            LoggerUtil.info("WeightRoundRobinLoadBalance use RoundRobinSelector. url:" + getUrlLogInfo());
            return;
        }

        // find the GCD and divide the weights
        int gcd = MathUtil.findGCD(weights);
        if (gcd > 1) {
            totalWeight = 0; // recalculate totalWeight
            for (int i = 0; i < weights.length; i++) {
                weights[i] /= gcd;
                totalWeight += weights[i];
            }
        }

        // Check whether it is suitable to use WeightedRingSelector
        if (weights.length <= WeightedRingSelector.MAX_REFERER_SIZE
                && totalWeight <= WeightedRingSelector.MAX_TOTAL_WEIGHT) {
            selector = new WeightedRingSelector<>(tempHolders, totalWeight, weights);
            LoggerUtil.info("WeightRoundRobinLoadBalance use WeightedRingSelector. url:" + getUrlLogInfo());
            return;
        }
        selector = new SlidingWindowWeightedRoundRobinSelector<>(tempHolders, weights);
        LoggerUtil.info("WeightRoundRobinLoadBalance use SlidingWindowWeightedRoundRobinSelector. url:" + getUrlLogInfo());
    }

    private String getUrlLogInfo() {
        URL url = clusterUrl;
        if (url == null && !CollectionUtil.isEmpty(weightedRefererHolders)) {
            url = weightedRefererHolders.get(0).referer.getUrl();
        }
        return url == null ? "" : url.toSimpleString();
    }

    static class RoundRobinSelector<T> implements Selector<T> {
        private volatile List<WeightedRefererHolder<T>> holders;
        private final AtomicInteger idx = new AtomicInteger(0);

        public RoundRobinSelector(List<WeightedRefererHolder<T>> holders) {
            this.holders = holders;
        }

        @Override
        public Referer<T> select(Request request) {
            List<WeightedRefererHolder<T>> tempHolders = holders;
            Referer<T> ref = tempHolders.get(MathUtil.getNonNegative(idx.incrementAndGet()) % tempHolders.size()).referer;
            if (ref.isAvailable()) {
                return ref;
            }
            // If the referer is not available, loop selection from random position.
            int start = ThreadLocalRandom.current().nextInt(tempHolders.size());
            for (int i = 0; i < tempHolders.size(); i++) {
                ref = tempHolders.get((start + i) % tempHolders.size()).referer;
                if (ref.isAvailable()) {
                    return ref;
                }
            }
            return null;
        }

        public void refresh(List<WeightedRefererHolder<T>> holders) {
            this.holders = holders;
        }
    }

    static class WeightedRingSelector<T> implements Selector<T> {
        static final int MAX_REFERER_SIZE = 256;
        static final int MAX_TOTAL_WEIGHT = 256 * 20; // The maximum space occupied by the weight ring。 default is 5KB
        private final List<WeightedRefererHolder<T>> holders;

        private final AtomicInteger ringIndex = new AtomicInteger(0);
        private final int[] weights;
        private final byte[] weightRing;

        public WeightedRingSelector(List<WeightedRefererHolder<T>> holders, int totalWeight, int[] weights) {
            this.holders = holders;
            this.weightRing = new byte[totalWeight];
            this.weights = weights;
            initWeightRing();
        }

        private void initWeightRing() {
            int ringIndex = 0;
            for (int i = 0; i < weights.length; i++) {
                for (int j = 0; j < weights[i]; j++) {
                    weightRing[ringIndex++] = (byte) i;
                }
            }
            if (ringIndex != weightRing.length) { // should not happen. just log it.
                LoggerUtil.error("WeightedRingSelector initWeightRing with wrong totalWeight. expect:" + weightRing.length + ", actual:" + ringIndex);
            }
            CollectionUtil.shuffleByteArray(weightRing);
        }

        public Referer<T> select(Request request) {
            Referer<T> ref = holders.get(getHolderIndex(MathUtil.getNonNegative(ringIndex.getAndIncrement()))).getReferer();
            if (ref.isAvailable()) {
                return ref;
            }
            // If the referer is not available, loop selection from random position.
            int start = ThreadLocalRandom.current().nextInt(weightRing.length);
            for (int i = 0; i < weightRing.length; i++) {
                Referer<T> referer = holders.get(getHolderIndex(start + i)).getReferer();
                if (referer.isAvailable()) {
                    return referer;
                }
            }
            return null;
        }

        private int getHolderIndex(int ringIndex) {
            int holderIndex = weightRing[ringIndex % weightRing.length];
            if (holderIndex < 0) { // The java byte range is -128~127
                holderIndex += 256;
            }
            return holderIndex;
        }
    }


    /**
     * SlidingWindowWeightedRoundRobinSelector
     */
    static class SlidingWindowWeightedRoundRobinSelector<T> implements Selector<T> {
        static final int DEFAULT_WINDOW_SIZE = 50;
        private final AtomicInteger index = new AtomicInteger(0);
        private int windowSize;

        private final List<SelectorItem<T>> items;

        public SlidingWindowWeightedRoundRobinSelector(List<WeightedRefererHolder<T>> weightedRefererHolders, int[] weights) {
            items = new ArrayList<>(weightedRefererHolders.size());
            init(weightedRefererHolders, weights);
        }

        private void init(List<WeightedRefererHolder<T>> weightedRefererHolders, int[] weights) {
            windowSize = weights.length;
            if (windowSize > DEFAULT_WINDOW_SIZE) { // The sliding window size needs to be calculated only when the number of referers is greater than DEFAULT_WINDOW_SIZE
                windowSize = DEFAULT_WINDOW_SIZE;
                // The window size cannot be divided by the number of referers, which ensures that the starting position
                // of the window will gradually change during sliding
                while (weights.length % windowSize == 0) {
                    windowSize--;
                }
            }

            for (int i = 0; i < weights.length; i++) {
                items.add(new SelectorItem<>(weightedRefererHolders.get(i).referer, weights[i]));
            }
        }

        public Referer<T> select(Request request) {
            int windowStartIndex = MathUtil.getNonNegative(index.getAndAdd(windowSize));
            int totalWeight = 0;
            int maxWeight = 0; // Max weight of current window referers
            int maxWeightIndex = 0; // Index of the referer with max weight

            // Use SWRR(https://github.com/nginx/nginx/commit/52327e0627f49dbda1e8db695e63a4b0af4448b1) to select referer from the current window.
            // In order to reduce costs, do not limit concurrency in the entire selection process,
            // and only use atomic updates for the current weight.
            // Since concurrent threads will execute Select in different windows,
            // the problem of instantaneous requests increase on one node due to concurrency will not be serious.
            // And because the referers used on different client sides are shuffled,
            // the impact of high instantaneous concurrent selection on the server side will be further reduced.
            for (int i = 0; i < windowSize; i++) {
                int idx = (windowStartIndex + i) % items.size();
                SelectorItem<T> item = items.get(idx);
                if (item.referer.isAvailable()) { // Only count available nodes
                    int currentWeight = item.currentWeight.addAndGet(item.weight);
                    totalWeight += item.weight;
                    if (currentWeight > maxWeight) {
                        maxWeight = currentWeight;
                        maxWeightIndex = idx;
                    }
                }
            }
            if (maxWeight > 0) { // select max weight referer
                SelectorItem<T> item = items.get(maxWeightIndex);
                item.currentWeight.addAndGet(-totalWeight);
                if (item.referer.isAvailable()) {
                    return item.referer;
                }
            }

            // If no suitable node is selected or the node is unavailable,
            // then select an available referer from a random index
            int idx = windowStartIndex + ThreadLocalRandom.current().nextInt(windowSize);
            for (int i = 1; i < items.size(); i++) {
                SelectorItem<T> item = items.get((idx + i) % items.size());
                if (item.referer.isAvailable()) {
                    return item.referer;
                }
            }
            return null;
        }
    }

    private static class SelectorItem<T> {
        final Referer<T> referer;
        int weight; // referer weight
        AtomicInteger currentWeight = new AtomicInteger(0); // current weight

        public SelectorItem(Referer<T> referer, int weight) {
            this.referer = referer;
            this.weight = weight;
        }
    }
}
