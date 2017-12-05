package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.LoggerUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author sunnights
 */
public class NettyDecoder extends ByteToMessageDecoder {
    private Codec codec;
    private Channel channel;
    private int maxContentLength = 0;

    public NettyDecoder(Codec codec, Channel channel, int maxContentLength) {
        this.codec = codec;
        this.channel = channel;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() <= MotanConstants.NETTY_HEADER) {
            return;
        }

        in.markReaderIndex();
        short type = in.readShort();
        if (type != MotanConstants.NETTY_MAGIC_TYPE) {
            in.resetReaderIndex();
            throw new MotanFrameworkException("NettyDecoder transport header not support, type: " + type);
        }
        in.skipBytes(1);
        int rpcVersion = (in.readByte() & 0xff) >>> 3;
        switch (rpcVersion) {
            case 0:
                decodeV1(ctx, in, out);
                break;
            case 1:
                decodeV2(ctx, in, out);
                break;
            default:
                decodeV2(ctx, in, out);
        }
    }

    private void decodeV2(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.resetReaderIndex();
        if (in.readableBytes() < 21) {
            return;
        }
        in.skipBytes(2);
        boolean isRequest = isV2Request(in.readByte());
        in.skipBytes(2);
        long requestId = in.readLong();
        int size = 13;
        int metaSize = in.readInt();
        size += 4;
        if (metaSize > 0) {
            size += metaSize;
            if (in.readableBytes() < metaSize) {
                in.resetReaderIndex();
                return;
            }
            in.skipBytes(metaSize);
        }
        if (in.readableBytes() < 4) {
            in.resetReaderIndex();
            return;
        }
        int bodySize = in.readInt();
        checkMaxContext(bodySize, ctx, isRequest, requestId);
        size += 4;
        if (bodySize > 0) {
            size += bodySize;
            if (in.readableBytes() < bodySize) {
                in.resetReaderIndex();
                return;
            }
        }
        byte[] data = new byte[size];
        in.resetReaderIndex();
        in.readBytes(data);
        decode(data, out, isRequest, requestId);
    }

    private boolean isV2Request(byte b) {
        return (b & 0x01) == 0x00;
    }

    private void decodeV1(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        in.resetReaderIndex();
        in.skipBytes(2);// skip magic num
        byte messageType = (byte) in.readShort();
        long requestId = in.readLong();
        int dataLength = in.readInt();

        // FIXME 如果dataLength过大，可能导致问题
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }
        checkMaxContext(dataLength, ctx, messageType == MotanConstants.FLAG_REQUEST, requestId);
        byte[] data = new byte[dataLength];
        in.readBytes(data);
        decode(data, out, messageType == MotanConstants.FLAG_REQUEST, requestId);
    }

    private void checkMaxContext(int dataLength, ChannelHandlerContext ctx, boolean isRequest, long requestId) throws Exception {
        if (maxContentLength > 0 && dataLength > maxContentLength) {
            LoggerUtil.warn("NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
                    dataLength, maxContentLength, ctx.channel().remoteAddress(), ctx.channel().localAddress());
            Exception e = new MotanServiceException("NettyDecoder transport data content length over of limit, size: " + dataLength + " > " + maxContentLength);
            if (isRequest) {
                Response response = buildExceptionResponse(requestId, e);
                byte[] msg = CodecUtil.encodeObjectToBytes(channel, codec, response);
                ctx.channel().writeAndFlush(msg);
                throw e;
            } else {
                throw e;
            }
        }
    }

    private void decode(byte[] data, List<Object> out, boolean isRequest, long requestId) {
        out.add(new NettyMessage(isRequest, requestId, data));
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }

}
