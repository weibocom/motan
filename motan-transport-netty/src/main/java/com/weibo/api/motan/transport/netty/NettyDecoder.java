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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * netty client decode
 * 
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 * 
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
		if (buffer.readableBytes() <= MotanConstants.NETTY_HEADER) {
			return null;
		}

		buffer.markReaderIndex();

		short type = buffer.readShort();
		
		if (type != MotanConstants.NETTY_MAGIC_TYPE) {
			buffer.resetReaderIndex();
			throw new MotanFrameworkException("NettyDecoder transport header not support, type: " + type);
		}

		byte messageType = (byte) buffer.readShort();
		long requestId = buffer.readLong();

		int dataLength = buffer.readInt();

		// FIXME 如果dataLength过大，可能导致问题
		if (buffer.readableBytes() < dataLength) {
			buffer.resetReaderIndex();
			return null;
		}

		if (maxContentLength > 0 && dataLength > maxContentLength) {
			LoggerUtil.warn(
					"NettyDecoder transport data content length over of limit, size: {}  > {}. remote={} local={}",
					dataLength, maxContentLength, ctx.getChannel().getRemoteAddress(), ctx.getChannel()
							.getLocalAddress());
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

		
		byte[] data = new byte[dataLength];

		buffer.readBytes(data);

		try {
		    String remoteIp = getRemoteIp(channel);
			return codec.decode(client, remoteIp, data);
		} catch (Exception e) {
			if (messageType == MotanConstants.FLAG_REQUEST) {
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
