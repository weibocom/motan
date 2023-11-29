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

package com.weibo.api.motan.transport.netty4;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestEncoder;
import io.netty.util.CharsetUtil;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanglei28
 * @date 2023/11/28.
 * simple http client for unit test
 */
public class TestHttpClient {
    private String host;
    private int port;
    private int defaultTimeout;
    private EventLoopGroup workerGroup;
    private Bootstrap b;
    private Map<String, CompletableFuture<String>> results = new ConcurrentHashMap<>();

    public TestHttpClient(String host, int port, int defaultTimeout) {
        this.host = host;
        this.port = port;
        this.defaultTimeout = defaultTimeout;
        init();
    }

    private void init() {
        b = new Bootstrap();
        workerGroup = new NioEventLoopGroup();
        b.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("http-decoder", new HttpResponseDecoder());
                ch.pipeline().addLast("http-encoder", new HttpRequestEncoder());
                ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(1024 * 1024));
                ch.pipeline().addLast("clientHandler", new SimpleChannelInboundHandler<FullHttpResponse>() {
                    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpResponse httpResponse) {
                        CompletableFuture<String> future = results.get(ctx.channel().id().asLongText());
                        if (future != null) {
                            future.complete(httpResponse.content().toString(CharsetUtil.UTF_8));
                        }
                    }
                });
            }
        });
    }

    public void close() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        results.clear();
    }

    public String get(String uri) throws Exception {
        DefaultHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, new URI(uri).toASCIIString());
        return send(request);
    }

    public String post(String uri, Map<String, String> params, Map<String, String> headers) throws Exception {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, new URI(uri).toASCIIString());
        if (params != null && !params.isEmpty()) {
            HttpPostRequestEncoder encoder = new HttpPostRequestEncoder(new DefaultHttpDataFactory(), request, false);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                encoder.addBodyAttribute(entry.getKey(), entry.getValue());
            }
            request = encoder.finalizeRequest();
        }
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.headers().set(entry.getKey(), entry.getValue());
            }
        }
        return send(request);
    }

    private String send(HttpRequest request) throws Exception {
        ChannelFuture f = b.connect(host, port).sync();
        CompletableFuture<String> future = new CompletableFuture<>();
        results.put(f.channel().id().asLongText(), future);
        String result;
        try {
            f.channel().writeAndFlush(request).sync();
            result = future.get(defaultTimeout, TimeUnit.MILLISECONDS);
        } finally {
            results.remove(f.channel().id().asLongText());
            f.channel().closeFuture().sync();
        }
        return result;
    }
}
