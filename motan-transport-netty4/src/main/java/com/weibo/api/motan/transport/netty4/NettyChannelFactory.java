package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.transport.SharedObjectFactory;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sunnights
 */
public class NettyChannelFactory implements SharedObjectFactory<NettyChannel> {
    private NettyClient nettyClient;
    private String factoryName;

    public NettyChannelFactory(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.factoryName = "NettyChannelFactory_" + nettyClient.getUrl().getHost() + "_" + nettyClient.getUrl().getPort();
    }

    @Override
    public NettyChannel makeObject() throws Exception {
        NettyChannel nettyChannel = new NettyChannel(nettyClient);
        nettyChannel.open();
        return nettyChannel;
    }

    @Override
    public boolean rebuildObject(NettyChannel nettyChannel) throws Exception {
        ReentrantLock lock = nettyChannel.getLock();
        if (lock.tryLock()) {
            try {
                if (!nettyChannel.isAvailable()) {
                    nettyChannel.close();
                    nettyChannel.open();
                    LoggerUtil.info("rebuild channel success: " + nettyChannel.getUrl());
                }
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
}
