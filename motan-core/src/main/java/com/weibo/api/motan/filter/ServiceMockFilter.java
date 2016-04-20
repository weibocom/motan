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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.ReflectUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * mock serivce。测试场景使用
 */
@SpiMeta(name = "mock")
@Activation(sequence = 100)
public class ServiceMockFilter implements Filter {

    private static String RETURN_PREFIX = "return";

    private static ConcurrentHashMap<String, MockInfo> mockServices = new ConcurrentHashMap<String, MockInfo>();

    public static boolean isDefault(String value) {
        return "true".equalsIgnoreCase(value) || "default".equalsIgnoreCase(value);
    }

    private static MockInfo getServiceStat(URL url) {
        MockInfo info = mockServices.get(url.getIdentity());
        if (info == null) {
            info = new MockInfo(url);
            mockServices.putIfAbsent(url.getIdentity(), info);
            info = mockServices.get(url.getIdentity());
        }
        return info;
    }

    @Override
    public Response filter(Caller<?> caller, Request request) {

        // Do nothing when mock is empty.
        String mockServiceName = caller.getUrl().getParameter(URLParamType.mock.getName());
        if (StringUtils.isEmpty(mockServiceName) || "false".equals(mockServiceName)) {
            return caller.call(request);
        }

        MockInfo info = getServiceStat(caller.getUrl());

        DefaultResponse response = new DefaultResponse();

        if (mockServiceName.startsWith(RETURN_PREFIX)) {
            String value = mockServiceName.substring(RETURN_PREFIX.length());
            try {
                info.callNum.addAndGet(1);

                long sleepTime = caclSleepTime(info);
                Thread.sleep(sleepTime);

                response.setValue(parseMockValue(value));
            } catch (RuntimeException e) {
                if (e.getCause() != null) {
                    response.setException(new MotanBizException("mock service call process error", e.getCause()));
                } else {
                    response.setException(new MotanBizException("mock service call process error", e));
                }
            } catch (Exception e) {
                throw new IllegalStateException("Illegal mock json value in <motan:service ... mock=\"" + mockServiceName + "\" />");
            }
        } else {
            try {
                Class<?> mockClass =
                        isDefault(mockServiceName) ? ReflectUtil.forName(caller.getInterface().getName() + "Mock") : ReflectUtil
                                .forName(mockServiceName);
                if (!caller.getInterface().isAssignableFrom(mockClass)) {
                    throw new MotanFrameworkException("The mock implemention class " + mockClass.getName() + " not implement interface "
                            + caller.getInterface().getName());
                }
                try {
                    mockClass.getConstructor();
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("No such empty constructor \"public " + mockClass.getSimpleName()
                            + "()\" in mock implemention class " + mockClass.getName());
                }

                String methodDesc = ReflectUtil.getMethodDesc(request.getMethodName(), request.getParamtersDesc());

                Method[] methods = mockClass.getMethods();

                boolean invoke = false;
                for (Method method : methods) {
                    if (methodDesc.equals(ReflectUtil.getMethodDesc(method))) {
                        Object value = invoke(mockClass.newInstance(), method, request.getArguments(), info);
                        response.setValue(value);
                        invoke = true;
                        break;
                    }
                }
                if (!invoke) {
                    throw new MotanFrameworkException("Mock method is not found." + methodDesc);
                }

            } catch (ClassNotFoundException e) {
                throw new MotanFrameworkException("Mock service is not found."
                        + (isDefault(mockServiceName) ? caller.getInterface().getName() + "Mock" : mockServiceName));
            } catch (Exception e) {
                if (e.getCause() != null) {
                    response.setException(new MotanBizException("mock service call process error", e.getCause()));
                } else {
                    response.setException(new MotanBizException("mock service call process error", e));
                }
            }
        }
        return response;
    }

    private Object invoke(Object clz, Method method, Object[] args, MockInfo info) throws InterruptedException, InvocationTargetException, IllegalAccessException {

        info.callNum.addAndGet(1);

        long sleepTime = caclSleepTime(info);
        Thread.sleep(sleepTime);

        return method.invoke(clz, args);
    }

    // Sleep invoke based on SLA
    private long caclSleepTime(MockInfo info) {
        double rMean = info.totalSleepTime.doubleValue() / info.callNum.get();

        long sleepTime;

        int n = new Random().nextInt(1000);

        long delta = (long) (rMean - info.mean + 1);
        if (n < 900) {
            sleepTime = info.p90;
        } else if (900 <= n && n < 990) {
            sleepTime = info.p99;
        } else if (990 <= n && n < 999) {
            sleepTime = info.p999;
        } else {
            sleepTime = info.p999 + 1;
        }

        // Use 0ms to offset the mean time.
        sleepTime = delta > 0 ? 0 : sleepTime;

        info.totalSleepTime.addAndGet(sleepTime);

        // Throw runtimeException when errorRate is defined.
        if (info.errorRate != 0) {
            int rate = 1;
            while (info.errorRate * rate < 1) {
                rate *= 10;
            }
            if (new Random().nextInt(rate) == 1) {
                throw new RuntimeException();
            }
        }

        return sleepTime;
    }

    public Object parseMockValue(String mock) throws Exception {
        return parseMockValue(mock, null);
    }

    public Object parseMockValue(String mock, Type[] returnTypes) {
        Object value;
        if ("empty".equals(mock)) {
            value = ReflectUtil.getEmptyObject(returnTypes != null && returnTypes.length > 0 ? (Class<?>) returnTypes[0] : null);
        } else if ("null".equals(mock)) {
            value = null;
        } else if ("true".equals(mock)) {
            value = true;
        } else if ("false".equals(mock)) {
            value = false;
        } else if (mock.length() >= 2 && (mock.startsWith("\"") && mock.endsWith("\"") || mock.startsWith("\'") && mock.endsWith("\'"))) {
            value = mock.subSequence(1, mock.length() - 1);
        } else if (returnTypes != null && returnTypes.length > 0 && returnTypes[0] == String.class) {
            value = mock;
        } else {
            value = mock;
        }
        return value;
    }

    public static class MockInfo {

        private long mean;
        private long p90;
        private long p99;
        private long p999;
        private double errorRate;

        private AtomicLong callNum = new AtomicLong(0);
        private AtomicLong totalSleepTime = new AtomicLong(0);

        public MockInfo(URL url) {
            mean = Long.valueOf(url.getParameter(URLParamType.mean.getName(), URLParamType.mean.getValue()));
            p90 = Long.valueOf(url.getParameter(URLParamType.p90.getName(), URLParamType.p90.getValue()));
            p99 = Long.valueOf(url.getParameter(URLParamType.p99.getName(), URLParamType.p99.getValue()));
            p999 = Long.valueOf(url.getParameter(URLParamType.p999.getName(), URLParamType.p999.getValue()));
            errorRate = Double.valueOf(url.getParameter(URLParamType.errorRate.getName(), URLParamType.errorRate.getValue()));

        }
    }
}
