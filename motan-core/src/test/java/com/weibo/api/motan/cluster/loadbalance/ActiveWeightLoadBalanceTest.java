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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.rpc.Referer;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ActiveWeightLoadBalanceTest {

    private int lowActive = 1;
    private int smallSize = 5;
    private int largeSize = 15;

    private int testLoop = 100;

    @Test
    public void testSelect() {
        for (int i = 0; i < testLoop; i++) {
            allAvailableCluster(smallSize);
            allAvailableCluster(largeSize);

            partOfUnAvailableCluster(smallSize, 1);
            partOfUnAvailableCluster(smallSize, smallSize / 2);
            partOfUnAvailableCluster(smallSize, smallSize - 1);

            partOfUnAvailableCluster(largeSize, 1);
            partOfUnAvailableCluster(largeSize, largeSize / 2);
            partOfUnAvailableCluster(largeSize, largeSize - 1);


            allUnAvailableCluster(smallSize);
            allUnAvailableCluster(largeSize);
        }
    }

    private void allAvailableCluster(int refererSize) {
        ActiveWeightLoadBalance balance = createBalance(refererSize, 0);

        Referer referer = balance.select(null);

        Assert.assertNotNull(referer);

        if (refererSize <= ActiveWeightLoadBalance.MAX_REFERER_COUNT) {
            Assert.assertEquals(referer.activeRefererCount(), lowActive);
        } else {
            Assert.assertTrue(refererSize - ActiveWeightLoadBalance.MAX_REFERER_COUNT >= referer.activeRefererCount());
        }

        List<Referer> referersHolder = new ArrayList<Referer>();
        balance.selectToHolder(null, referersHolder);

        if (refererSize <= ActiveWeightLoadBalance.MAX_REFERER_COUNT) {
            Assert.assertEquals(referersHolder.size(), refererSize);
            check(referersHolder);
        } else {
            Assert.assertEquals(referersHolder.size(), ActiveWeightLoadBalance.MAX_REFERER_COUNT);
            check(referersHolder);
        }
    }

    private void partOfUnAvailableCluster(int refererSize, int unAvailableSize) {
        if (refererSize <= unAvailableSize) {
            throw new RuntimeException("refereSize: " + refererSize + " unAvailableSize: " + unAvailableSize);
        }

        ActiveWeightLoadBalance balance = createBalance(refererSize, unAvailableSize);

        Referer referer = balance.select(null);

        Assert.assertNotNull(referer);
        Assert.assertTrue(referer.isAvailable());

        int availableSize = (refererSize - unAvailableSize);

        if (availableSize <= ActiveWeightLoadBalance.MAX_REFERER_COUNT) {
            Assert.assertTrue(referer.activeRefererCount() - lowActive - unAvailableSize <= 0);
        } else {
            Assert.assertTrue(refererSize - ActiveWeightLoadBalance.MAX_REFERER_COUNT + unAvailableSize >= referer.activeRefererCount());
        }

        List<Referer> referersHolder = new ArrayList<Referer>();
        balance.selectToHolder(null, referersHolder);

        if (availableSize <= ActiveWeightLoadBalance.MAX_REFERER_COUNT) {
            Assert.assertEquals(referersHolder.size(), availableSize);
            check(referersHolder);
        } else {
            Assert.assertEquals(referersHolder.size(), ActiveWeightLoadBalance.MAX_REFERER_COUNT);
            check(referersHolder);
        }
    }

    private static void check(List<Referer> referersHolder) {
        int prefix = 0;
        for (Referer aReferersHolder : referersHolder) {
            Assert.assertTrue(aReferersHolder.isAvailable());

            Assert.assertTrue(aReferersHolder.activeRefererCount() > prefix);
            prefix = aReferersHolder.activeRefererCount();
        }
    }

    private void allUnAvailableCluster(int refererSize) {
        ActiveWeightLoadBalance balance = createBalance(refererSize, refererSize);

        try {
            balance.select(null);
            Assert.assertTrue(false);
        } catch (Exception e) {
            // 应该有异常抛出
            Assert.assertTrue(true);
        }

        try {
            List<Referer> referersHolder = new ArrayList<Referer>();
            balance.selectToHolder(null, referersHolder);
            Assert.assertTrue(false);
        } catch (Exception e) {
            // 应该有异常抛出
            Assert.assertTrue(true);
        }
    }

    private ActiveWeightLoadBalance createBalance(int refererSize, int unAvailableSize) {
        List<MockReferer> referers = new ArrayList<MockReferer>();

        for (int i = 0; i < refererSize; i++) {
            MockReferer referer = new MockReferer();
            referer.active = lowActive + i;

            referers.add(referer);
        }

        Collections.shuffle(referers);

        for (int i = 0; i < unAvailableSize; i++) {
            referers.get(i).available = false;
        }

        ActiveWeightLoadBalance balance = new ActiveWeightLoadBalance();
        balance.onRefresh(referers);

        return balance;
    }

    public static void main(String[] args) {
        List<MockReferer> referers = new ArrayList<MockReferer>();

        for (int i = 0; i < 20; i++) {
            MockReferer referer = new MockReferer();
            referer.active = 1 + i;

            referers.add(referer);
        }

        for (int j = 0; j < 20; j++) {
            List<Referer> copy = new ArrayList<Referer>(referers);
            Collections.shuffle(copy);

            String buffer = "[";
            for (Referer referer : copy) {
                buffer += referer.activeRefererCount() + ",";
            }

            Collections.sort(copy, new ActiveWeightLoadBalance.LowActivePriorityComparator());

            int prefix = -1;
            for (Referer aCopy : copy) {
                if (aCopy.activeRefererCount() <= prefix) {
                    throw new RuntimeException("bug bug bug");
                }
                prefix = aCopy.activeRefererCount();
            }
        }

    }
}
