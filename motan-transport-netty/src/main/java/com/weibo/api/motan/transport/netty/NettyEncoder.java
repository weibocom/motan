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
import com.weibo.api.motan.protocol.rpc.DefaultRpcCodec;
import com.weibo.api.motan.protocol.v2motan.MotanV2Header;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.ByteUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import java.io.IOException;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 */
@SuppressWarnings("all")
public class NettyEncoder extends OneToOneEncoder {
    private Codec codec;
    private com.weibo.api.motan.transport.Channel client;

    public NettyEncoder(Codec codec, com.weibo.api.motan.transport.Channel client) {
        this.codec = codec;
        this.client = client;
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel nettyChannel, Object message) throws Exception {
        byte[] data = encodeMessage(message);
        ChannelBuffer channelBuffer;
        short type = ByteUtil.bytes2short(data, 0);
        if (type == DefaultRpcCodec.MAGIC) {
            channelBuffer = encodeV1(message, data);
        } else if (type == MotanV2Header.MAGIC) {
            channelBuffer  = encodeV2(data);
        } else {
            throw new MotanFrameworkException("can not encode message, unknown magic:" + type);
        }
        if (message instanceof Response) {
            ((Response) message).setAttachment(MotanConstants.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));
        }
        if (message instanceof Request) {
            MotanFrameworkUtil.logEvent((Request) message, MotanConstants.TRACE_CENCODE);
        } else if (message instanceof Response) {
            MotanFrameworkUtil.logEvent((Response) message, MotanConstants.TRACE_SENCODE);
        }
        return channelBuffer;
    }


    private ChannelBuffer encodeV2(byte[] data) throws Exception {
        return ChannelBuffers.wrappedBuffer(data);
    }

    private ChannelBuffer encodeV1(Object message, byte[] data) throws Exception {
        long requestId = getRequestId(message);
        byte[] transportHeader = new byte[MotanConstants.NETTY_HEADER];
        ByteUtil.short2bytes(MotanConstants.NETTY_MAGIC_TYPE, transportHeader, 0);
        transportHeader[3] = getType(message);
        ByteUtil.long2bytes(requestId, transportHeader, 4);
        ByteUtil.int2bytes(data.length, transportHeader, 12);
        return ChannelBuffers.wrappedBuffer(transportHeader, data);
    }

    private byte[] encodeMessage(Object message) throws IOException {
        byte[] data = null;
        if (message instanceof Response) {
            try {
                data = codec.encode(client, message);
            } catch (Exception e) {
                LoggerUtil.error("NettyEncoder encode error, identity=" + client.getUrl().getIdentity(), e);
                long requestId = getRequestId(message);
                Response response = MotanFrameworkUtil.buildErrorResponse(requestId, ((Response) message).getRpcProtocolVersion(), e);
                data = codec.encode(client, response);
            }
        } else {
            data = codec.encode(client, message);
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
