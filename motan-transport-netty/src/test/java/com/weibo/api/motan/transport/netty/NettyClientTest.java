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

import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.RequestIdGenerator;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

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

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello", parameters);
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName("com.weibo.api.motan.procotol.example.IHello");
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
        } catch (Exception e) {
        }
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
            assertTrue(e.getMessage().contains("NettyChannel is unavaliable"));
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
}
