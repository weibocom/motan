package com.weibo.api.motan.transport.netty4.client;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.Netty4Decoder;
import com.weibo.api.motan.transport.netty4.Netty4Encoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * Created by guohang.bao on 16/5/18.
 */
public class Netty4ClientInitializer extends ChannelInitializer<SocketChannel> {

    private URL url;

    private Codec codec;

    private Netty4Client client;

    public Netty4ClientInitializer(URL url, Codec codec, Netty4Client client) {
        this.url = url;
        this.codec = codec;
        this.client = client;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        // 最大响应包限制
        int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(),
                URLParamType.maxContentLength.getIntValue());
        p.addLast("decoder", new Netty4Decoder(codec, client, maxContentLength));
        p.addLast("encoder", new Netty4Encoder(codec, client));
        p.addLast("handler", new Netty4ClientHandler(client));

    }
}
