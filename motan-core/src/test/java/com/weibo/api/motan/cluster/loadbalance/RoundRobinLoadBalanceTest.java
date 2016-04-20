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
import java.util.List;

import org.jmock.Expectations;

import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;

/**
 * 
 * RoundRobin loadBalance
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class RoundRobinLoadBalanceTest extends AbstractLoadBalanceTest {

    private RoundRobinLoadBalance<IHello> roundRobinLoadBalance = new RoundRobinLoadBalance<IHello>();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        roundRobinLoadBalance.onRefresh(referers);
    }

    public void testSelect() {
        Request request = mockery.mock(Request.class);
        mockery.checking(new Expectations() {
            {
                for (int i = 0; i < referers.size(); i++) {
                    if (i % 2 == 0) {
                        atLeast(0).of(referers.get(i)).isAvailable();
                        will(returnValue(true));
                    } else {
                        atLeast(0).of(referers.get(i)).isAvailable();
                        will(returnValue(false));
                    }
                }
            }
        });

        Referer<IHello> ref = roundRobinLoadBalance.select(request);
        for (int i = 0; i < referers.size(); i++) {
            if (i % 2 == 1) {
                assertNotSame(ref, referers.get(i));
            }
        }

        List<Referer<IHello>> refHolder = new ArrayList<Referer<IHello>>();
        roundRobinLoadBalance.selectToHolder(request, refHolder);
        assertEquals(refHolder.size(), referers.size() / 2);
    }
}
