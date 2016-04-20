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

package com.weibo.api.motan.cluster;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.ha.FailoverHaStrategy;
import com.weibo.api.motan.cluster.loadbalance.RandomLoadBalance;
import com.weibo.api.motan.cluster.support.ClusterSpi;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * Cluster test。
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-23
 */

public class ClusterTest extends BaseTestCase {

    private ClusterSpi<IHello> cluster = new ClusterSpi<IHello>();
    private List<Referer<IHello>> referers;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        HaStrategy<IHello> ha = new FailoverHaStrategy<IHello>();
        LoadBalance<IHello> lb = new RandomLoadBalance<IHello>();

        referers = new ArrayList<Referer<IHello>>();
        referers.add(mockery.mock(Referer.class, "ref1"));
        referers.add(mockery.mock(Referer.class, "ref2"));

        cluster.setUrl(new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class
                .getName()));
        cluster.setHaStrategy(ha);
        cluster.setLoadBalance(lb);
        cluster.onRefresh(referers);
        cluster.init();
    }

    @Test
    public void testCall() {
        final Request request = mockery.mock(Request.class);
        final Response rs = mockery.mock(Response.class);

        mockery.checking(new Expectations() {
            {
                allowing(any(Referer.class)).method("getUrl").withNoArguments();
                will(returnValue(new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 18080, Object.class
                        .getName())));
                allowing(any(Referer.class)).method("isAvailable").withNoArguments();
                will(returnValue(true));
                allowing(any(Referer.class)).method("call").with(same(request));
                will(returnValue(rs));

                atLeast(0).of(request).setRetries(0);
                will(returnValue(null));
                atLeast(0).of(request).getRetries();
                will(returnValue(0));
                atLeast(0).of(request).getMethodName();
                will(returnValue("get"));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void"));
            }
        });
        Response callRs = cluster.call(request);
        Assert.assertEquals(rs, callRs);
    }
}
