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

package com.weibo.api.motan.transport.netty4;


import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.RequestIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.*;

/**
 * @author maijunsheng
 * @author sunnights
 */
public class NettyClientTest {

    private NettyServer nettyServer;
    private NettyClient nettyClient;
    private DefaultRequest request;
    private URL url;
    private String interfaceName = "com.weibo.api.motan.procotol.example.IHello";

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, interfaceName, parameters);

        request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName("hello");
        request.setParamtersDesc("void");

        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());

                return response;
            }
        });

        nettyServer.open();
    }

    @After
    public void tearDown() {
        if (nettyClient != null) {
            nettyClient.close();
        }
        nettyServer.close();
    }

    @Test
    public void testNormal() {
        nettyClient = new NettyClient(url);
        nettyClient.open();

        Response response;
        try {
            response = nettyClient.request(request);
            Object result = response.getValue();

            Assert.assertNotNull(result);
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
        } catch (MotanServiceException e) {
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(false);
        }

    }

    @Test
    public void testAsync() {
        nettyClient = new NettyClient(url);
        nettyClient.open();
        RpcContext.getContext().putAttribute(MotanConstants.ASYNC_SUFFIX, true);
        Response response;
        try {
            response = nettyClient.request(request);
            Assert.assertTrue(response instanceof ResponseFuture);
            Object result = response.getValue();
            RpcContext.destroy();
            Assert.assertNotNull(result);
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
        } catch (MotanServiceException e) {
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(false);
        }

    }

    @Test
    public void testAbNormal() throws InterruptedException {
        // requestTimeout 不可以小于等于0
        url.addParameter(URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getValue());
        nettyClient = new NettyClient(url);
        // nettyClient未开启，状态为INIT
        try {
            nettyClient.request(request);
            fail("Netty Client should not be active!");
        } catch (MotanServiceException e) {
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }

        // 模拟失败连接的次数大于或者等于设置的次数，client期望为不可用
        url.addParameter(URLParamType.maxClientConnection.getName(), "10");
        url.addParameter(URLParamType.requestTimeout.getName(), "1");
        nettyClient = new NettyClient(url);
        nettyClient.open();

        int concurrent = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(concurrent);
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(concurrent);
        for (int i = 0; i < concurrent; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        cyclicBarrier.await();
                        nettyClient.request(request);
                    } catch (Exception e) {
                    }
                    countDownLatch.countDown();
                }
            }).start();
        }
        countDownLatch.await();

        assertFalse(nettyClient.isAvailable());
        nettyClient.resetErrorCount();
        assertTrue(nettyClient.isAvailable());
    }

    @Test
    public void testGetChannel() throws InterruptedException {
        int connections = 3;
        url.addParameter(URLParamType.maxClientConnection.getName(), String.valueOf(connections));
        NettyTestClient nettyClient = new NettyTestClient(url);
        this.nettyClient = nettyClient;
        nettyClient.open();

        for (Object o : nettyClient.getObjects()) {
            Channel channel = (Channel) o;
            channel.close();
            assertFalse(channel.isAvailable());
        }

        Channel channel = null;
        try {
            channel = nettyClient.getChannel();
        } catch (Exception e) {
        }
        assertTrue(channel == null);

        Thread.sleep(1000);
        for (Object o : nettyClient.getObjects()) {
            channel = (Channel) o;
            assertTrue(channel.isAvailable());
        }
    }

    class NettyTestClient extends NettyClient {

        public NettyTestClient(URL url) {
            super(url);
        }

        public Object[] getObjects() {
            return super.objects;
        }

        public Channel getChannel() {
            return super.getObject();
        }
    }
}
