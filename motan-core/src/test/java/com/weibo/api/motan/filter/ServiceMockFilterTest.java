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

package com.weibo.api.motan.filter;

import java.util.HashMap;
import java.util.Map;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.protocol.example.IHelloMock;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StatsUtil;

import org.jmock.Expectations;

public class ServiceMockFilterTest extends BaseTestCase {
    private ServiceMockFilter smfilter = new ServiceMockFilter();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testCall() {
        final Request request = mockery.mock(Request.class);
        final Response response = mockery.mock(Response.class);
        final URL url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, IHello.class.getName());
        url.addParameter(URLParamType.mock.getName(), "default");

        final Caller<IHello> caller = mockery.mock(Caller.class);
        final Map<String, String> attachments = new HashMap<String, String>();
        attachments.put(URLParamType.host.getName(), URLParamType.host.getValue());
        attachments.put(URLParamType.application.getName(), URLParamType.application.getValue());
        attachments.put(URLParamType.module.getName(), URLParamType.module.getValue());

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(caller).getUrl();
                will(returnValue(url));
                exactly(1).of(caller).call(request);
                will(returnValue(response));
                atLeast(1).of(caller).getInterface();
                will(returnValue(IHello.class));
                exactly(1).of(request).getInterfaceName();
                will(returnValue(IHello.class.getName()));
                exactly(1).of(request).getMethodName();
                will(returnValue("hello"));
                exactly(1).of(request).getParamtersDesc();
                will(returnValue("void"));
                exactly(1).of(request).getRequestId();
                will(returnValue(100L));
                exactly(1).of(request).getArguments();
                will(returnValue(null));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });

        Response mockResponse = smfilter.filter(caller, request);

        assertEquals(mockResponse.getValue(), new IHelloMock().hello());
    }

    public void testPerformance() {
        final AccessStatisticFilter asFilter = new AccessStatisticFilter();

        final Request request = mockery.mock(Request.class);
        final Response response = mockery.mock(Response.class);
        final URL url =
                new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName());
        url.addParameter(URLParamType.mock.getName(), "default");
        url.addParameter(URLParamType.mean.getName(), "1");
        url.addParameter(URLParamType.p90.getName(), "2");
        url.addParameter(URLParamType.p99.getName(), "4");
        url.addParameter(URLParamType.p999.getName(), "10");
        url.addParameter(URLParamType.errorRate.getName(), "1");

        final Map<String, String> attachments = new HashMap<String, String>();
        attachments.put(URLParamType.host.getName(), URLParamType.host.getValue());
        attachments.put(URLParamType.application.getName(), URLParamType.application.getValue());
        attachments.put(URLParamType.module.getName(), URLParamType.module.getValue());


        final Caller<IHello> caller = mockery.mock(Caller.class);

        mockery.checking(new Expectations() {
            {
                atLeast(1).of(caller).getUrl();
                will(returnValue(url));
                atLeast(1).of(caller).call(request);
                will(returnValue(response));
                atLeast(1).of(caller).getInterface();
                will(returnValue(IHello.class));
                atLeast(1).of(request).getInterfaceName();
                will(returnValue(IHello.class.getName()));
                atLeast(1).of(request).getMethodName();
                will(returnValue("hello"));
                atLeast(1).of(request).getParamtersDesc();
                will(returnValue("void"));
                atLeast(1).of(request).getRequestId();
                will(returnValue(100L));
                atLeast(1).of(request).getArguments();
                will(returnValue(null));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });

        final Caller<IHello> smcaller = new Caller<IHello>() {

            @Override
            public void init() {
                caller.init();
            }

            @Override
            public void destroy() {
                caller.destroy();
            }

            @Override
            public boolean isAvailable() {
                return caller.isAvailable();
            }

            @Override
            public String desc() {
                return caller.desc();
            }

            @Override
            public URL getUrl() {
                return caller.getUrl();
            }

            @Override
            public Class<IHello> getInterface() {
                return caller.getInterface();
            }

            @Override
            public Response call(Request request) {
                return smfilter.filter(caller, request);
            }

        };

        for (int i = 0; i < 1000; i++) {
            asFilter.filter(smcaller, request);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        StatsUtil.logAccessStatistic(true);
    }
}
