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
package com.weibo.api.motan.transport.netty4.http;

import static org.junit.Assert.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * 
 * @Description NettyHttpRequestHandlerTest
 * @author zhanglei
 * @date 2016年7月27日
 *
 */
public class NettyHttpRequestHandlerTest {
    public static JUnit4Mockery mockery = null;

    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testChannelRead0() throws Exception {
        final MessageHandler messageHandler = mockery.mock(MessageHandler.class);
        final ChannelHandlerContext ctx = mockery.mock(ChannelHandlerContext.class);
        final FullHttpResponse response = mockery.mock(FullHttpResponse.class);
        mockery.checking(new Expectations() {
            {
                allowing(ctx).write(with(any(FullHttpResponse.class)));
                will(new CustomAction("verify") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        FullHttpResponse actualResponse = (FullHttpResponse) invocation.getParameter(0);
                        assertNotNull(actualResponse);
                        assertEquals(response, actualResponse);
                        return null;
                    }

                });
                allowing(ctx).flush();
                will(returnValue(null));
                allowing(ctx).close();
                will(returnValue(null));

                atLeast(1).of(messageHandler).handle(with(any(Channel.class)), with(anything()));
                will(returnValue(response));
                allowing(response).headers();
                will(returnValue(new DefaultHttpHeaders()));
            }
        });
        FullHttpRequest httpRequest = buildHttpRequest("anyPath");
        NettyHttpRequestHandler handler = new NettyHttpRequestHandler(null, messageHandler);
        handler.channelRead0(ctx, httpRequest);
    }

    @Test
    public void testServerStatus() throws Exception {
        final MessageHandler messageHandler = mockery.mock(MessageHandler.class);
        final ChannelHandlerContext ctx = mockery.mock(ChannelHandlerContext.class);
        mockery.checking(new Expectations() {
            {
                allowing(ctx).write(with(any(FullHttpResponse.class)));
                will(new CustomAction("verify") {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable {
                        verifyStatus((FullHttpResponse) invocation.getParameter(0));
                        return null;
                    }

                });
                allowing(ctx).flush();
                will(returnValue(null));
                allowing(ctx).close();
                will(returnValue(null));

                allowing(messageHandler).handle(with(any(Channel.class)), with(anything()));
                will(returnValue(null));
            }
        });

        FullHttpRequest httpRequest = buildHttpRequest(NettyHttpRequestHandler.ROOT_PATH);
        NettyHttpRequestHandler handler = new NettyHttpRequestHandler(null, messageHandler);

        // 关闭心跳开关
        MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, false);
        handler.channelRead0(ctx, httpRequest);

        // 打开心跳开关
        MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true);
        handler.channelRead0(ctx, httpRequest);

    }


    private void verifyStatus(FullHttpResponse response) {
        if (MotanSwitcherUtil.isOpen(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER)) {
            assertEquals(HttpResponseStatus.OK, response.getStatus());
        } else {
            assertEquals(HttpResponseStatus.SERVICE_UNAVAILABLE, response.getStatus());
        }
    }

    private FullHttpRequest buildHttpRequest(String requestPath) throws Exception {
        PooledByteBufAllocator allocator = new PooledByteBufAllocator();
        ByteBuf buf = allocator.buffer(0);
        FullHttpRequest httpReqeust = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, requestPath, buf);
        return httpReqeust;
    }

}
