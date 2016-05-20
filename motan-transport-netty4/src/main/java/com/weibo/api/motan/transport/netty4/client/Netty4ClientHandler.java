package com.weibo.api.motan.transport.netty4.client;

import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * Created by guohang.bao on 16/5/18.
 */
public class Netty4ClientHandler extends SimpleChannelInboundHandler<Response> {

    private Netty4Client client;

    public Netty4ClientHandler(Netty4Client client) {
        this.client = client;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Response response) throws Exception {

        NettyResponseFuture responseFuture = client.removeCallback(response.getRequestId());

        if (responseFuture == null) {
            LoggerUtil.warn(
                    "NettyClient has response from server, but resonseFuture not exist,  requestId={}",
                    response.getRequestId());
            return;
        }

        if (response.getException() != null) {
            responseFuture.onFailure(response);
        } else {
            responseFuture.onSuccess(response);
        }
    }
}
