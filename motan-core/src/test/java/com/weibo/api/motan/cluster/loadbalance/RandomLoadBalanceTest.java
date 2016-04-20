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
 * RandomLoadbalance test
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class RandomLoadBalanceTest extends AbstractLoadBalanceTest {

    private RandomLoadBalance<IHello> randomLoadBalance = new RandomLoadBalance<IHello>();
    private final int falseCount = 5;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        randomLoadBalance.onRefresh(referers);
        mockery.checking(new Expectations() {
            {
                int i = 0;
                for (Referer<IHello> ref : referers) {
                    boolean rs = i++ >= falseCount;
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(rs));
                }
            }
        });
    }

    public void testSelect() {

        final Request request = mockery.mock(Request.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(request).getArguments();
                will(returnValue(new Object[] {1, 2, 3}));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void_"));
            }
        });

        for (int i = 0; i < 10; i++) {
            Referer<IHello> ref = randomLoadBalance.select(request);
            for (int j = 0; j < falseCount; j++) {
                assertNotSame(ref, referers.get(j));
            }
        }
    }

    public void testSelectToHolder() {
        final Request request = mockery.mock(Request.class);
        mockery.checking(new Expectations() {
            {
                atLeast(1).of(request).getArguments();
                will(returnValue(new Object[] {1, 2, 3}));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void_"));
            }
        });

        for (int i = 0; i < 10; i++) {
            List<Referer<IHello>> refHolder = new ArrayList<Referer<IHello>>();
            randomLoadBalance.selectToHolder(request, refHolder);
            assertEquals(refHolder.size(), (referers.size() - falseCount));
        }
    }
}
