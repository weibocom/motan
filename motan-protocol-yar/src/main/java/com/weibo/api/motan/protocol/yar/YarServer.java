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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;

public class YarServer {
    private final HttpYarHandler handler;
    private int port;
    private URL url;
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public YarServer(URL url) {
        this.url = url;
        handler = new HttpYarHandler(new ThreadPoolExecutor(200, 800, 15, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(500)));
//        handler = new HttpYarHandler();
        port = url.getPort();
    }

    public void addProvider(Provider provicer) {
        handler.addProvider(provicer);
    }

    public boolean open() throws InterruptedException {
        if (channel != null && channel.isOpen()) {
            return true;
        }
        if (channel != null) {
            channel.close();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
        }


        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                ch.pipeline().addLast("http-aggregator", new HttpObjectAggregator(65536));
                ch.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                ch.pipeline().addLast("serverHandler", handler);
            }
        }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, false);

        ChannelFuture f = b.bind(port).sync();
        channel = f.channel();
        return true;
    }

    public void close() {
        if (channel != null) {
            channel.close();
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            workerGroup = null;
            bossGroup = null;
        }
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    public boolean isAvailable() {
        // TODO active == availabe ?
        return channel.isActive();
    }

}
