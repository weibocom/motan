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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.ThreadPoolExecutor;

import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * 
 * @Description http request handler for netty4
 * @author zhanglei
 * @date 2016年5月31日
 *
 */

@Sharable
public class NettyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private Channel serverChannel;
    private ThreadPoolExecutor threadPoolExecutor;
    private MessageHandler messageHandler;

    public NettyHttpRequestHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    public NettyHttpRequestHandler(Channel serverChannel, MessageHandler messageHandler) {
        this.serverChannel = serverChannel;
        this.messageHandler = messageHandler;
    }

    public NettyHttpRequestHandler(Channel serverChannel, MessageHandler messageHandler, ThreadPoolExecutor threadPoolExecutor) {
        this.serverChannel = serverChannel;
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = threadPoolExecutor;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest) throws Exception {
        // TODO check badRequest

        // TODO 需要测试跨线程有没有内存无法释放的问题，如果有就使用map传递到messageHandler
        httpRequest.content().retain();

        if (threadPoolExecutor == null) {
            processHttpRequest(ctx, httpRequest);
        } else {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    processHttpRequest(ctx, httpRequest);
                }
            });
        }
    }


    protected void processHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = null;
        try {
            httpResponse = (FullHttpResponse) messageHandler.handle(serverChannel, httpRequest);
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler process http request fail.", e);
            httpResponse = getDefaultErrorResponse(e.getMessage());
        } finally {
            httpRequest.content().release();
        }
        try {
            ctx.write(httpResponse);
            ctx.flush();
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler write response fail.", e);
        }
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    protected FullHttpResponse getDefaultErrorResponse(String errMsg) {
        FullHttpResponse errorResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SERVICE_UNAVAILABLE, Unpooled.wrappedBuffer(errMsg
                        .getBytes()));
        return errorResponse;
    }

}
