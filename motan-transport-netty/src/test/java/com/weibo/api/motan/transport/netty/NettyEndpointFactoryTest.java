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

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.Endpoint;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.transport.Server;
import com.weibo.api.motan.transport.support.HeartbeatClientEndpointManager;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-19
 * 
 */
public class NettyEndpointFactoryTest extends TestCase {

    @Test
    public void testCreateServer() {
        testNotShareChannel(true);
        testNotShareChannel(false);

        testShareChannel(true);
        testShareChannel(false);
    }

    private void testNotShareChannel(boolean isServer) {
        NettyEndpointFactory factory = new NettyEndpointFactory();
        MessageHandler handler = new ProviderMessageRouter();

        URL url = new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");

        Endpoint endpoint = createEndpoint(url, handler, isServer, factory);

        Assert.assertEquals(endpoint.getUrl().getUri(), url.getUri());

        url = new URL("motan", "localhost", 18081, "com.weibo.api.motan.procotol.example.IHello");
        endpoint = createEndpoint(url, handler, isServer, factory);
        Assert.assertEquals(endpoint.getUrl().getUri(), url.getUri());

        Assert.assertTrue(endpoint != createEndpoint(new URL("motan", "localhost", 18081, "com.weibo.api.motan.procotol.example.IHello"),
                handler, isServer, factory));

        if (isServer) {
            Assert.assertEquals(factory.getShallServerChannels().size(), 0);
        }

        if (isServer) {
            factory.safeReleaseResource((Server) endpoint, url);
        } else {
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 3);
            factory.safeReleaseResource((Client) endpoint, url);
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 2);
        }
    }

    private void testShareChannel(boolean isServer) {
        NettyEndpointFactory factory = new NettyEndpointFactory();
        MessageHandler handler = new ProviderMessageRouter();

        URL url1 = new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
        url1.addParameter(URLParamType.shareChannel.getName(), "true");

        Endpoint endpoint1 = createEndpoint(url1, handler, isServer, factory);

        Assert.assertEquals(endpoint1.getUrl().getServerPortStr(), url1.getServerPortStr());

        URL url2 = new URL("motan", "localhost", 18081, "com.weibo.api.motan.protocol.example.IHello1");
        url2.addParameter(URLParamType.shareChannel.getName(), "true");

        Endpoint endpoint2 = createEndpoint(url2, handler, isServer, factory);
        Assert.assertEquals(endpoint2.getUrl().getServerPortStr(), url2.getServerPortStr());

        URL url3 = new URL("motan", "localhost", 18081, "com.weibo.api.motan.protocol.example.IHello2");
        url3.addParameter(URLParamType.shareChannel.getName(), "true");

        Endpoint endpoint3 = createEndpoint(url3, handler, isServer, factory);

        if (isServer) {
            Assert.assertTrue(endpoint2 == endpoint3);
        } else {
            Assert.assertTrue(endpoint2 != endpoint3);
        }

        URL url4 = new URL("injvm", "localhost", 18081, "com.weibo.api.motan.protocol.example.IHello3");
        url4.addParameter(URLParamType.shareChannel.getName(), "true");
        Endpoint endpoint4 = null;

        if (isServer) {
            try {
                endpoint4 = createEndpoint(url4, handler, isServer, factory);
                Assert.assertTrue(false);
            } catch (Exception e) {
                Assert.assertTrue(true);
            }
        } else {
            try {
                endpoint4 = createEndpoint(url4, handler, isServer, factory);
                Assert.assertTrue(true);
            } catch (Exception e) {
                Assert.assertTrue(false);
            }
        }

        if (isServer) {
            Assert.assertEquals(factory.getShallServerChannels().size(), 2);
        }

        if (isServer) {
            factory.safeReleaseResource((Server) endpoint1, url1);
            factory.safeReleaseResource((Server) endpoint2, url2);
            factory.safeReleaseResource((Server) endpoint3, url3);
            Assert.assertEquals(factory.getShallServerChannels().size(), 0);
        } else {
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 4);
            factory.safeReleaseResource((Client) endpoint1, url1);
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 3);
            factory.safeReleaseResource((Client) endpoint2, url2);
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 2);
            factory.safeReleaseResource((Client) endpoint3, url3);
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 1);
            factory.safeReleaseResource((Client) endpoint4, url4);
            Assert.assertEquals(((HeartbeatClientEndpointManager) factory.getEndpointManager()).getClients().size(), 0);
        }
    }

    private Endpoint createEndpoint(URL url, MessageHandler messageHandler, boolean isServer, NettyEndpointFactory factory) {
        if (isServer) {
            return (Endpoint) factory.createServer(url, messageHandler);
        } else {
            return (Endpoint) factory.createClient(url);
        }
    }
}
