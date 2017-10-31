package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.Server;
import com.weibo.api.motan.transport.support.AbstractEndpointFactory;

/**
 * @author sunnights
 */
@SpiMeta(name = "netty4")
public class NettyEndpointFactory extends AbstractEndpointFactory {
    @Override
    protected Server innerCreateServer(URL url, MessageHandler messageHandler) {
        return new NettyServer(url, messageHandler);
    }

    @Override
    protected Client innerCreateClient(URL url) {
        return new NettyClient(url);
    }
}
