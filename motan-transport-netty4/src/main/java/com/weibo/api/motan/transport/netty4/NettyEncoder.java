package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.protocol.v2motan.MotanV2Codec;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

/**
 * @author sunnights
 */
public class NettyEncoder extends MessageToByteEncoder<Object> {
    private Codec codec;
    private Channel motanChannel;

    public NettyEncoder(Codec codec, Channel motanChannel) {
        this.codec = codec;
        this.motanChannel = motanChannel;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        if (codec instanceof MotanV2Codec) {
            encodeV2(ctx, msg, out);
        } else {
            encodeV1(ctx, msg, out);
        }
    }

    private void encodeV2(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws IOException {
        out.writeBytes(encodeMessage(msg));
    }

    private void encodeV1(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws IOException {
        long requestId = getRequestId(msg);
        byte[] data = encodeMessage(msg);
        byte[] transportHeader = new byte[MotanConstants.NETTY_HEADER];
        ByteUtil.short2bytes(MotanConstants.NETTY_MAGIC_TYPE, transportHeader, 0);
        transportHeader[3] = getType(msg);
        ByteUtil.long2bytes(requestId, transportHeader, 4);
        ByteUtil.int2bytes(data.length, transportHeader, 12);
        out.writeBytes(transportHeader);
        out.writeBytes(data);
    }

    private byte[] encodeMessage(Object msg) throws IOException {
        byte[] data = null;
        if (msg instanceof Response) {
            try {
                data = codec.encode(motanChannel, msg);
            } catch (Exception e) {
                LoggerUtil.error("NettyEncoder encode error, identity=" + motanChannel.getUrl().getIdentity(), e);
                long requestId = getRequestId(msg);
                Response response = buildExceptionResponse(requestId, e);
                data = codec.encode(motanChannel, response);
            }
        } else {
            data = codec.encode(motanChannel, msg);
        }
        return data;
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

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
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
}
