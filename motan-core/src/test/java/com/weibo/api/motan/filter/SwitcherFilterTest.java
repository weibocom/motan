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

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.NetUtils;
import org.jmock.Expectations;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class SwitcherFilterTest extends BaseTestCase {

    Request request;
    Response response;
    Caller<IHello> caller;
    URL url;
    Map<String, String> attachments;
    private SwitcherFilter filter = new SwitcherFilter();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        request = mockery.mock(Request.class);
        response = mockery.mock(Response.class);
        caller = mockery.mock(Caller.class);
        url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName());
        attachments = new HashMap<String, String>();
        attachments.put(URLParamType.host.getName(), URLParamType.host.getValue());
        attachments.put(URLParamType.application.getName(), URLParamType.application.getValue());
        attachments.put(URLParamType.module.getName(), URLParamType.module.getValue());
    }

    @SuppressWarnings("unchecked")
    public void testFilter() {
        mockery.checking(new Expectations() {
            {
                oneOf(caller).call(request);
                will(returnValue(response));
                atLeast(1).of(caller).getUrl();
                will(returnValue(url));
                atLeast(1).of(request).getMethodName();
                will(returnValue("mock_mothod_name"));
                atLeast(1).of(request).getParamtersDesc();
                will(returnValue("mock_param_desc"));
                atLeast(1).of(request).getInterfaceName();
                will(returnValue("mock_class_name"));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });

        filter.filter(caller, request);
    }

    public void testPutSwitcher() {
        MotanSwitcherUtil.setSwitcherValue("mock_class_name", true);

        mockery.checking(new Expectations() {
            {
                oneOf(caller).call(request);
                will(returnValue(response));
                atLeast(1).of(caller).getUrl();
                will(returnValue(url));
                atLeast(1).of(request).getMethodName();
                will(returnValue("mock_mothod_name"));
                atLeast(1).of(request).getParamtersDesc();
                will(returnValue("mock_param_desc"));
                atLeast(1).of(request).getInterfaceName();
                will(returnValue("mock_class_name"));
                atLeast(1).of(request).getRequestId();
                will(returnValue(1L));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });


        filter.filter(caller, request);
    }

    public void testOnSwitcher() {
        MotanSwitcherUtil.setSwitcherValue("mock_class_name",true);

        mockery.checking(new Expectations() {
            {
                oneOf(caller).call(request);
                will(returnValue(response));
                atLeast(2).of(caller).getUrl();
                will(returnValue(url));
                atLeast(2).of(request).getMethodName();
                will(returnValue("mock_mothod_name"));
                atLeast(2).of(request).getParamtersDesc();
                will(returnValue("mock_param_desc"));
                atLeast(2).of(request).getInterfaceName();
                will(returnValue("mock_class_name"));
                atLeast(1).of(request).getRequestId();
                will(returnValue(1L));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });


        Response resultOnSwitcher = filter.filter(caller, request);

        assertNotNull(resultOnSwitcher);
        assertTrue(resultOnSwitcher.getException().getMessage().contains("Request false for switcher is on"));

        MotanSwitcherUtil.setSwitcherValue("mock_class_name",false);

        Response resultOffSwitcher = filter.filter(caller, request);

        assertEquals(response, resultOffSwitcher);
    }
}
