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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.mock.MockDynamicReferer;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.MetaUtil;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author zhanglei28
 * @date 2024/3/21.
 */
@SuppressWarnings("all")
public class WeightRoundRobinLoadBalanceTest extends TestCase {
    WeightRoundRobinLoadBalance<IHello> weightRoundRobinLoadBalance;


    public void setUp() throws Exception {
        MotanGlobalConfigUtil.putConfig(MotanConstants.META_CACHE_EXPIRE_SECOND_KEY, "1");
        MotanGlobalConfigUtil.putConfig(MotanConstants.WEIGHT_REFRESH_PERIOD_SECOND_KEY, "1");
        MetaUtil.clearCache();
        weightRoundRobinLoadBalance = new WeightRoundRobinLoadBalance();
    }

    public void tearDown() throws Exception {
        weightRoundRobinLoadBalance.closeRefreshTask();
        weightRoundRobinLoadBalance = null;
    }

    public void testAbstractWeightedLoadBalance() throws InterruptedException {
        URL url = new URL("motan2", "127.0.0.1", 8080, "mockService");

        // test dynamic weight config
        url.addParameter(URLParamType.dynamicMeta.getName(), "false"); // disable dynamic weight
        weightRoundRobinLoadBalance.init(url);
        assertFalse(weightRoundRobinLoadBalance.supportDynamicWeight);
        assertTrue(AbstractWeightedLoadBalance.dynamicWeightedLoadBalances.isEmpty());

        url.removeParameter(URLParamType.dynamicMeta.getName());
        weightRoundRobinLoadBalance.init(url);
        assertTrue(weightRoundRobinLoadBalance.supportDynamicWeight);
        assertEquals(1, AbstractWeightedLoadBalance.dynamicWeightedLoadBalances.size());

        int staticWeight = 9;
        MetaUtil.clearCache();
        List<Referer<IHello>> referers = buildDynamicReferers(10, true, staticWeight);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);
        weightRoundRobinLoadBalance.weightedRefererHolders.forEach((holder) -> {
            // test static weight
            assertEquals(staticWeight, holder.staticWeight);
            assertEquals(0, holder.dynamicWeight);
            assertEquals(staticWeight, holder.getWeight());
        });

        // test dynamic weight change
        ((MockDynamicReferer) weightRoundRobinLoadBalance.weightedRefererHolders.get(3).referer).setWeight(true, 22);
        triggerRefresh();
        assertEquals(22, weightRoundRobinLoadBalance.weightedRefererHolders.get(3).dynamicWeight);
        assertEquals(22, weightRoundRobinLoadBalance.weightedRefererHolders.get(3).getWeight());
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.WeightedRingSelector);

        // test closeRefreshTask
        weightRoundRobinLoadBalance.closeRefreshTask();
        assertTrue(AbstractWeightedLoadBalance.dynamicWeightedLoadBalances.isEmpty());
    }

    public void testGetRefererWeight() throws ExecutionException {
        MockDynamicReferer referer = new MockDynamicReferer(new URL("motan2", "127.0.0.1", 8080, "mockService"), 1);
        // test static weight
        assertEquals(11, weightRoundRobinLoadBalance.getRefererWeight(referer, false, 11));
        assertEquals(5, weightRoundRobinLoadBalance.getRefererWeight(referer, false, 5));
        referer.setWeight(false, 8);
        assertEquals(8, weightRoundRobinLoadBalance.getRefererWeight(referer, false, 11));

        // test dynamic weight
        assertEquals(0, weightRoundRobinLoadBalance.getRefererWeight(referer, true, 0));
        assertEquals(5, weightRoundRobinLoadBalance.getRefererWeight(referer, true, 5));
        referer.setWeight(true, 15);
        assertEquals(15, weightRoundRobinLoadBalance.getRefererWeight(referer, true, 11));

        // test abnormal weight
        referer.setWeight(false, -8);
        assertEquals(AbstractWeightedLoadBalance.MIN_WEIGHT, weightRoundRobinLoadBalance.getRefererWeight(referer, false, 11));

        referer.setWeight(false, 501);
        assertEquals(AbstractWeightedLoadBalance.MAX_WEIGHT, weightRoundRobinLoadBalance.getRefererWeight(referer, false, 11));

        referer.setWeight(true, -1);
        assertEquals(AbstractWeightedLoadBalance.MIN_WEIGHT, weightRoundRobinLoadBalance.getRefererWeight(referer, true, 11));

        referer.setWeight(true, 666);
        assertEquals(AbstractWeightedLoadBalance.MAX_WEIGHT, weightRoundRobinLoadBalance.getRefererWeight(referer, true, 11));
    }


    public void testNotifyWeightChange() {
        // test use RR
        List<Referer<IHello>> referers = buildDynamicReferers(20, true, 8);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);

        referers = buildDynamicReferers(20, true, 0);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);

        referers = buildDynamicReferers(20, true, 101);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);

        referers = buildDynamicReferers(20, true, -10); // abnormal weight
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);


        // test use WR
        referers = buildDynamicReferers(20, false, 8);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.WeightedRingSelector);

        referers = buildDynamicReferers(WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_REFERER_SIZE, false, 8);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.WeightedRingSelector);

        referers = buildDynamicReferers(WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_REFERER_SIZE, false, WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_TOTAL_WEIGHT / WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_REFERER_SIZE);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.WeightedRingSelector);

        // test use SWWRR
        referers = buildDynamicReferers(WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_REFERER_SIZE + 1, false, 6);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.SlidingWindowWeightedRoundRobinSelector);

        referers = buildDynamicReferers(40, false, 800);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.SlidingWindowWeightedRoundRobinSelector);
    }

    public void testRoundRobinSelector() {
        int round = 100;

        // small size
        checkRR(20, 8, round, 1, 1, 0);

        // large size
        checkRR(500, 8, round, 1, 1, 0);

        // some nodes are unavailable
        double maxRatio = 0.4;
        double avgRatio = 0.1;
        round = 200;
        checkRR(20, 8, round, round * maxRatio, round * avgRatio, 2);
        checkRR(100, 8, round, round * maxRatio, round * avgRatio, 10);

        maxRatio = 0.7;
        checkRR(300, 8, round, round * maxRatio, round * avgRatio, 50);
    }

    private void checkRR(int size, int initialMaxWeight, int round, double expectMaxDelta, double expectAvgDelta, int unavailableSize) {
        List<Referer<IHello>> referers = buildDynamicReferers(size, true, initialMaxWeight, true, unavailableSize);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.RoundRobinSelector);
        processCheck("RR", referers, round, expectMaxDelta, expectAvgDelta, unavailableSize);
    }


    public void testWeightRingSelector() {
        int round = 100;
        // small size
        checkWR(51, 49, round, 1, 1, 0);

        // max node size of WR
        checkWR(256, 15, round, 1, 1, 0);

        // some nodes are unavailable
        double maxRatio = 0.4;
        double avgRatio = 0.1;
        checkWR(46, 75, round, round * maxRatio, round * avgRatio, 5);
        checkWR(231, 31, round, round * maxRatio, round * avgRatio, 35);
        maxRatio = 0.6;
        checkWR(211, 31, round, round * maxRatio, round * avgRatio, 45);
    }

    private void checkWR(int size, int initialMaxWeight, int round, double expectMaxDelta, double expectAvgDelta, int unavailableSize) {
        List<Referer<IHello>> referers = buildDynamicReferers(size, false, initialMaxWeight, true, unavailableSize);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.WeightedRingSelector);
        processCheck("WR", referers, round, expectMaxDelta, expectAvgDelta, unavailableSize);
    }

    public void testSlidingWindowWeightedRoundRobinSelector() {
        int size;
        int round = 100;
        // equals default window size，the accuracy is higher than sliding window.
        size = WeightRoundRobinLoadBalance.SlidingWindowWeightedRoundRobinSelector.DEFAULT_WINDOW_SIZE;
        checkSWWRR(size,
                WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_TOTAL_WEIGHT * 3 / size,
                round, 2, 1, 0);

        // less than default window size
        size = WeightRoundRobinLoadBalance.SlidingWindowWeightedRoundRobinSelector.DEFAULT_WINDOW_SIZE - 9;
        checkSWWRR(size,
                WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_TOTAL_WEIGHT * 3 / size,
                round, 2, 1, 0);

        // greater than default window size
        // sliding windows will reduce the accuracy of WRR, so the threshold should be appropriately increased.
        double maxRatio = 0.5;
        double avgRatio = 0.1;
        round = 200;
        size = 270;
        checkSWWRR(size, 45, round, round * maxRatio, round * avgRatio, 0);


        // some nodes are unavailable
        size = 260;
        checkSWWRR(size,
                WeightRoundRobinLoadBalance.WeightedRingSelector.MAX_TOTAL_WEIGHT * 3 / size,
                round, round * maxRatio, round * avgRatio, 10);
        size = 399;
        checkSWWRR(size, 67, round, round * maxRatio, round * avgRatio, 40);
    }

    private void checkSWWRR(int size, int initialMaxWeight, int round, double expectMaxDelta, double expectAvgDelta, int unavailableSize) {
        List<Referer<IHello>> referers = buildDynamicReferers(size, false, initialMaxWeight, true, unavailableSize);
        weightRoundRobinLoadBalance.onRefresh(referers);
        assertTrue(weightRoundRobinLoadBalance.selector instanceof WeightRoundRobinLoadBalance.SlidingWindowWeightedRoundRobinSelector);
        processCheck("SWWRR", referers, round, expectMaxDelta, expectAvgDelta, unavailableSize);
    }

    private void processCheck(String type, List<Referer<IHello>> referers, int round, double expectMaxDelta, double expectAvgDelta, int unavailableSize) {
        int totalWeight = 0;
        for (Referer<?> referer : referers) {
            if (!((MockDynamicReferer) referer).available) {
                continue;
            }
            totalWeight += ((MockDynamicReferer) referer).staticWeight;
        }

        for (int i = 0; i < totalWeight * round; i++) {
            weightRoundRobinLoadBalance.doSelect(null).call(null);
        }
        double maxDelta = 0.0;
        double totalDelta = 0.0;
        int unavailableCount = 0;

        for (Referer<?> referer : referers) {
            if (!((MockDynamicReferer) referer).available) {
                unavailableCount++;
            } else {
                MockDynamicReferer mdr = (MockDynamicReferer) referer;
                double ratio = ((double) mdr.count.get() / mdr.staticWeight);
                double delta = Math.abs(ratio - round);
                if (delta > maxDelta) {
                    maxDelta = delta;
                }
                totalDelta += delta;
                if (delta > expectMaxDelta) {
                    System.out.println(type + ": count=" + mdr.count.get() + ", staticWeight=" + mdr.staticWeight + ", ratio=" + ratio + ", delta=" + delta);
                }
                assertTrue(delta <= expectMaxDelta); // check max delta
            }
        }
        // avg delta
        double avgDelta = totalDelta / (referers.size() - unavailableSize);
        assertTrue(avgDelta - round < expectAvgDelta);
        System.out.println(String.format("%s: avgDeltaPercent=%.2f%%, maxDeltaPercent=%.2f%%, avgDelta=%.2f, maxDelta=%.2f", type, avgDelta * 100 / round, maxDelta * 100 / round, avgDelta, maxDelta));

        if (unavailableSize > 0) {
            assertEquals(unavailableSize, unavailableCount);
        }
    }

    private void triggerRefresh() {
        MetaUtil.clearCache();
        weightRoundRobinLoadBalance.refreshHoldersDynamicWeightTask(); // Manually trigger refresh tasks
    }

    private List<Referer<IHello>> buildDynamicReferers(int size, boolean sameStaticWeight, int maxWeight) {
        return buildDynamicReferers(size, sameStaticWeight, maxWeight, false, 0);
    }

    private List<Referer<IHello>> buildDynamicReferers(int size, boolean sameStaticWeight, int maxWeight, boolean adjust, int unavailableSize) {
        List<Referer<IHello>> referers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int weight = sameStaticWeight ? maxWeight : (int) (Math.random() * maxWeight);
            if (adjust) {
                weight = adjust(weight);
            }
            MockDynamicReferer<IHello> referer = new MockDynamicReferer(
                    new URL("motan2", "127.0.0.1", 8080 + i, "mockService"),
                    i, weight);
            if (i < unavailableSize) {
                referer.available = false;
            }
            referers.add(referer);
        }
        return referers;
    }

    private int adjust(int w) {
        if (w < AbstractWeightedLoadBalance.MIN_WEIGHT) {
            return AbstractWeightedLoadBalance.MIN_WEIGHT;
        } else if (w > AbstractWeightedLoadBalance.MAX_WEIGHT) {
            return AbstractWeightedLoadBalance.MAX_WEIGHT;
        } else {
            return w;
        }
    }
}