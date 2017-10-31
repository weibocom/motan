package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.pool.BasePoolableObjectFactory;

/**
 * @author sunnights
 */
public class NettyChannelFactory extends BasePoolableObjectFactory {
    private NettyClient nettyClient;
    private String factoryName;

    public NettyChannelFactory(NettyClient nettyClient) {
        super();
        this.nettyClient = nettyClient;
        this.factoryName = "NettyChannelFactory_" + nettyClient.getUrl().getHost() + "_" + nettyClient.getUrl().getPort();
    }

    @Override
    public Object makeObject() throws Exception {
        NettyChannel nettyChannel = new NettyChannel(nettyClient);
        nettyChannel.open();
        return nettyChannel;
    }

    @Override
    public void destroyObject(final Object obj) throws Exception {
        if (obj instanceof NettyChannel) {
            NettyChannel client = (NettyChannel) obj;
            URL url = nettyClient.getUrl();

            try {
                client.close();
                LoggerUtil.info(factoryName + " client disconnect Success: " + url.getUri());
            } catch (Exception e) {
                LoggerUtil.error(factoryName + " client disconnect Error: " + url.getUri(), e);
            }
        }
    }

    @Override
    public boolean validateObject(final Object obj) {
        if (obj instanceof NettyChannel) {
            final NettyChannel client = (NettyChannel) obj;
            try {
                return client.isAvailable();
            } catch (final Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public void activateObject(Object obj) throws Exception {
        if (obj instanceof NettyChannel) {
            final NettyChannel client = (NettyChannel) obj;
            if (!client.isAvailable()) {
                client.open();
            }
        }
    }

    @Override
    public String toString() {
        return factoryName;
    }
}
