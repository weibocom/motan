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

import org.jmock.Expectations;

import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;

/**
 * 
 * Unit test
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-27
 */

public class ConsistentHashLoadBalanceTest extends AbstractLoadBalanceTest {

    private ConsistentHashLoadBalance<IHello> consistentHashLoadBalance = new ConsistentHashLoadBalance<IHello>();


    @Override
    public void setUp() throws Exception {
        super.setUp();
        consistentHashLoadBalance.onRefresh(referers);
    }

    public void testSelect() {
        final Request request = mockery.mock(Request.class);
        mockery.checking(new Expectations() {
            {
                for (Referer<IHello> ref : referers) {
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                }
                atLeast(1).of(request).getArguments();
                will(returnValue(new Object[] {1, 2, 3}));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void_"));
            }
        });

        Referer<IHello> ref1 = consistentHashLoadBalance.select(request);
        for (int i = 0; i < 100; i++) {
            Referer<IHello> ref2 = consistentHashLoadBalance.select(request);
            assertEquals(ref1, ref2);
        }
    }

    public void testSelect2() {
        final Request request = mockery.mock(Request.class);
        mockery.checking(new Expectations() {
            {
                int i = 0;
                for (Referer<IHello> ref : referers) {
                    boolean rs = i++ % 2 == 1;
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(rs));
                }
                atLeast(1).of(request).getArguments();
                will(returnValue(new Object[] {1, 2, 3}));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void_"));
            }
        });

        Referer<IHello> ref1 = consistentHashLoadBalance.select(request);
        for (int i = 0; i < 100; i++) {
            Referer<IHello> ref2 = consistentHashLoadBalance.select(request);
            assertEquals(ref1, ref2);
        }
    }
}
