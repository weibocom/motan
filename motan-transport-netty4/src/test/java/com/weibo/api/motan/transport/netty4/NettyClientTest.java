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
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.RequestIdGenerator;
import com.weibo.api.motan.util.StatsUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
    private String interfaceName = "com.weibo.api.motan.protocol.example.IHello";

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, interfaceName, parameters);
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");

        request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName("hello");
        request.setParamtersDesc("void");

        nettyServer = new NettyServer(url, (channel, message) -> {
            Request request = (Request) message;
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());

            return response;
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
        } catch (Exception e) {
            fail();
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
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testAbNormal() {
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
            fail();
        }
    }

    @Test
    public void testAbNormal2() throws Exception {
        // 模拟失败连接的次数大于或者等于设置的次数，client期望为不可用
        url.addParameter(URLParamType.fusingThreshold.getName(), "1");
        url.addParameter(URLParamType.requestTimeout.getName(), "1");
        NettyTestClient nettyClient = new NettyTestClient(url);
        this.nettyClient = nettyClient;
        nettyClient.open();
        nettyClient.getChannel0();

        Thread.sleep(50L);
        try {
            nettyClient.request(request);
        } catch (MotanServiceException e) {
            assertFalse(nettyClient.isAvailable());
            nettyClient.resetErrorCount();
            assertTrue(nettyClient.isAvailable());
        }
    }

    @Test
    public void testForceClose() throws Exception {
        nettyServer.close();
        nettyServer = new NettyServer(url, new ProviderMessageRouter());
        nettyServer.open();
        NettyTestClient nettyClient = new NettyTestClient(url);
        this.nettyClient = nettyClient;
        nettyClient.open();
        assertTrue(nettyClient.isAvailable());
        assertFalse(nettyClient.forceClosed);

        // provider not exist
        request.setInterfaceName("unknownService");
        int forceCloseTimes = url.getIntParameter(URLParamType.fusingThreshold.getName(), URLParamType.fusingThreshold.getIntValue()) / 2;
        for (int i = 0; i < forceCloseTimes + 1; i++) {
            try {
                nettyClient.request(request);
                fail();
            } catch (MotanServiceException e) {
                if (i < forceCloseTimes) {
                    // check provide not exist exception
                    assertTrue(nettyClient.isAvailable());
                    assertEquals(e.getErrorCode(), MotanErrorMsgConstant.PROVIDER_NOT_EXIST.getErrorCode());
                    assertTrue(e.getOriginMessage().contains(MotanErrorMsgConstant.PROVIDER_NOT_EXIST_EXCEPTION_PREFIX));
                } else {
                    assertTrue(e.getErrorCode() != MotanErrorMsgConstant.PROVIDER_NOT_EXIST.getErrorCode());
                }
            }
        }

        // check force close
        assertFalse(nettyClient.isAvailable());
        assertTrue(nettyClient.forceClosed);
        assertTrue(nettyClient.isClosed());
        nettyClient.heartbeat(null); // not process heartbeat when force closed
        // check statistic
        assertEquals(0, nettyClient.statisticCount);
        assertTrue(nettyClient.statisticCallback().startsWith("type:MOTAN_FORCE_CLOSED_NODE_STAT"));
        assertEquals(1, nettyClient.statisticCount);
        StatsUtil.logStatisticCallback();
        assertEquals(2, nettyClient.statisticCount);
        nettyClient.close(); // nettyClient will be removed from StatisticCallbackMap
        StatsUtil.logStatisticCallback();
        assertEquals(2, nettyClient.statisticCount);
    }

    @Test
    public void testClient() throws InterruptedException {
        nettyServer.close();

        NettyTestClient nettyClient = new NettyTestClient(url);
        this.nettyClient = nettyClient;
        nettyClient.open();

        for (Channel channel : nettyClient.getChannels()) {
            assertFalse(channel.isAvailable());
        }
        assertFalse(nettyClient.isAvailable());

        nettyServer.open();
        nettyClient.heartbeat(new DefaultRpcHeartbeatFactory().createRequest());

        Thread.sleep(50L);
        for (Channel channel : nettyClient.getChannels()) {
            assertTrue(channel.isAvailable());
        }
        assertTrue(nettyClient.isAvailable());
    }

    @Test
    public void testLazyInit() {
        // test open init
        url.addParameter(URLParamType.lazyInit.getName(), "false");
        NettyTestClient testClient = new NettyTestClient(url);
        nettyClient = testClient;
        nettyClient.open();
        assertTrue(testClient.getPoolInit());
        testClient.close();

        // test lazy init
        url.addParameter(URLParamType.lazyInit.getName(), "true");
        testClient = new NettyTestClient(url);
        nettyClient = testClient;
        nettyClient.open();
        assertFalse(testClient.getPoolInit());

        Response response;
        try {
            response = nettyClient.request(request);
            assertTrue(testClient.getPoolInit());

            Object result = response.getValue();
            Assert.assertNotNull(result);
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
        } catch (Exception e) {
            fail();
        }
    }

    static class NettyTestClient extends NettyClient {
        int statisticCount;

        public NettyTestClient(URL url) {
            super(url);
        }

        public ArrayList<Channel> getChannels() {
            return super.channels;
        }

        public Channel getChannel0() {
            return super.getChannel();
        }

        public boolean getPoolInit() {
            return super.poolInit;
        }

        @Override
        public String statisticCallback() {
            statisticCount++;
            return super.statisticCallback();
        }
    }
}
