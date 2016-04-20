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

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.Referer;

/**
 * 
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
        referers = new ArrayList<Referer<IHello>>();
        for (int i = 0; i < 10; i++) {
            final Referer<IHello> ref = mockery.mock(Referer.class, "ref_" + i);
            referers.add(ref);
        }
    }
}
