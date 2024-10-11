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

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;

import java.util.ArrayList;
import java.util.List;

/**
 * Precreate referers.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class AbstractLoadBalanceTest extends BaseTestCase {

    protected List<Referer<IHello>> referers;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        referers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final Referer<IHello> ref = mockery.mock(Referer.class, "ref_" + i);
            referers.add(ref);
        }
    }

    // TODO
    public void testDefaultDoSelectToHolder() {
        referers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            referers.add(new MockReferer<>(new URL("motan", "localhost", i * 100 + i, "com.weibo.Hello")));
        }

        LoadBalance<IHello> lb = new AbstractLoadBalance<IHello>() {
            @Override
            protected Referer<IHello> doSelect(Request request) {
                return getReferers().get(3);
            }
        };

        lb.onRefresh(referers);
        Request request = new DefaultRequest();
        Referer<IHello> fixedOne = lb.select(request);
        MockReferer<IHello> unavailableOne = (MockReferer<IHello>) referers.get(6);
        unavailableOne.available = false;
        for (int i = 0; i < 5; i++) { // repeat verification N times
            List<Referer<IHello>> refererHolder = new ArrayList<>();
            lb.selectToHolder(request, refererHolder);
            assertSame(fixedOne, refererHolder.get(0)); // the first one is the fixed one
            for (int j = 1; j < refererHolder.size(); j++) { // subsequent referers are not the first one
                assertNotSame(fixedOne, refererHolder.get(j));
                assertNotSame(unavailableOne, refererHolder.get(j));
            }
        }
    }
}
