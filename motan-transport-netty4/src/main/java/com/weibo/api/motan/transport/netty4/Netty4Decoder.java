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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * Created by guohang.bao on 16/5/17.
 */
public class Netty4Decoder extends ByteToMessageDecoder {

    private Codec codec;
    private com.weibo.api.motan.transport.Channel client;
    private int maxContentLength = 0;

    public Netty4Decoder(Codec codec, Channel client, int maxContentLength) {
        this.codec = codec;
        this.client = client;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        io.netty.channel.Channel channel = ctx.channel();

        if (byteBuf.readableBytes() <= MotanConstants.NETTY_HEADER) {
            return;
        }

        byteBuf.markReaderIndex();

        short type = byteBuf.readShort();

        if (type != MotanConstants.NETTY_MAGIC_TYPE) {
            byteBuf.resetReaderIndex();
            throw new MotanFrameworkException("NettyDecoder transport header not support, type: " + type);
        }

        byte messageType = (byte) byteBuf.readShort();
        long requestId = byteBuf.readLong();

        int dataLength = byteBuf.readInt();

        // FIXME 如果dataLength过大，可能导致问题
        if (byteBuf.readableBytes() < dataLength) {
            byteBuf.resetReaderIndex();
            return;
        }

        if (maxContentLength > 0 && dataLength > maxContentLength) {
            LoggerUtil.warn(
                    "NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
                    dataLength, maxContentLength, channel.remoteAddress(), channel.localAddress());
            Exception e = new MotanServiceException("NettyDecoder transport data content length over of limit, size: "
                    + dataLength + " > " + maxContentLength);

            if (messageType == MotanConstants.FLAG_REQUEST) {
                Response response = buildExceptionResponse(requestId, e);
                channel.write(response);
                throw e;
            } else {
                throw e;
            }
        }

        // TODO use byte array pool
        byte[] data = new byte[dataLength];

        byteBuf.readBytes(data);

        try {
            String remoteIp = getRemoteIp(channel);
            out.add(codec.decode(client, remoteIp, data));
        } catch (Exception e) {
            if (messageType == MotanConstants.FLAG_REQUEST) {
                Response response = buildExceptionResponse(requestId, e);
                channel.write(response);
                out.add(response);
            } else {
                out.add(buildExceptionResponse(requestId, e));
            }
        }

    }


    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }


    private String getRemoteIp(io.netty.channel.Channel channel) {
        String ip = "";
        SocketAddress remote = channel.remoteAddress();
        if (remote != null) {
            try {
                ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            } catch (Exception e) {
                LoggerUtil.warn("get remoteIp error!dedault will use. msg:" + e.getMessage() + ", remote:" + remote.toString());
            }
        }
        return ip;
    }

}
