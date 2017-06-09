/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.transport.netty;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * netty client decode
 *
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 */
public class NettyDecoder extends FrameDecoder {

    private Codec codec;
    private com.weibo.api.motan.transport.Channel client;
    private int maxContentLength = 0;

    public NettyDecoder(Codec codec, com.weibo.api.motan.transport.Channel client, int maxContentLength) {
        this.codec = codec;
        this.client = client;
        this.maxContentLength = maxContentLength;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        //根据版本号决定走哪个分支
        if (buffer.readableBytes() <= MotanConstants.NETTY_HEADER) {
            return null;
        }
        buffer.markReaderIndex();

        short type = buffer.readShort();

        if (type != MotanConstants.NETTY_MAGIC_TYPE) {
            buffer.resetReaderIndex();
            throw new MotanFrameworkException("NettyDecoder transport header not support, type: " + type);
        }
        buffer.skipBytes(1);
        int rpcversion = (buffer.readByte() & 0xff) >>> 3;
        switch (rpcversion) {
            case 0:
                return decodev1(ctx, channel, buffer);
            case 1:
                return decodev2(ctx, channel, buffer);
            default:
                return decodev2(ctx, channel, buffer);
        }

    }

    private Object decodev2(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        buffer.resetReaderIndex();
        if(buffer.readableBytes() < 21){
            return null;
        }
        buffer.skipBytes(2);
        boolean isRequest = isV2Request(buffer.readByte());
        buffer.skipBytes(2);
        long requestId = buffer.readLong();
        int size = 13;
        int metasize = buffer.readInt();
        size += 4;
        if (metasize > 0) {
            size += metasize;
            if (buffer.readableBytes() < metasize) {
                buffer.resetReaderIndex();
                return null;
            }
            buffer.skipBytes(metasize);
        }
        if(buffer.readableBytes() < 4){
            buffer.resetReaderIndex();
            return null;
        }
        int bodysize = buffer.readInt();
        checkMaxContext(bodysize, ctx, channel, isRequest, requestId);
        size += 4;
        if (bodysize > 0){
            size += bodysize;
            if (buffer.readableBytes() < bodysize) {
                buffer.resetReaderIndex();
                return null;
            }
        }
        byte[] data = new byte[size];
        buffer.resetReaderIndex();
        buffer.readBytes(data);
        return decode(data, channel, isRequest, requestId);
    }

    private boolean isV2Request(byte b){
        return (b & 0x01) == 0x00;
    }

    private Object decodev1(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        buffer.resetReaderIndex();
        buffer.skipBytes(2);// skip magic num
        byte messageType = (byte) buffer.readShort();
        long requestId = buffer.readLong();
        int dataLength = buffer.readInt();

        // FIXME 如果dataLength过大，可能导致问题
        if (buffer.readableBytes() < dataLength) {
            buffer.resetReaderIndex();
            return null;
        }
        checkMaxContext(dataLength, ctx, channel, messageType == MotanConstants.FLAG_REQUEST, requestId);

        byte[] data = new byte[dataLength];

        buffer.readBytes(data);
        return decode(data, channel, messageType == MotanConstants.FLAG_REQUEST, requestId);
    }

    private void checkMaxContext(int dataLength, ChannelHandlerContext ctx, Channel channel, boolean isRequest, long requestId) throws Exception {
        if (maxContentLength > 0 && dataLength > maxContentLength) {
            LoggerUtil.warn(
                    "NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
                    dataLength, maxContentLength, ctx.getChannel().getRemoteAddress(), ctx.getChannel()
                            .getLocalAddress());
            Exception e = new MotanServiceException("NettyDecoder transport data content length over of limit, size: "
                    + dataLength + " > " + maxContentLength);

            if (isRequest) {
                Response response = buildExceptionResponse(requestId, e);
                channel.write(response);
                throw e;
            } else {
                throw e;
            }
        }
    }

    private Object decode(byte[] data, Channel channel, boolean isRequest, long requestId){
        try {
            String remoteIp = getRemoteIp(channel);
            return codec.decode(client, remoteIp, data);
        } catch (Exception e) {
            LoggerUtil.error("NettyDecoder decode fail! requestid" + requestId + ", size:" + data.length, e);
            if (isRequest) {
                Response resonse = buildExceptionResponse(requestId, e);
                channel.write(resonse);
                return null;
            } else {
                Response resonse = buildExceptionResponse(requestId, e);
                return resonse;
            }
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }


    private String getRemoteIp(Channel channel) {
        String ip = "";
        SocketAddress remote = channel.getRemoteAddress();
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
