package com.weibo.api.motan.transport.netty4;

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
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.StatisticCallback;
import com.weibo.api.motan.util.StatsUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author sunnights
 */
public class NettyServer extends AbstractServer implements StatisticCallback {
    protected NettyServerChannelManage channelManage = null;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private MessageHandler messageHandler;
    private StandardThreadExecutor standardThreadExecutor = null;

    public NettyServer(URL url, MessageHandler messageHandler) {
        super(url);
        this.messageHandler = messageHandler;
    }

    @Override
    public boolean isBound() {
        return serverChannel != null && serverChannel.isActive();
    }

    @Override
    public Response request(Request request) throws TransportException {
        throw new MotanFrameworkException("NettyServer request(Request request) method not support: url: " + url);
    }

    @Override
    public boolean open() {
        if (isAvailable()) {
            LoggerUtil.warn("NettyServer ServerChannel already Open: url=" + url);
            return state.isAliveState();
        }
        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        LoggerUtil.info("NettyServer ServerChannel start Open: url=" + url);
        boolean shareChannel = url.getBooleanParameter(URLParamType.shareChannel.getName(), URLParamType.shareChannel.getBooleanValue());
        final int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(), URLParamType.maxContentLength.getIntValue());
        int maxServerConnection = url.getIntParameter(URLParamType.maxServerConnection.getName(), URLParamType.maxServerConnection.getIntValue());
        int workerQueueSize = url.getIntParameter(URLParamType.workerQueueSize.getName(), URLParamType.workerQueueSize.getIntValue());

        int minWorkerThread, maxWorkerThread;

        if (shareChannel) {
            minWorkerThread = url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread = url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MAX_WORKDER);
        } else {
            minWorkerThread = url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MIN_WORKDER);
            maxWorkerThread = url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MAX_WORKDER);
        }

        standardThreadExecutor = (standardThreadExecutor != null && !standardThreadExecutor.isShutdown()) ? standardThreadExecutor
                : new StandardThreadExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new DefaultThreadFactory("NettyServer-" + url.getServerPortStr(), true));
        standardThreadExecutor.prestartAllCoreThreads();

        channelManage = new NettyServerChannelManage(maxServerConnection);

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("channel_manage", channelManage);
                        pipeline.addLast("decoder", new NettyDecoder(codec, NettyServer.this, maxContentLength));
                        pipeline.addLast("encoder", new NettyEncoder());
                        pipeline.addLast("handler", new NettyChannelHandler(NettyServer.this, messageHandler, standardThreadExecutor));
                    }
                });
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture channelFuture = serverBootstrap.bind(new InetSocketAddress(url.getPort()));
        channelFuture.syncUninterruptibly();
        serverChannel = channelFuture.channel();
        state = ChannelState.ALIVE;
        StatsUtil.registryStatisticCallback(this);
        LoggerUtil.info("NettyServer ServerChannel finish Open: url=" + url);
        return state.isAliveState();
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        if (state.isCloseState()) {
            LoggerUtil.info("NettyServer close fail: already close, url={}", url.getUri());
            return;
        }

        if (state.isUnInitState()) {
            LoggerUtil.info("NettyServer close Fail: don't need to close because node is unInit state: url={}", url.getUri());
            return;
        }

        try {
            // close listen socket
            if (serverChannel != null) {
                serverChannel.close();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                bossGroup = null;
                workerGroup = null;
            }
            // close all clients's channel
            if (channelManage != null) {
                channelManage.close();
            }
            // shutdown the threadPool
            if (standardThreadExecutor != null) {
                standardThreadExecutor.shutdownNow();
            }
            // 设置close状态
            state = ChannelState.CLOSE;
            // 取消统计回调的注册
            StatsUtil.unRegistryStatisticCallback(this);
            LoggerUtil.info("NettyServer close Success: url={}", url.getUri());
        } catch (Exception e) {
            LoggerUtil.error("NettyServer close Error: url=" + url.getUri(), e);
        }
    }

    @Override
    public boolean isClosed() {
        return state.isCloseState();
    }

    @Override
    public boolean isAvailable() {
        return state.isAliveState();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public String statisticCallback() {
        return String.format("identity: %s connectionCount: %s taskCount: %s queueCount: %s maxThreadCount: %s maxTaskCount: %s",
                url.getIdentity(), channelManage.getChannels().size(), standardThreadExecutor.getSubmittedTasksCount(),
                standardThreadExecutor.getQueue().size(), standardThreadExecutor.getMaximumPoolSize(),
                standardThreadExecutor.getMaxSubmittedTaskCount());
    }
}
