package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.transport.SharedObjectFactory;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sunnights
 */
public class NettyChannelFactory implements SharedObjectFactory<NettyChannel> {
    private static final ExecutorService rebuildExecutorService = new StandardThreadExecutor(5, 30, 10L, TimeUnit.SECONDS, 100,
            new DefaultThreadFactory("RebuildExecutorService", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    private NettyClient nettyClient;
    private String factoryName;

    public NettyChannelFactory(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.factoryName = "NettyChannelFactory_" + nettyClient.getUrl().getHost() + "_" + nettyClient.getUrl().getPort();
    }

    @Override
    public NettyChannel makeObject() {
        return new NettyChannel(nettyClient);
    }

    @Override
    public boolean rebuildObject(NettyChannel nettyChannel, boolean async) {
        ReentrantLock lock = nettyChannel.getLock();
        if (lock.tryLock()) {
            try {
                if (!nettyChannel.isAvailable() && !nettyChannel.isReconnect()) {
                    nettyChannel.reconnect();
                    if (async) {
                        rebuildExecutorService.submit(new RebuildTask(nettyChannel));
                    } else {
                        nettyChannel.close();
                        nettyChannel.open();
                        LoggerUtil.info("rebuild channel success: " + nettyChannel.getUrl());
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("rebuild error: " + this.toString() + ", " + nettyChannel.getUrl(), e);
            } finally {
                lock.unlock();
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return factoryName;
    }

    class RebuildTask implements Runnable {
        private NettyChannel channel;

        public RebuildTask(NettyChannel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                channel.getLock().lock();
                channel.close();
                channel.open();
                LoggerUtil.info("rebuild channel success: " + channel.getUrl());
            } catch (Exception e) {
                LoggerUtil.error("rebuild error: " + this.toString() + ", " + channel.getUrl(), e);
            } finally {
                channel.getLock().unlock();
            }
        }
    }
}
