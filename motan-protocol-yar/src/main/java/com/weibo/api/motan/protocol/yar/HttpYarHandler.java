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
package com.weibo.api.motan.protocol.yar;

import io.netty.buffer.ByteBuf;
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.yar.YarProtocol;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

@Sharable
public class HttpYarHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private ThreadPoolExecutor threadPoolExecutor;

    protected ConcurrentHashMap<String, Provider<?>> providerMap = new ConcurrentHashMap<String, Provider<?>>();


    public HttpYarHandler() {
        super();
    }

    public HttpYarHandler(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest) throws Exception {
        final String path = httpRequest.getUri();
        System.out.println("request uri:" + path);
        ByteBuf buf = httpRequest.content();
        final byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);
        
        if (threadPoolExecutor == null) {
            processHttpRequest(ctx, path, bytes);
        } else {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    processHttpRequest(ctx, path, bytes);
                }
            });
        }


    }

    // TODO 异常时返回默认异常
    private void processHttpRequest(ChannelHandlerContext ctx, String requestPath, byte[] requestContent) {
        YarResponse yarResponse = null;
        String packagerName = "JSON";
        try{
         // TODO badRequest
            YarRequest yarRequest = YarProtocol.buildRequest(requestContent);
            packagerName = yarRequest.getPackagerName();
            Provider provider = providerMap.get(requestPath);
            if (provider == null) {
                // TODO 返回默认异常 response
            }
            Class<?> clazz = provider.getInterface();
            Request request = YarProtocolUtil.convert(yarRequest, clazz);
            Response response = provider.call(request);
            yarResponse = YarProtocolUtil.convert(response, packagerName);
        }catch(Exception e){
            //TODO log
            e.printStackTrace();
            yarResponse = buildDefaultErrorResponse(e.getMessage(), packagerName);
        }
        
        //http response
        try {
            
            FullHttpResponse httpResponse =
                    new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(YarProtocol
                            .toProtocolBytes(yarResponse)));
            httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
            httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, httpResponse.content().readableBytes());
            //TODO need support?
//            if (HttpHeaders.isKeepAlive(httpRequest)) {
//                httpResponse.headers().set(HttpHeaders.Names.CONNECTION, Values.KEEP_ALIVE);
//            }
            ctx.write(httpResponse);
            ctx.flush();
        } catch (Exception e) {
            // TODO log
            e.printStackTrace();
        }
        
        
    }
    
    
    private YarResponse buildDefaultErrorResponse(String errMsg, String packagerName){
        YarResponse yarResponse = new YarResponse();
        yarResponse.setPackagerName(packagerName);
        yarResponse.setError(errMsg);
        yarResponse.setStatus("500"); //TODO 需要确定含义
        return yarResponse;
    }



    public void addProvider(Provider provider) {
        String path = YarProtocolUtil.getYarPath(provider.getUrl());
        Provider old = providerMap.putIfAbsent(path, provider);
        if (old != null) {
            throw new MotanFrameworkException("duplicate yar provider");
        }
    }



}
