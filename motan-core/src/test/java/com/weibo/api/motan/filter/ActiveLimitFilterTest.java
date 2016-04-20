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

import org.jmock.Expectations;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.RpcStats;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * 类说明
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class ActiveLimitFilterTest extends BaseTestCase {

    private ActiveLimitFilter activeLimitFilter = new ActiveLimitFilter();

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @SuppressWarnings("unchecked")
    public void testFilter() {
        final Request request = mockery.mock(Request.class);
        final Response response = mockery.mock(Response.class);
        final Caller<IHello> caller = mockery.mock(Caller.class);
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(URLParamType.actives.getName(), "" + 3);
        final URL url =
                new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName(),
                        parameters);

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

            }
        });

        activeLimitFilter.filter(caller, request);

        for (int i = 0; i < 4; i++) {
            RpcStats.beforeCall(url, request);
        }
        try {
            activeLimitFilter.filter(caller, request);
            assertFalse(true);
        } catch (MotanServiceException e) {
            assertEquals(MotanErrorMsgConstant.SERVICE_REJECT, e.getMotanErrorMsg());
        } catch (Exception e) {
            assertFalse(true);
        }
    }
}
