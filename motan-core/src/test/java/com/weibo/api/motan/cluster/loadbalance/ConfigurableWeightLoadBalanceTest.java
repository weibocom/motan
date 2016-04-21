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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

import org.junit.Test;

import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;

/**
 * @author chengya1
 * @author zhanglei
 */
public class ConfigurableWeightLoadBalanceTest {

    private ConfigurableWeightLoadBalance<IHello> balance;

    @Test
    public void testDoSelect() {
        int[] groupWeight = new int[] {2, 3, 5};
        Map<String, AtomicInteger> counter = generate(3, groupWeight, new int[] {3, 4, 5});

        for (int j = 0; j < 100; j++) {
            int size = 100;
            for (int i = 0; i < size; i++) {
                Referer referer = balance.doSelect(new DefaultRequest());
                String group = referer.getServiceUrl().getGroup();
                counter.get(group).incrementAndGet();
            }

            for (String key : counter.keySet()) {
                float total = size * (j + 1);
                float ratio = counter.get(key).get() * 10 / total;
                int weight = groupWeight[Integer.parseInt(key.substring("group".length()))];
                Assert.assertTrue(Math.abs(weight - ratio) < 2); // 权重误差不超过阈值。

            }
        }

    }

    @Test
    public void testDoSelectToHolder() {
        generate(3, new int[] {2, 3, 5}, new int[] {3, 4, 5});
        List<Referer<IHello>> list = new ArrayList<Referer<IHello>>();
        balance.doSelectToHolder(new DefaultRequest(), list);
        assertTrue(list.size() > 0 && list.size() <= ConfigurableWeightLoadBalance.MAX_REFERER_COUNT);

    }


    private Map<String, AtomicInteger> generate(int groupNum, int[] groupWeight, int[] groupSize) {
        StringBuilder sb = new StringBuilder();
        List<Referer<IHello>> list = new ArrayList<Referer<IHello>>();
        Map<String, AtomicInteger> counterMap = new HashMap<String, AtomicInteger>();
        for (int i = 0; i < groupNum; i++) {
            // build weight
            sb.append("group").append(i).append(":").append(groupWeight[i]).append(",");
            // build counter
            counterMap.put("group" + i, new AtomicInteger(0));
            // build param map
            Map<String, String> param = new HashMap<String, String>();
            param.put("group", "group" + i);
            // build referers
            for (int j = 0; j < groupSize[i]; j++) {
                list.add(new MockReferer<IHello>(new URL("motan", "localhost", i * 100 + j, "com.weibo.Hello", param)));
            }
        }

        balance = new ConfigurableWeightLoadBalance();
        balance.setWeightString(sb.substring(0, sb.length() - 1));
        balance.onRefresh(list);
        return counterMap;
    }
}
