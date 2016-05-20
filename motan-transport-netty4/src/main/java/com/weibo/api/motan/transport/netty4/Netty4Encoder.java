package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by guohang.bao on 16/5/17.
 */
public class Netty4Encoder extends MessageToByteEncoder<Object> {

    private Codec codec;
    private com.weibo.api.motan.transport.Channel client;

    public Netty4Encoder(Codec codec, com.weibo.api.motan.transport.Channel client) {
        this.codec = codec;
        this.client = client;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object message, ByteBuf out) throws Exception {

        long requestId = getRequestId(message);
        byte[] data = null;

        if (message instanceof Response) {
            try {
                data = codec.encode(client, message);
            } catch (Exception e) {
                LoggerUtil.error("NettyEncoder encode error, identity=" + client.getUrl().getIdentity(), e);
                Response response = buildExceptionResponse(requestId, e);
                data = codec.encode(client, response);
            }
        } else {
            data = codec.encode(client, message);
        }

        byte[] transportHeader = new byte[MotanConstants.NETTY_HEADER];
        ByteUtil.short2bytes(MotanConstants.NETTY_MAGIC_TYPE, transportHeader, 0);
        transportHeader[3] = getType(message);
        ByteUtil.long2bytes(getRequestId(message), transportHeader, 4);
        ByteUtil.int2bytes(data.length, transportHeader, 12);

        out.writeBytes(transportHeader);
        out.writeBytes(data);
    }


    private long getRequestId(Object message) {
        if (message instanceof Request) {
            return ((Request) message).getRequestId();
        } else if (message instanceof Response) {
            return ((Response) message).getRequestId();
        } else {
            return 0;
        }
    }

    private byte getType(Object message) {
        if (message instanceof Request) {
            return MotanConstants.FLAG_REQUEST;
        } else if (message instanceof Response) {
            return MotanConstants.FLAG_RESPONSE;
        } else {
            return MotanConstants.FLAG_OTHER;
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }
}
