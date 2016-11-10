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
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.util.concurrent.ThreadPoolExecutor;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * 
 * @Description http request handler for netty4
 * @author zhanglei
 * @date 2016-5-31
 *
 */

@Sharable
public class NettyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final String BAD_REQUEST = "/bad-request";
    public static final String ROOT_PATH = "/";
    public static final String STATUS_PATH = "/rpcstatus";
    private Channel serverChannel;
    private ThreadPoolExecutor threadPoolExecutor;
    private MessageHandler messageHandler;
    protected String swictherName = MotanConstants.REGISTRY_HEARTBEAT_SWITCHER;
    
    

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
        // check badRequest
        if(BAD_REQUEST.equals(httpRequest.getUri())){
            sendResponse(ctx, buildDefaultResponse("bad request!", HttpResponseStatus.BAD_REQUEST));
            return;
        }
        
        // service status 
        if(ROOT_PATH.equals(httpRequest.getUri()) || STATUS_PATH.equals(httpRequest.getUri())){
            if(isSwitchOpen()){// 200
                sendResponse(ctx, buildDefaultResponse("ok!", HttpResponseStatus.OK));
            }else{//503
                sendResponse(ctx, buildErrorResponse("service not available!"));
            }
            return;
        }

        httpRequest.content().retain();

        if (threadPoolExecutor == null) {
            processHttpRequest(ctx, httpRequest);
        } else {
            try{
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        processHttpRequest(ctx, httpRequest);
                    }
                });
            }catch(Exception e){
                LoggerUtil.error("request is rejected by threadpool!", e);
                httpRequest.content().release();
                sendResponse(ctx, buildErrorResponse("request is rejected by threadpool!"));
            }
        }
    }


    protected void processHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = null;
        try {
            httpResponse = (FullHttpResponse) messageHandler.handle(serverChannel, httpRequest);
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler process http request fail.", e);
            httpResponse = buildErrorResponse(e.getMessage());
        } finally {
            httpRequest.content().release();
        }
        sendResponse(ctx, httpResponse);
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse){
        boolean close = false;
        try {
            ctx.write(httpResponse);
            ctx.flush();
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler write response fail.", e);
            close = true;
        } finally {
            // close connection
            if (close || httpResponse == null || !Values.KEEP_ALIVE.equals(httpResponse.headers().get(HttpHeaders.Names.CONNECTION))) {
                ctx.close();
            }
        }
    }
    
    protected FullHttpResponse buildErrorResponse(String errMsg) {
        return buildDefaultResponse(errMsg, HttpResponseStatus.SERVICE_UNAVAILABLE);
    }
    
    protected FullHttpResponse buildDefaultResponse(String msg, HttpResponseStatus status){
        FullHttpResponse errorResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(msg
                        .getBytes()));
        return errorResponse;
    }
    
    /**
     * is service switcher close. http status will be 503 when switcher is close
     * @return
     */
    protected boolean isSwitchOpen(){
        return MotanSwitcherUtil.isOpen(swictherName);
    }
    
    
    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }


}
