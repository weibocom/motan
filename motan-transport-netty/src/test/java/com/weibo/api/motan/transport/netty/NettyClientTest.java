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
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.RequestIdGenerator;
import com.weibo.api.motan.util.StatsUtil;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 */
public class NettyClientTest extends TestCase {

    private NettyServer nettyServer;
    private NettyClient nettyClient;
    private DefaultRequest request;
    private URL url;

    public void setUp() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.protocol.example.IHello", parameters);
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName("com.weibo.api.motan.protocol.example.IHello");
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

    public void tearDown() {
        nettyClient.close();
        nettyServer.close();
    }

    public void testNormal() throws TransportException {
        nettyClient = new NettyClient(url);
        nettyClient.open();

        Response response;
        response = nettyClient.request(request);
        Object result = response.getValue();

        Assert.assertNotNull(result);
        Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
    }

    public void testAsync() throws TransportException {
        nettyClient = new NettyClient(url);
        nettyClient.open();
        RpcContext.getContext().putAttribute(MotanConstants.ASYNC_SUFFIX, true);
        Response response;
        try {
            response = nettyClient.request(request);
            Assert.assertTrue(response instanceof ResponseFuture);
            Object result = response.getValue();
            Assert.assertNotNull(result);
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
        } finally {
            RpcContext.destroy();
        }
    }

    public void testAbNormal() throws TransportException {
        // requestTimeout 不可以小于等于0
        url.addParameter(URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getValue());
        nettyClient = new NettyClient(url);
        assertFalse(nettyClient.isAvailable());
        try {
            nettyClient.request(request);
            fail("Netty Client should not be active!");
        } catch (MotanServiceException e) {
            assertTrue(true);
        }

        // 模拟失败连接的次数大于或者等于设置的次数，client期望为不可用
        url.addParameter(URLParamType.fusingThreshold.getName(), "1");
        url.addParameter(URLParamType.requestTimeout.getName(), "1");
        nettyClient = new NettyClient(url);
        nettyClient.open();
        try {
            nettyClient.request(request);
        } catch (MotanServiceException e) {
            assertFalse(nettyClient.isAvailable());
            nettyClient.resetErrorCount();
            assertTrue(nettyClient.isAvailable());
        }
    }

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

    public void testLazyInit() throws TransportException {
        nettyServer.close();
        url.addParameter(URLParamType.lazyInit.getName(), "true");
        // async init connections
        url.addParameter(URLParamType.asyncInitConnection.getName(), "true");
        nettyClient = new NettyClient(url);
        nettyClient.open();
        assertTrue(nettyClient.isAvailable());
        nettyClient.close();

        // sync init connections
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        url.removeParameter(URLParamType.codec.getName());
        nettyClient = new NettyClient(url);
        nettyClient.open();
        assertTrue(nettyClient.isAvailable());

        nettyServer.open();
        try {
            Response response = nettyClient.request(request);
            assertEquals("method: hello requestId: " + request.getRequestId(), response.getValue());
        } finally {
            nettyClient.close();
            nettyServer.close();
        }
    }

    public void testClientAlive() throws TransportException, InterruptedException {
        nettyServer.close();
        // async init connections
        url.addParameter(URLParamType.asyncInitConnection.getName(), "true");
        nettyClient = new NettyClient(url);
        nettyClient.open();
        assertFalse(nettyClient.isAvailable());
        nettyClient.close();

        // sync init connections
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        url.removeParameter(URLParamType.codec.getName());
        nettyClient = new NettyClient(url);
        nettyClient.open();
        assertFalse(nettyClient.isAvailable());

        try {
            nettyClient.request(request);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("NettyChannel is unavailable"));
        }

        nettyServer.open();
        nettyClient.heartbeat(new DefaultRpcHeartbeatFactory().createRequest());
        Thread.sleep(50L);
        try {
            Response response = nettyClient.request(request);
            assertEquals("method: hello requestId: " + request.getRequestId(), response.getValue());
        } finally {
            nettyClient.close();
            nettyServer.close();
        }
    }

    static class NettyTestClient extends NettyClient {
        int statisticCount;

        public NettyTestClient(URL url) {
            super(url);
        }

        @Override
        public String statisticCallback() {
            statisticCount++;
            return super.statisticCallback();
        }
    }
}
