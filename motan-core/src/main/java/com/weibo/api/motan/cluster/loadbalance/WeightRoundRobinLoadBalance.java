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

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhanglei28
 * @date 2024/3/19.
 */
@SpiMeta(name = "wrr")
public class WeightRoundRobinLoadBalance<T> extends AbstractWeightedLoadBalance<T> {
    private volatile Selector<T> selector;

    @Override
    public boolean canSelectMulti() {
        return false;
    }

    @Override
    public Referer<T> doSelect(Request request) {
        if (selector == null) {
            return null;
        }
        return selector.select();
    }

    @Override
    public void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        throw new UnsupportedOperationException();
    }

    @Override
    synchronized void notifyWeightChange() {
        selector = new Selector<>(weightedRefererHolders);
    }

    private static class Selector<T> {
        private boolean sameWeightAll; // Whether all referers have the same weight
        private int totalWeight; // Total weight of all referers
        private int maxWeight; // Max weight of all referers
        private int maxWeightIndex; // Index of the referer with max weight

        private final List<SelectorItem<T>> items;

        public Selector(List<WeightedRefererHolder<T>> weightedRefererHolders) {
            items = new ArrayList<>(weightedRefererHolders.size());
            init(weightedRefererHolders);
        }

        private void init(List<WeightedRefererHolder<T>> weightedRefererHolders) {
            // TODO 初始化工作
            int lastWeight;
            for (WeightedRefererHolder<T> holder : weightedRefererHolders) {
                items.add(new SelectorItem<>(holder.referer, holder.getWeight()));

            }
        }

        Referer<T> select() {
            if (sameWeightAll) { // TODO 退化成轮询

            }
            // TODO 是否需要同步？目前看是需要

            // 如果有无效节点，该如何处理？
            // 应该在每次计算时把不可用节点排除。总值是否需要每次计算？如果每次排掉的话，应该动态计算总值。
            return null;
        }
    }

    private static class SelectorItem<T> {
        final Referer<T> referer;
        int weight; // original weight
        int currentWeight = 0;

        public SelectorItem(Referer<T> referer, int weight) {
            this.referer = referer;
            this.weight = weight;
        }
    }
}
