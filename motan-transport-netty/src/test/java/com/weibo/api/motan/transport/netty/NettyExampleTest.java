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

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.RequestIdGenerator;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-3
 * 
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class NettyExampleTest extends TestCase {

    private static URL url = null;

    static {
        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Codec.class);
        loader.addExtensionClass(MockDefaultRpcCodec.class);
    }

    @Before
    public void setUp() {
        url = new URL("netty", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
        url.addParameter(URLParamType.codec.getName(), "mockMotan");
        url.addParameter(URLParamType.requestTimeout.getName(), "2000");
    }

    @Test
    public void testNettyEncodeException() throws Exception {
        NettyServer nettyServer;
        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                // 序列化错误
                response.setValue(new UnSerializableClass());
                return response;
            }
        });

        nettyServer.open();
        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(url.getPath());
        request.setMethodName("helloSerializable");
        request.setParamtersDesc("com.weibo.api.motan.procotol.example.UnSerializableClass");
        request.setArguments(new Object[] {new UnSerializableClass()});

        try {
            nettyClient.request(request);
            Assert.assertFalse(true);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        DefaultRequest request1 = new DefaultRequest();
        request1.setRequestId(RequestIdGenerator.getRequestId());
        request1.setInterfaceName(url.getPath());
        request1.setMethodName("helloSerializable");
        request1.setParamtersDesc("void");
        try {
            nettyClient.request(request1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("error_code: 20002"));

        } finally {
            nettyClient.close();
            nettyServer.close();
        }

    }

    /**
     * @throws Exception
     */
    @Test
    public void testNettyRequestDecodeException() throws Exception {
        NettyServer nettyServer;
        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("success");
                return response;
            }
        });
        nettyServer.open();

        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(url.getPath());
        request.setMethodName("hello");
        request.setParamtersDesc("void");

        try {
            nettyClient.request(request);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("framework decode error"));
        } finally {
            nettyClient.close();
            nettyServer.close();
        }
    }

    @Test
    public void testNettyDecodeException() throws Exception {

        NettyServer nettyServer;
        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("error");
                return response;
            }
        });
        nettyServer.open();

        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(url.getPath());
        request.setMethodName("hello");
        request.setParamtersDesc("void");

        try {
            nettyClient.request(request);
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("response dataType not support"));
        } finally {
            nettyClient.close();
            nettyServer.close();
        }
    }

}

class UnSerializableClass {

    public String hello() {
        return "I am a unserializable class";
    }
}
