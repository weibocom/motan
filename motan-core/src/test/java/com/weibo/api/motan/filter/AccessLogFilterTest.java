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
import com.weibo.api.motan.log.DefaultLogService;
import com.weibo.api.motan.log.LogService;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.NetUtils;
import org.jmock.Expectations;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit test
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class AccessLogFilterTest extends BaseTestCase {

    private AccessLogFilter accessLogFilter = new AccessLogFilter();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        accessLogFilter = new AccessLogFilter();
    }

    public void testCall() throws Exception {
        final URL url = getDefaultUrl();
        url.addParameter(URLParamType.accessLog.getName(), String.valueOf(false));
        final Map<String, String> attachments = getDefaultAttachment();
        checkProcess(url, attachments, false);

        url.addParameter(URLParamType.accessLog.getName(), String.valueOf(true));
        checkProcess(url, attachments, true);
    }


    public void testSwitcher() throws Exception {
        URL url = getDefaultUrl();
        url.addParameter(URLParamType.accessLog.getName(), String.valueOf(false));
        final Map<String, String> attachments = getDefaultAttachment();
        checkProcess(url, attachments, false);

        MotanSwitcherUtil.setSwitcherValue(AccessLogFilter.ACCESS_LOG_SWITCHER_NAME, true);
        checkProcess(url, attachments, true);

        MotanSwitcherUtil.setSwitcherValue(AccessLogFilter.ACCESS_LOG_SWITCHER_NAME, false);
        checkProcess(url, attachments, false);
    }

    public void testTraceLog() throws Exception {
        URL url = getDefaultUrl();
        url.addParameter(URLParamType.accessLog.getName(), String.valueOf(false)); // not log access
        final Map<String, String> attachments = getDefaultAttachment();
        // 强制log开关关闭
        MotanSwitcherUtil.setSwitcherValue(AccessLogFilter.ACCESS_LOG_SWITCHER_NAME, false);
        checkProcess(url, attachments, false);

        // set trace log attachment
        attachments.put(MotanConstants.ATT_PRINT_TRACE_LOG, "true");
        checkProcess(url, attachments, true);

    }

    private URL getDefaultUrl() {
        return new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalIpString(), 0, RegistryService.class.getName());
    }

    private Map<String, String> getDefaultAttachment() {
        Map<String, String> attachments = new HashMap<>();
        attachments.put(URLParamType.host.getName(), URLParamType.host.getValue());
        attachments.put(URLParamType.application.getName(), URLParamType.application.getValue());
        attachments.put(URLParamType.module.getName(), URLParamType.module.getValue());
        return attachments;
    }

    private void checkProcess(URL url, Map<String, String> attachments, boolean isProcess) throws Exception {
        checkProcessNormal(url, attachments, isProcess);
        checkProcessWithTraceable(url, attachments, isProcess, true, false);
        checkProcessWithTraceable(url, attachments, isProcess, false, false);
        checkProcessWithTraceable(url, attachments, isProcess, false, true);
    }


    private void checkProcessNormal(URL url, Map<String, String> attachments, boolean isProcess) throws Exception {
        resetMockery();
        final Request request = mockery.mock(Request.class);
        final Caller<IHello> caller = mockery.mock(Caller.class);
        final LogService logService = mockery.mock(LogService.class);
        LoggerUtil.setLogService(logService);
        mockery.checking(new Expectations() {
            {
                allowing(caller).getUrl();
                will(returnValue(url));
                atLeast(1).of(caller).call(request);
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });
        if (isProcess) {
            mockery.checking(new Expectations() {
                {
                    exactly(1).of(request).getInterfaceName();
                    will(returnValue(IHello.class.getName()));
                    exactly(1).of(request).getMethodName();
                    will(returnValue("get"));
                    exactly(1).of(request).getParamtersDesc();
                    will(returnValue("param_desc"));
                    exactly(1).of(logService).accessLog(with(any(String.class)));
                    allowing(request).getRequestId();
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    never(request).getInterfaceName();
                    never(request).getMethodName();
                    never(logService).accessLog(with(any(String.class)));
                }
            });
        }
        accessLogFilter.filter(caller, request);
        mockery.assertIsSatisfied();

        LoggerUtil.setLogService(new DefaultLogService());
    }

    private void checkProcessWithTraceable(URL url, Map<String, String> attachments, boolean isProcess, boolean isServerEnd, boolean timeout) throws Exception {
        resetMockery();
        final DefaultRequest request = mockery.mock(DefaultRequest.class);
        final DefaultResponse response = timeout ? null : new DefaultResponse();
        final Caller<IHello> caller;
        if (isServerEnd) {
            caller = mockery.mock(Provider.class);
        } else {
            caller = mockery.mock(Referer.class);
        }
        final LogService logService = mockery.mock(LogService.class);
        final TraceableContext requestTraceableContext = mockery.mock(TraceableContext.class, "requestTraceableContext");
        final TraceableContext responseTraceableContext = mockery.mock(TraceableContext.class, "responseTraceableContext");
        if (!timeout) {
            Field field = DefaultResponse.class.getDeclaredField("traceableContext");
            field.setAccessible(true);
            field.set(response, responseTraceableContext);
        }

        LoggerUtil.setLogService(logService);
        mockery.checking(new Expectations() {
            {
                allowing(caller).getUrl();
                will(returnValue(url));
                atLeast(1).of(caller).call(request);
                will(returnValue(response));
                allowing(request).getAttachments();
                will(returnValue(attachments));
            }
        });
        if (isProcess) {
            mockery.checking(new Expectations() {
                {
                    exactly(1).of(request).getInterfaceName();
                    will(returnValue(IHello.class.getName()));
                    exactly(1).of(request).getMethodName();
                    will(returnValue("get"));
                    exactly(1).of(request).getParamtersDesc();
                    will(returnValue("param_desc"));
                    exactly(1).of(logService).accessLog(with(any(String.class)));
                    allowing(request).getRequestId();
                    allowing(request).getTraceableContext();
                    will(returnValue(requestTraceableContext));
                    if (isServerEnd) {
                        exactly(1).of(requestTraceableContext).getReceiveTime(); // request receive time for server end
                        will(returnValue(10L));
                        exactly(1).of(responseTraceableContext).getSendTime(); // response send time for server end
                        will(returnValue(15L));
                    } else {
                        if (!timeout) {
                            exactly(1).of(requestTraceableContext).getSendTime(); // request send time for client end
                            will(returnValue(10L));
                            exactly(1).of(responseTraceableContext).getReceiveTime(); // response receive time for client end
                            will(returnValue(18L));
                        }
                    }
                }
            });
        } else {
            mockery.checking(new Expectations() {
                {
                    never(request).getInterfaceName();
                    never(request).getMethodName();
                    never(logService).accessLog(with(any(String.class)));
                    never(request).getTraceableContext();
                }
            });
        }
        accessLogFilter.filter(caller, request);
        if (!timeout) {
            response.onFinish();
        }
        if (isServerEnd) {
            Thread.sleep(100);
        }
        mockery.assertIsSatisfied();

        LoggerUtil.setLogService(new DefaultLogService());
    }

    private void resetMockery() throws Exception {
        setUp();
    }
}
