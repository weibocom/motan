/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.filter;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhanglei28
 * @date 2024/1/5.
 */
public class FaultInjectionFilterTest extends BaseTestCase {
    static long defaultDelay = 10;
    FaultInjectionFilter filter = new FaultInjectionFilter();
    List<FaultInjectionFilter.FaultInjectionConfig> configList = new ArrayList<>();
    AtomicLong processTime = new AtomicLong(defaultDelay); // default expect process time
    Caller<IHello> caller;
    DefaultRequest request;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        clearConfigs();
        // init request
        request = new DefaultRequest();
        request.setRequestId(123456L);
        request.setInterfaceName("com.weibo.test.FaultTest");
        request.setMethodName("hello");
        Object[] arguments = new Object[]{processTime};
        request.setArguments(arguments);

        // init caller
        caller = new Caller<IHello>() {
            @Override
            public Class<IHello> getInterface() {
                return IHello.class;
            }

            @Override
            public Response call(Request request) {
                AtomicLong time = (AtomicLong) request.getArguments()[0];
                DefaultResponse response;
                if (request.getMethodName().equals("exception")) {
                    response = MotanFrameworkUtil.buildErrorResponse(request, new MotanServiceException("exception"));
                } else {
                    response = new DefaultResponse("success");
                }
                response.setProcessTime(time.get());
                return response;
            }

            @Override
            public void init() {
            }

            @Override
            public void destroy() {
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String desc() {
                return "";
            }

            @Override
            public URL getUrl() {
                return null;
            }
        };
    }

    public void testFilter() {
        // ==== Mismatch ====
        // no configs
        check(processTime.get());
        processTime.set(20);
        check(processTime.get());

        // not matched
        FaultInjectionFilter.FaultInjectionConfig config = buildDefaultConfig("com.weibo.test.NotFaultTest");
        configList.add(config);
        updateConfigs();
        check(processTime.get());

        // matched，then not matched
        config.servicePattern = request.getInterfaceName();
        updateConfigs();
        check(processTime.get() + defaultDelay);
        config.servicePattern = request.getInterfaceName() + "notMatch";
        updateConfigs();
        check(processTime.get());

        // clear configs
        clearConfigs();
        check(processTime.get());

        // ==== matched ====
        // with delay time
        config.delayTime = 33;
        config.servicePattern = request.getInterfaceName();
        updateConfigs();
        check(processTime.get() + config.delayTime);

        // with delay ratio
        processTime.set(100);
        config.delayTime = 0;
        config.delayRatio = 0.25f;
        updateConfigs();
        check((long) (processTime.get() * (1 + config.delayRatio)));

        config.delayRatio = 0.75f;
        updateConfigs();
        check((long) (processTime.get() * (1 + config.delayRatio)));

        config.delayRatio = 1.25f;
        updateConfigs();
        check((long) (processTime.get() * (1 + config.delayRatio)));

        // with exception
        config.exceptionPercent = 35;
        config.delayTime = 33;
        config.delayRatio = 0;
        updateConfigs();
        check(processTime.get() + config.delayTime, 0, 8, MotanServiceException.class);

        config.exceptionPercent = 100;
        updateConfigs();
        check(processTime.get() + config.delayTime, 0, 1, MotanServiceException.class);

        config.exceptionPercent = 120;
        updateConfigs();
        check(processTime.get() + config.delayTime, 0, 1, MotanServiceException.class);

        // with exception time
        config.exceptionTime = 17;
        config.exceptionPercent = 80;
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 2, MotanServiceException.class);

        // with exception type
        config.exceptionPercent = 100;
        config.exceptionType = IllegalAccessException.class.getName();
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 1, IllegalAccessException.class);

        // method not matched
        config.methodPattern = "xxx";
        updateConfigs();
        check(processTime.get());

        // method matched
        config.methodPattern = request.getMethodName();
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 1, IllegalAccessException.class);

        // service fuzzy matching
        config.servicePattern = "com(.)+test\\.Fau(.)*";
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 1, IllegalAccessException.class);

        // method fuzzy matching
        config.methodPattern = "he(.)*";
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 1, IllegalAccessException.class);

        // ==== multi configs ====
        FaultInjectionFilter.FaultInjectionConfig config1 = buildDefaultConfig("notMatchedService", null, 55, 0);
        FaultInjectionFilter.FaultInjectionConfig config2 = buildDefaultConfig("xxx", "echo", 0, 0.5f);

        // only matched one config
        configList.clear();
        configList.add(config1); // 不命中
        configList.add(config); // 命中
        configList.add(config2); // 不命中
        updateConfigs();
        check(processTime.get() + config.delayTime, config.exceptionTime, 1, IllegalAccessException.class);

        // multiple matched configs priority order
        config.delayTime = 10;
        config1.delayTime = 20;
        config2.delayTime = 30;
        config1.servicePattern = request.getInterfaceName();
        config2.servicePattern = "com.(.)*";
        config2.methodPattern = "hello";
        updateConfigs();
        check(processTime.get() + config1.delayTime);

        // mismatch
        config.servicePattern = "notMatch";
        config1.servicePattern = "not(.*)";
        config2.methodPattern = "methodNotMatch";
        updateConfigs();
        check(processTime.get());
    }

    public void testAsyncRequest() {
        caller = new Caller<IHello>() {
            @Override
            public Class<IHello> getInterface() {
                return IHello.class;
            }

            @Override
            public Response call(Request request) {
                AtomicLong time = (AtomicLong) request.getArguments()[0];
                final ResponseFuture response = new DefaultResponseFuture(request, 100, new URL(MotanConstants.PROTOCOL_MOTAN2, "localhost", 0, "tempService"));
                new Thread(() -> {
                    try {
                        Thread.sleep(time.get());
                    } catch (InterruptedException ignore) {
                    }
                    DefaultResponse tempResponse;
                    if (request.getMethodName().equals("exception")) {
                        tempResponse = MotanFrameworkUtil.buildErrorResponse(request, new MotanServiceException("exception"));
                        tempResponse.setProcessTime(time.get());
                        response.onFailure(tempResponse);
                    } else {
                        tempResponse = new DefaultResponse("success");
                        tempResponse.setProcessTime(time.get());
                        response.onSuccess(tempResponse);
                    }
                }).start();
                return response;
            }

            @Override
            public void init() {
            }

            @Override
            public void destroy() {
            }

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            public String desc() {
                return "";
            }

            @Override
            public URL getUrl() {
                return null;
            }
        };

        FaultInjectionFilter.FaultInjectionConfig config = buildDefaultConfig(request.getInterfaceName());
        config.delayTime = 33;
        configList.add(config);
        updateConfigs();
        checkAsync(processTime.get() + config.delayTime);
    }

    private void check(long expectProcessTime) {
        Response response = filter.filter(caller, request);
        assertEquals(expectProcessTime, response.getProcessTime());
        assertNull(response.getException());
    }

    private void check(long expectProcessTime, long exceptionTime, int maxLoop, Class<? extends Exception> expectException) {
        Response response;
        Exception exception = null;
        for (int i = 0; i < maxLoop; i++) {
            response = filter.filter(caller, request);
            if (response.getException() != null) {
                exception = response.getException();
                // verify process time of exception
                assertEquals(exceptionTime, response.getProcessTime());
            } else { // verify process time of normal request
                assertEquals(expectProcessTime, response.getProcessTime());
            }
        }
        assertNotNull(exception);
        assertTrue(exception instanceof MotanAbstractException);
        if (exception instanceof MotanBizException) {
            assertSame(expectException, exception.getCause().getClass());
        }
    }

    private void checkAsync(long expectProcessTime) {
        Response response = filter.filter(caller, request);
        ResponseFuture responseFuture = (ResponseFuture) response;
        responseFuture.getValue(); // sync get result
        assertEquals(expectProcessTime, response.getProcessTime());
        assertNull(response.getException());
    }

    private void updateConfigs() {
        FaultInjectionFilter.FaultInjectionUtil.updateConfigs(configList);
    }

    private void clearConfigs() {
        FaultInjectionFilter.FaultInjectionUtil.clearConfigs();
    }

    private FaultInjectionFilter.FaultInjectionConfig buildDefaultConfig(String service) {
        return buildDefaultConfig(service, null, defaultDelay, 0F);
    }

    private FaultInjectionFilter.FaultInjectionConfig buildDefaultConfig(String service, String method, long delay, float delayRatio) {
        FaultInjectionFilter.FaultInjectionConfig config = new FaultInjectionFilter.FaultInjectionConfig();
        config.servicePattern = service;
        config.methodPattern = method;
        config.delayTime = delay;
        config.delayRatio = delayRatio;
        return config;
    }
}