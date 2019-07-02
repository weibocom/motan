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
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.RequestIdGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 * 
 */
public class NettyServerExample {
    public static void main(String[] args) throws InterruptedException {
        URL url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");

        NettyServer nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;

                System.out.println("[server] get request: requestId: " + request.getRequestId() + " method: " + request.getMethodName());

                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " time: " + System.currentTimeMillis());

                return response;
            }
        });

        nettyServer.open();
        System.out.println("~~~~~~~~~~~~~ Server open ~~~~~~~~~~~~~");

        Thread.sleep(100000);
    }

    @Test
    public void testServerTrace() throws InterruptedException {
        String interfaceName = "com.weibo.api.motan.protocol.example.IHello";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestTimeout", "500");
        URL url = new URL("netty", "localhost", 18080, interfaceName, parameters);

        MotanSwitcherUtil.setSwitcherValue(MotanConstants.MOTAN_TRACE_INFO_SWITCHER, true);
        final Set<String> serverTraceKey = new HashSet<>();
        serverTraceKey.add(MotanConstants.TRACE_SRECEIVE);
        serverTraceKey.add(MotanConstants.TRACE_SDECODE);
        serverTraceKey.add(MotanConstants.TRACE_SEXECUTOR_START);
        serverTraceKey.add(MotanConstants.TRACE_PROCESS);
        serverTraceKey.add(MotanConstants.TRACE_SENCODE);
        serverTraceKey.add(MotanConstants.TRACE_SSEND);

        NettyServer nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
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
                response.addFinishCallback(new Runnable() {
                    @Override
                    public void run() {
                        serverTraceKey.removeAll(((Traceable) response).getTraceableContext().getTraceInfoMap().keySet());
                        if (((Traceable) response).getTraceableContext().getSendTime() != 0) {
                            serverTraceKey.remove(MotanConstants.TRACE_SSEND);
                        }
                    }
                }, null);
                return response;
            }
        });
        nettyServer.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName("hello");
        request.setParamtersDesc("void");
        TraceableContext requestTraceableContext = request.getTraceableContext();
        Assert.assertEquals(0, requestTraceableContext.getSendTime());
        Assert.assertEquals(0, requestTraceableContext.getReceiveTime());

        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        Response response;
        try {
            response = nettyClient.request(request);
            Object result = response.getValue();
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
            Assert.assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CONNECTION));
            Assert.assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CENCODE));
            Assert.assertFalse(0 == requestTraceableContext.getSendTime());
            if (response instanceof Traceable) {
                Assert.assertFalse(0 == ((Traceable) response).getTraceableContext().getReceiveTime());
                Assert.assertNotNull(((Traceable) response).getTraceableContext().getTraceInfo(MotanConstants.TRACE_CDECODE));
            }
        } catch (Exception e) {
            fail();
        }
        Thread.sleep(100);
        Assert.assertTrue(serverTraceKey.isEmpty());
        nettyClient.close();
        nettyServer.close();
    }
}
