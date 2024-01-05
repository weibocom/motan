/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.transport.netty.admin;

import com.weibo.api.motan.admin.AbstractAdminServer;
import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.admin.AdminUtil;
import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty.StandardThreadExecutor;
import com.weibo.api.motan.util.LoggerUtil;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class AdminHttpServer extends AbstractAdminServer {
    private final static ChannelFactory channelFactory = new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(new DefaultThreadFactory("AdminHttpServerBoss", true)),
            Executors.newCachedThreadPool(new DefaultThreadFactory("AdminHttpServerWorker", true)));
    private StandardThreadExecutor standardThreadExecutor = null;
    private Channel serverChannel;
    private volatile ChannelState state = ChannelState.UNINIT;

    public AdminHttpServer(URL url, AdminHandler adminHandler) {
        this.url = url;
        this.adminHandler = adminHandler;
    }

    @Override
    public boolean open() {
        if (state.isAliveState()) {
            return true;
        }
        final int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(),
                URLParamType.maxContentLength.getIntValue());
        standardThreadExecutor = (standardThreadExecutor != null && !standardThreadExecutor.isShutdown()) ? standardThreadExecutor
                : new StandardThreadExecutor(5, 50, 500,
                new DefaultThreadFactory("AdminHttpServer-" + url.getServerPortStr(), true));
        standardThreadExecutor.prestartAllCoreThreads();
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(() -> {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, maxContentLength));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("nettyChannelHandler", new SimpleChannelUpstreamHandler() {
                @Override
                public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) {
                    try {
                        standardThreadExecutor.execute(() -> processHttpRequest(event));
                    } catch (Exception e) {
                        LoggerUtil.error("AdminHttpServer request is rejected by threadPool!", e);
                        sendResponse(event.getChannel(), buildErrorResponse(AdminUtil.toJsonErrorMessage("request is rejected by thread pool")));
                    }
                }
            });
            return pipeline;
        });
        try {
            serverChannel = bootstrap.bind(new InetSocketAddress(url.getPort()));
            if (url.getPort() == 0) {
                url.setPort(((InetSocketAddress) serverChannel.getLocalAddress()).getPort());
            }
            state = ChannelState.ALIVE;
            LoggerUtil.info("AdminHttpServer Open: url=" + url);
            return true;
        } catch (Exception e) {
            LoggerUtil.error("AdminHttpServer Open fail: url=" + url, e);
            return false;
        }
    }

    private void processHttpRequest(MessageEvent event) {
        HttpResponse httpResponse;
        try {
            HttpRequest httpRequest = (HttpRequest) event.getMessage();
            // set remote ip
            httpRequest.setHeader(URLParamType.host.getName(), ((InetSocketAddress) event.getChannel().getRemoteAddress()).getAddress().getHostAddress());
            httpResponse = convertHttpResponse(adminHandler.handle(convertRequest(httpRequest)));
        } catch (Exception e) {
            LoggerUtil.error("AdminHttpServer convert request fail.", e);
            httpResponse = buildErrorResponse(AdminUtil.toJsonErrorMessage(e.getMessage()));
        }
        sendResponse(event.getChannel(), httpResponse);
    }

    Request convertRequest(HttpRequest httpRequest) {
        DefaultRequest request = new DefaultRequest();
        Map<String, String> params = new ConcurrentHashMap<>();

        // decode path and query params
        QueryStringDecoder decoder = new QueryStringDecoder(httpRequest.getUri());
        for (Map.Entry<String, List<String>> entry : decoder.getParameters().entrySet()) {
            params.put(entry.getKey(), entry.getValue().get(0));
        }
        request.setMethodName(decoder.getPath());

        // decode post form params
        decoder = new QueryStringDecoder("?" + httpRequest.getContent().toString(StandardCharsets.UTF_8));
        for (Map.Entry<String, List<String>> entry : decoder.getParameters().entrySet()) {
            params.put(entry.getKey(), entry.getValue().get(0));
        }
        request.setArguments(new Object[]{params});

        // add headers to attachments
        for (Map.Entry<String, String> entry : httpRequest.getHeaders()) {
            request.setAttachment(entry.getKey(), entry.getValue());
        }
        return request;
    }

    HttpResponse convertHttpResponse(Response response) {
        if (response.getException() != null) {
            String errMsg;
            if (response.getException() instanceof MotanAbstractException) {
                errMsg = ((MotanAbstractException) response.getException()).getOriginMessage();
            } else {
                errMsg = response.getException().getMessage();
            }
            return buildErrorResponse(errMsg);
        }
        return buildOkResponse(response.getValue().toString());
    }

    private HttpResponse buildErrorResponse(String errorMessage) {
        return buildHttpResponse(errorMessage.getBytes(StandardCharsets.UTF_8), SERVICE_UNAVAILABLE);
    }

    private HttpResponse buildOkResponse(String content) {
        return buildHttpResponse(content.getBytes(StandardCharsets.UTF_8), OK);
    }

    private HttpResponse buildHttpResponse(byte[] content, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        ChannelBuffer responseBuffer = new DynamicChannelBuffer(content.length);
        responseBuffer.writeBytes(content);
        response.setContent(responseBuffer);
        response.setHeader("Content-Type", "text/html; charset=UTF-8");
        response.setHeader("Content-Length", responseBuffer.writerIndex());
        return response;
    }

    private void sendResponse(Channel ch, HttpResponse response) {
        try {
            ChannelFuture f = ch.write(response);
            f.addListener(future -> future.getChannel().close());
        } catch (Exception e) {
            LoggerUtil.error("AdminHttpServer send response fail.", e);
            ch.close();
        }
    }

    @Override
    public void close() {
        if (state.isCloseState()) {
            return;
        }
        if (serverChannel != null) {
            serverChannel.close();
            serverChannel = null;
        }
        if (standardThreadExecutor != null) {
            standardThreadExecutor.shutdownNow();
            standardThreadExecutor = null;
        }
        state = ChannelState.CLOSE;
        LoggerUtil.info("AdminHttpServer close Success: url={}", url.getUri());
    }
}
