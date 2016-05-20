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

package com.weibo.api.motan.transport.netty4.client;

import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 * 
 */
public class NettyChannel implements com.weibo.api.motan.transport.Channel {
	private volatile ChannelState state = ChannelState.UNINIT;

	private Netty4Client nettyClient;

	private io.netty.channel.Channel channel = null;

	private InetSocketAddress remoteAddress = null;
	private InetSocketAddress localAddress = null;

	public NettyChannel(Netty4Client nettyClient) {
		this.nettyClient = nettyClient;
		this.remoteAddress = new InetSocketAddress(nettyClient.getUrl().getHost(), nettyClient.getUrl().getPort());
	}

	@Override
	public Response request(Request request) throws TransportException {
	    int timeout = nettyClient.getUrl().getMethodParameter(request.getMethodName(), request.getParamtersDesc(),
	            URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getIntValue());
		if (timeout <= 0) {
               throw new MotanFrameworkException("Netty4Client init Error: timeout(" + timeout + ") <= 0 is forbid.",
                       MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
           }
		NettyResponseFuture response = new NettyResponseFuture(request, timeout, this.nettyClient);
		this.nettyClient.registerCallback(request.getRequestId(), response);

		ChannelFuture writeFuture = this.channel.writeAndFlush(request);

		boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.SECONDS);

		if (result && writeFuture.isSuccess()) {
			response.addListener(new FutureListener() {
				@Override
				public void operationComplete(Future future) throws Exception {
					if (future.isSuccess() || (future.isDone() && ExceptionUtil.isBizException(future.getException()))) {
						// 成功的调用 
						nettyClient.resetErrorCount();
					} else {
						// 失败的调用 
						nettyClient.incrErrorCount();
					}
				}
			});
			return response;
		}

		writeFuture.cancel(true);
		response = this.nettyClient.removeCallback(request.getRequestId());

		if (response != null) {
			response.cancel();
		}

		// 失败的调用 
		nettyClient.incrErrorCount();

		if (writeFuture.cause() != null) {
			throw new MotanServiceException("NettyChannel send request to server Error: url="
					+ nettyClient.getUrl().getUri() + " local=" + localAddress + " "
					+ MotanFrameworkUtil.toString(request), writeFuture.cause());
		} else {
			throw new MotanServiceException("NettyChannel send request to server Timeout: url="
					+ nettyClient.getUrl().getUri() + " local=" + localAddress + " "
					+ MotanFrameworkUtil.toString(request));
		}
	}

	@Override
	public synchronized boolean open() {
		if (isAvailable()) {
			LoggerUtil.warn("the channel already open, local: " + localAddress + " remote: " + remoteAddress + " url: "
					+ nettyClient.getUrl().getUri());
			return true;
		}

		try {
			ChannelFuture channelFuture = nettyClient.getBootstrap().connect(
					new InetSocketAddress(nettyClient.getUrl().getHost(), nettyClient.getUrl().getPort()));

			long start = System.currentTimeMillis();

			int timeout = nettyClient.getUrl().getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
			if (timeout <= 0) {
	            throw new MotanFrameworkException("Netty4Client init Error: timeout(" + timeout + ") <= 0 is forbid.",
	                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
			}
			// 不去依赖于connectTimeout
			boolean result = channelFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            boolean success = channelFuture.isSuccess();

			if (result && success) {
				channel = channelFuture.channel();
				if (channel.localAddress() != null && channel.localAddress() instanceof InetSocketAddress) {
					localAddress = (InetSocketAddress) channel.localAddress();
				}

				state = ChannelState.ALIVE;
				return true;
			}
            boolean connected = false;
            if(channelFuture.channel() != null){
                connected = channelFuture.channel().isOpen();
            }

			if (channelFuture.cause() != null) {
				channelFuture.cancel(true);
				throw new MotanServiceException("NettyChannel failed to connect to server, url: "
						+ nettyClient.getUrl().getUri()+ ", result: " + result + ", success: " + success + ", connected: " + connected, channelFuture.cause());
			} else {
				channelFuture.cancel(true);
                throw new MotanServiceException("NettyChannel connect to server timeout url: "
                        + nettyClient.getUrl().getUri() + ", cost: " + (System.currentTimeMillis() - start) + ", result: " + result + ", success: " + success + ", connected: " + connected);
            }
		} catch (MotanServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new MotanServiceException("NettyChannel failed to connect to server, url: "
					+ nettyClient.getUrl().getUri(), e);
		} finally {
			if (!state.isAliveState()) {
				nettyClient.incrErrorCount();
			}
		}
	}

	@Override
	public synchronized void close() {
		close(0);
	}

	@Override
	public synchronized void close(int timeout) {
		try {
			if (channel != null) {
				channel.close();
			}

			state = ChannelState.CLOSE;
		} catch (Exception e) {
			LoggerUtil
					.error("NettyChannel close Error: " + nettyClient.getUrl().getUri() + " local=" + localAddress, e);
		}
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return localAddress;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return remoteAddress;
	}

	@Override
	public boolean isClosed() {
		return state.isCloseState();
	}

	@Override
	public boolean isAvailable() {
		return state.isAliveState();
	}

	@Override
	public URL getUrl() {
		return nettyClient.getUrl();
	}
}
