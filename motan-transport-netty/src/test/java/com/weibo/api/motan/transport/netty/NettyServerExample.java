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

package com.weibo.api.motan.transport.netty;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.AsyncUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.RequestIdGenerator;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 */
public class NettyServerExample {
    public static void main(String[] args) throws InterruptedException {
        URL url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.protocol.example.IHello");

        NettyServer nettyServer = new NettyServer(url, (channel, message) -> {
            Request request = (Request) message;

            System.out.println("[server] get request: requestId: " + request.getRequestId() + " method: " + request.getMethodName());

            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setValue("method: " + request.getMethodName() + " time: " + System.currentTimeMillis());

            return response;
        });

        nettyServer.open();
        System.out.println("~~~~~~~~~~~~~ Server open ~~~~~~~~~~~~~");

        Thread.sleep(100000);
    }

    @Test
    public void testServerTrace() throws InterruptedException {
        URL url = defaultUrl();
        url.addParameter("requestTimeout", "500");
        MotanSwitcherUtil.setSwitcherValue(MotanConstants.MOTAN_TRACE_INFO_SWITCHER, true);
        final Set<String> serverTraceKey = new HashSet<>();
        serverTraceKey.add(MotanConstants.TRACE_SRECEIVE);
        serverTraceKey.add(MotanConstants.TRACE_SDECODE);
        serverTraceKey.add(MotanConstants.TRACE_SEXECUTOR_START);
        serverTraceKey.add(MotanConstants.TRACE_PROCESS);
        serverTraceKey.add(MotanConstants.TRACE_SENCODE);
        serverTraceKey.add(MotanConstants.TRACE_SSEND);

        NettyServer nettyServer = new NettyServer(url, (channel, message) -> {
            final Request request = (Request) message;

            if (request instanceof Traceable) {
                if (((Traceable) request).getTraceableContext().getReceiveTime() != 0) {
                    serverTraceKey.remove(MotanConstants.TRACE_SRECEIVE);
                }
                serverTraceKey.removeAll(((Traceable) request).getTraceableContext().getTraceInfoMap().keySet());
            }
            final DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());
            response.addFinishCallback(() -> {
                serverTraceKey.removeAll(((Traceable) response).getTraceableContext().getTraceInfoMap().keySet());
                if (((Traceable) response).getTraceableContext().getSendTime() != 0) {
                    serverTraceKey.remove(MotanConstants.TRACE_SSEND);
                }
            }, null);
            return response;
        });
        nettyServer.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(url.getPath());
        request.setMethodName("hello");
        request.setParamtersDesc("void");
        TraceableContext requestTraceableContext = request.getTraceableContext();
        assertEquals(0, requestTraceableContext.getSendTime());
        assertEquals(0, requestTraceableContext.getReceiveTime());

        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();
        Response response;
        try {
            response = nettyClient.request(request);
            Object result = response.getValue();
            assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
            assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CONNECTION));
            assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CENCODE));
            assertFalse(0 == requestTraceableContext.getSendTime());
            if (response instanceof Traceable) {
                assertFalse(0 == ((Traceable) response).getTraceableContext().getReceiveTime());
                assertNotNull(((Traceable) response).getTraceableContext().getTraceInfo(MotanConstants.TRACE_CDECODE));
            }
        } catch (Exception e) {
            fail();
        }
        Thread.sleep(100);
        assertTrue(serverTraceKey.isEmpty());
        nettyClient.close();
        nettyServer.close();
    }

    @Test
    public void testServerAsync() throws TransportException, InterruptedException {
        URL url = defaultUrl();
        final AtomicInteger callbackCount = new AtomicInteger();
        NettyServer nettyServer = new NettyServer(url, (channel, message) -> {
            final Request request = (Request) message;
            Response response;
            if ("async".equals(request.getMethodName())) { // async result
                DefaultResponseFuture defaultResponseFuture = new DefaultResponseFuture(request, 0, "127.0.0.1");
                AsyncUtil.getDefaultCallbackExecutor().execute(() -> {
                    int sleepTime = (int) request.getArguments()[1];
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException ignore) {
                        }
                    }
                    if ("success".equals(request.getArguments()[0])) {
                        defaultResponseFuture.onSuccess("success");
                    } else { // exception
                        defaultResponseFuture.onFailure(new RuntimeException("fail"));
                    }
                });
                defaultResponseFuture.addFinishCallback(() -> callbackCount.incrementAndGet(), null);
                response = defaultResponseFuture;
            } else { // sync result
                DefaultResponse defaultResponse = new DefaultResponse();
                if ("success".equals(request.getArguments()[0])) {
                    defaultResponse.setValue("success");
                } else { // exception
                    defaultResponse.setException(new MotanBizException("process fail", new RuntimeException("fail")));
                }
                defaultResponse.addFinishCallback(() -> callbackCount.incrementAndGet(), null);
                response = defaultResponse;
            }
            return response;
        });
        nettyServer.open();

        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(url.getPath());
        request.setMethodName("sync");
        request.setParamtersDesc("java.lang.String");
        request.setArguments(new Object[]{"success"});
        // sync success
        Response response = nettyClient.request(request);
        assertEquals("success", response.getValue());
        Thread.sleep(2);
        assertEquals(1, callbackCount.get());

        // sync exception
        request.setArguments(new Object[]{"fail"});
        try {
            response = nettyClient.request(request);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof MotanBizException);
            assertEquals("fail", e.getCause().getMessage());
        }
        Thread.sleep(2);
        assertEquals(2, callbackCount.get());

        // async success
        request.setMethodName("async");
        request.setParamtersDesc("java.lang.String,int");
        request.setArguments(new Object[]{"success", 0}); // not sleep
        response = nettyClient.request(request);
        assertEquals("success", response.getValue());
        Thread.sleep(2);
        assertEquals(3, callbackCount.get());

        // async exception
        request.setArguments(new Object[]{"fail", 10}); // with sleep
        try {
            response = nettyClient.request(request);
            fail();
        } catch (Exception e) {
            assertTrue(e instanceof MotanBizException);
            assertEquals("fail", e.getCause().getMessage());
        }
        Thread.sleep(2);
        assertEquals(4, callbackCount.get());
        nettyClient.close();
        nettyServer.close();
    }

    private URL defaultUrl() {
        return new URL("motan", "localhost", 18080, "com.weibo.api.motan.protocol.example.IHello");
    }
}
