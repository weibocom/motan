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

import com.weibo.api.motan.mock.MockDynamicReferer;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.weibo.api.motan.cluster.loadbalance.WeightRoundRobinLoadBalanceTest.buildDynamicReferers;

/**
 * @author zhanglei28
 * @date 2024/3/28.
 */
public class GroupWeightLoadBalanceTest extends TestCase {

    public void testSelect() {
        // Test using different internal LB
        testDoSelect("random");
        testDoSelect("roundrobin");
        testDoSelect("wrr");
        testDoSelect("localFirst");
        testDoSelect("consistent");
        testDoSelect("activeWeight");
        testDoSelect("configurableWeight");
    }

    public void testSelectWithRefererWeight() {
        GroupWeightLoadBalanceWrapper<IHello> gwlb = new GroupWeightLoadBalanceWrapper<>("wrr");
        gwlb.init(new URL("mock", "localhost", 0, "testService"));
        String weightString = "group1:3,group2:2,group3:1";
        List<TestItem> testItems = buildTestItems(weightString, 8, false, 20, 0);
        int round = 1000;
        checkGroupWeight(gwlb, weightString, testItems, round);

        // check group referer weight
        for (TestItem item : testItems) {
            int totalCount = 0, totalWeight = 0;
            for (Referer<?> referer : item.referers) {
                MockDynamicReferer<?> dynamicReferer = (MockDynamicReferer<?>) referer;
                totalCount += dynamicReferer.count.get();
                totalWeight += dynamicReferer.staticWeight;
            }
            int expectGroupRound = totalCount / totalWeight;
            for (Referer<?> referer : item.referers) {
                MockDynamicReferer<?> dynamicReferer = (MockDynamicReferer<?>) referer;
                double delta = Math.abs(expectGroupRound - dynamicReferer.count.get() / (double) dynamicReferer.staticWeight) / expectGroupRound;
                assertTrue(delta < 0.2);
            }
        }
    }

    private void testDoSelect(String loadBalanceName) {
        // Test using different internal LB
        GroupWeightLoadBalanceWrapper<IHello> gwlb = new GroupWeightLoadBalanceWrapper<>(loadBalanceName);
        gwlb.init(new URL("mock", "localhost", 0, "testService"));
        // single group
        checkSingleGroup(gwlb); // no weightString
        checkGroupWeight(gwlb, "group1:100");
        // referesh
        checkGroupWeight(gwlb, "group2:10");

        // multi groups
        checkGroupWeight(gwlb, "group1:100,group2:80");
        // group refresh
        checkGroupWeight(gwlb, "group1:33,group2:60");
        // reuse some group
        checkGroupWeight(gwlb, "group2:40,group3:10,group4:3,group5:100");
        // all new group
        checkGroupWeight(gwlb, "group6:41,group7:17,group8:23");
    }

    private void checkGroupWeight(GroupWeightLoadBalanceWrapper<IHello> gwlb, String weightString) {
        checkGroupWeight(gwlb, weightString, buildTestItems(weightString, 5), 100);
    }


    private void checkGroupWeight(GroupWeightLoadBalanceWrapper<IHello> gwlb, String weightString, List<TestItem> testItems, int round) {
        gwlb.setWeightString(weightString);
        List<Referer<IHello>> referers = new ArrayList<>();
        int totalWeight = 0;
        for (TestItem testItem : testItems) {
            referers.addAll(testItem.referers);
            Collections.shuffle(referers);
            totalWeight += testItem.groupWeight;
        }
        gwlb.onRefresh(referers);

        for (int i = 0; i < totalWeight * round; i++) {
            gwlb.doSelect(new DefaultRequest()).call(null);
        }

        double totalDelta = 0.0;
        for (TestItem testItem : testItems) {
            int totalCount = 0;
            for (Referer<IHello> referer : testItem.referers) {
                totalCount += ((MockDynamicReferer<?>) referer).count.get();
            }
            double delta = Math.abs(totalCount / (double) testItem.groupWeight - round) / round;
            assertTrue(delta < 0.001); // single group delta
            totalDelta += delta;
        }
        assertTrue(totalDelta / testItems.size() < 0.001); // avg group delta
        gwlb.destroy();
    }

    private void checkSingleGroup(GroupWeightLoadBalanceWrapper<IHello> gwlb) {
        gwlb.setWeightString(null);
        List<Referer<IHello>> referers = buildDynamicReferers(17, true, 5);
        gwlb.onRefresh(referers);
        int round = 100;
        for (int i = 0; i < round; i++) {
            gwlb.doSelect(new DefaultRequest()).call(null);
        }
        int totalCount = 0;
        for (Referer<IHello> referer : referers) {
            totalCount += ((MockDynamicReferer<?>) referer).count.get();
        }
        assertEquals(round, totalCount);
    }

    private List<TestItem> buildTestItems(String weightString, int refererSize) {
        return buildTestItems(weightString, refererSize, true, 5, 0);
    }

    private List<TestItem> buildTestItems(String weightString, int refererSize, boolean sameStaticWeight, int maxWeight, int unavailable) {
        String[] groupsAndWeights = weightString.split(",");
        List<TestItem> testItems = new ArrayList<>(groupsAndWeights.length);
        for (String groupsAndWeight : groupsAndWeights) {
            String[] gw = groupsAndWeight.split(":");
            testItems.add(new TestItem(buildDynamicReferers(refererSize, sameStaticWeight, maxWeight, true, unavailable, gw[0]),
                    Integer.parseInt(gw[1]), gw[0]));
        }
        return testItems;
    }

    static class TestItem {
        List<Referer<IHello>> referers;
        int groupWeight;
        String group;

        public TestItem(List<Referer<IHello>> referers, int groupWeight, String group) {
            this.referers = referers;
            this.groupWeight = groupWeight;
            this.group = group;
        }
    }
}