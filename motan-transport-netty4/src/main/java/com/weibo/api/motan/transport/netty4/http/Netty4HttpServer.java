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

import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.AbstractServer;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.StatisticCallback;
import com.weibo.api.motan.util.StatsUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.net.InetSocketAddress;

/**
 * @author zhanglei
 * netty4 http server.
 * @date 2016-5-31
 */
public class Netty4HttpServer extends AbstractServer implements StatisticCallback {
    private HttpMessageHandler httpMessageHandler;
    private URL url;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private StandardThreadExecutor standardThreadExecutor;

    public Netty4HttpServer(URL url, HttpMessageHandler httpMessageHandler) {
        this.url = url;
        this.httpMessageHandler = httpMessageHandler;
    }

    @Override
    public synchronized boolean open() {
        if (isAvailable()) {
            return true;
        }
        if (channel != null) {
            channel.close();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
        }
        boolean shareChannel = url.getBooleanParameter(URLParamType.shareChannel.getName(), URLParamType.shareChannel.getBooleanValue());
        int workerQueueSize = url.getIntParameter(URLParamType.workerQueueSize.getName(), 500);

        int minWorkerThread, maxWorkerThread;

        if (shareChannel) {
            minWorkerThread = url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread = url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MAX_WORKDER);
        } else {
            minWorkerThread =
                    url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread =
                    url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MAX_WORKDER);
        }
        final int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(), URLParamType.maxContentLength.getIntValue());
        standardThreadExecutor = (standardThreadExecutor != null && !standardThreadExecutor.isShutdown()) ? standardThreadExecutor
                : new StandardThreadExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new DefaultThreadFactory("NettyServer-" + url.getServerPortStr(), true));
        standardThreadExecutor.prestartAllCoreThreads();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(maxContentLength));
                ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                ch.pipeline().addLast("serverHandler", new SimpleChannelInboundHandler<FullHttpRequest>() {
                    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest) {
                        httpRequest.content().retain();
                        try {
                            standardThreadExecutor.execute(() -> processHttpRequest(ctx, httpRequest));
                        } catch (Exception e) {
                            LoggerUtil.error("request is rejected by threadPool!", e);
                            httpRequest.content().release();
                            sendResponse(ctx, NettyHttpUtil.buildErrorResponse("request is rejected by thread pool!"));
                        }
                    }
                });
            }
        }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, false);

        ChannelFuture f;
        try {
            f = b.bind(url.getPort()).sync();
            channel = f.channel();
        } catch (InterruptedException e) {
            LoggerUtil.error("init http server fail.", e);
            return false;
        }
        setLocalAddress((InetSocketAddress) channel.localAddress());
        if (url.getPort() == 0) {
            url.setPort(getLocalAddress().getPort());
        }
        state = ChannelState.ALIVE;
        StatsUtil.registryStatisticCallback(this);
        LoggerUtil.info("Netty4HttpServer ServerChannel finish Open: url=" + url);
        return true;
    }


    private void processHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse;
        try {
            httpRequest.headers().set(URLParamType.host.getName(), ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
            httpResponse = httpMessageHandler.handle(this, httpRequest);
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler process http request fail.", e);
            httpResponse = NettyHttpUtil.buildErrorResponse(e.getMessage());
        } finally {
            httpRequest.content().release();
        }
        sendResponse(ctx, httpResponse);
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
        boolean close = false;
        try {
            ctx.write(httpResponse);
            ctx.flush();
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler write response fail.", e);
            close = true;
        } finally {
            // close connection
            if (close || httpResponse == null || !httpResponse.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true)) {
                ctx.close();
            }
        }
    }

    @Override
    public void close() {
        close(0);
    }

    @Override
    public boolean isAvailable() {
        return state.isAliveState();
    }

    @Override
    public boolean isBound() {
        return channel != null && channel.isActive();
    }

    @Override
    public Response request(Request request) throws TransportException {
        throw new MotanFrameworkException("Netty4HttpServer request(Request request) method unSupport: url: " + url);
    }

    @Override
    public synchronized void close(int timeout) {
        if (state.isCloseState()) {
            LoggerUtil.info("Netty4HttpServer close fail: already close, url={}", url.getUri());
            return;
        }

        if (state.isUnInitState()) {
            LoggerUtil.info("Netty4HttpServer close Fail: don't need to close because node is unInit state: url={}",
                    url.getUri());
            return;
        }
        if (channel != null) {
            channel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (standardThreadExecutor != null) {
            standardThreadExecutor.shutdownNow();
        }
        workerGroup = null;
        bossGroup = null;
        standardThreadExecutor = null;
        channel = null;
        state = ChannelState.CLOSE;
        StatsUtil.unRegistryStatisticCallback(this);
    }

    @Override
    public boolean isClosed() {
        return state.isCloseState();
    }

    @Override
    public String statisticCallback() {
        return null;
    }

    @Override
    public URL getUrl() {
        return url;
    }

}
