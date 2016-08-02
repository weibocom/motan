/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.transport.netty4.yar;

import static org.junit.Assert.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.protocol.yar.AttachmentRequest;
import com.weibo.api.motan.protocol.yar.YarMessageRouter;
import com.weibo.api.motan.protocol.yar.YarProtocolUtil;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.transport.netty4.http.Netty4HttpServer;
import com.weibo.yar.YarProtocol;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

/**
 * 
 * @Description YarMessageHandlerWarpperTest
 * @author zhanglei
 * @date 2016年7月27日
 *
 */
public class YarMessageHandlerWarpperTest {
    public String uri = "/testpath?param1=a&param2=b&param3=c";

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testHandle() throws Exception {
        YarRequest yarRequest = new YarRequest(123, "JSON", "testmethod", new Object[] {"params", 456});
        final YarResponse yarResponse = YarProtocolUtil.buildDefaultErrorResponse("test err", "JSON");
        YarMessageHandlerWarpper handler = new YarMessageHandlerWarpper(new YarMessageRouter() {

            @Override
            public Object handle(Channel channel, Object message) {
                AttachmentRequest request = (AttachmentRequest) message;
                verifyAttachments(request.getAttachments());
                return yarResponse;
            }
        });
        FullHttpResponse httpResponse = (FullHttpResponse) handler.handle(new MockChannel(), buildHttpRequest(yarRequest, uri));

        assertNotNull(httpResponse);
        assertNotNull(httpResponse.content());
        YarResponse retYarResponse = getYarResponse(httpResponse);
        assertNotNull(retYarResponse);
        assertEquals(yarResponse, retYarResponse);
    }

    @Test
    public void testAbnormal() throws Exception {
        final String errmsg = "rpc process error";
        YarMessageHandlerWarpper handler = new YarMessageHandlerWarpper(new YarMessageRouter() {

            @Override
            public Object handle(Channel channel, Object message) {
                throw new RuntimeException(errmsg);
            }
        });
        // yar协议无法解析
        FullHttpResponse httpResponse = (FullHttpResponse) handler.handle(new MockChannel(), buildHttpRequest(null, uri));
        assertNotNull(httpResponse);
        assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
        YarResponse retYarResponse = getYarResponse(httpResponse);
        assertNotNull(retYarResponse);
        assertNotNull(retYarResponse.getError());

        // yar协议可以正常解析，但后续处理异常
        YarRequest yarRequest = new YarRequest(123, "JSON", "testmethod", new Object[] {"params", 456});
        httpResponse = (FullHttpResponse) handler.handle(new MockChannel(), buildHttpRequest(yarRequest, uri));
        assertNotNull(httpResponse);
        assertEquals(HttpResponseStatus.OK, httpResponse.getStatus());
        retYarResponse = getYarResponse(httpResponse);
        assertNotNull(retYarResponse);
        assertEquals(errmsg, retYarResponse.getError());
    }

    private void verifyAttachments(Map<String, String> attachments) {
        String[] params = uri.substring(uri.indexOf("?") + 1).split("&");
        for (String param : params) {
            String k = param.split("=")[0];
            String v = param.split("=")[1];
            assertTrue(attachments.containsKey(k));
            assertEquals(v, attachments.get(k));
        }
    }

    private YarResponse getYarResponse(FullHttpResponse httpResponse) throws Exception {
        ByteBuf buf = httpResponse.content();
        final byte[] contentBytes = new byte[buf.readableBytes()];
        buf.getBytes(0, contentBytes);
        YarResponse yarResponse = YarProtocol.buildResponse(contentBytes);
        return yarResponse;
    }

    private FullHttpRequest buildHttpRequest(YarRequest yarRequest, String requestPath) throws Exception {
        PooledByteBufAllocator allocator = new PooledByteBufAllocator();
        ByteBuf buf = allocator.buffer(2048, 1024 * 1024);
        if (yarRequest != null) {
            buf.writeBytes(YarProtocol.toProtocolBytes(yarRequest));
        }
        FullHttpRequest httpReqeust = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, requestPath, buf);
        return httpReqeust;
    }

    class MockChannel extends Netty4HttpServer {
        public MockChannel() {
            super(null, null);
        }

        public MockChannel(URL url, MessageHandler messageHandler) {
            super(url, messageHandler);
        }

        @Override
        public Response request(Request request) throws TransportException {
            return new DefaultResponse();
        }
    }

}
