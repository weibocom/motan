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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.RpcContext;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.NetUtils;
import org.jboss.netty.channel.*;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 * 
 */
public class NettyChannelHandler extends SimpleChannelHandler {
	private ThreadPoolExecutor threadPoolExecutor;
	private MessageHandler messageHandler;
	private Channel serverChannel;

	public NettyChannelHandler(Channel serverChannel) {
		this.serverChannel = serverChannel;
	}

	public NettyChannelHandler(Channel serverChannel, MessageHandler messageHandler) {
		this.serverChannel = serverChannel;
		this.messageHandler = messageHandler;
	}

	public NettyChannelHandler(Channel serverChannel, MessageHandler messageHandler,
			ThreadPoolExecutor threadPoolExecutor) {
		this.serverChannel = serverChannel;
		this.messageHandler = messageHandler;
		this.threadPoolExecutor = threadPoolExecutor;
	}

	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		LoggerUtil.info("NettyChannelHandler channelConnected: remote=" + ctx.getChannel().getRemoteAddress()
				+ " local=" + ctx.getChannel().getLocalAddress() + " event=" + e.getClass().getSimpleName());
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		LoggerUtil.info("NettyChannelHandler channelDisconnected: remote=" + ctx.getChannel().getRemoteAddress()
				+ " local=" + ctx.getChannel().getLocalAddress() + " event=" + e.getClass().getSimpleName());
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object message = e.getMessage();

		if (message instanceof Request) {
			processRequest(ctx, e);
		} else if (message instanceof Response) {
			processResponse(ctx, e);
		} else {
			LoggerUtil.error("NettyChannelHandler messageReceived type not support: class=" + message.getClass());
			throw new MotanFrameworkException("NettyChannelHandler messageReceived type not support: class="
					+ message.getClass());
		}
	}

	/**
	 * <pre>
	 *  request process: 主要来自于client的请求，需要使用threadPoolExecutor进行处理，避免service message处理比较慢导致iothread被阻塞
	 * </pre>
	 * 
	 * @param ctx
	 * @param e
	 */
	private void processRequest(final ChannelHandlerContext ctx, MessageEvent e) {
		final Request request = (Request) e.getMessage();
		request.setAttachment(URLParamType.host.getName(), NetUtils.getHostName(ctx.getChannel().getRemoteAddress()));

		final long processStartTime = System.currentTimeMillis();

		// 使用线程池方式处理
		try {
			threadPoolExecutor.execute(new Runnable() {
				@Override
                public void run() {
				    try{
				        RpcContext.init(request);
	                    processRequest(ctx, request, processStartTime);
				    }finally{
				        RpcContext.destroy();
				    }
                }
            });
		} catch (RejectedExecutionException rejectException) {
			DefaultResponse response = new DefaultResponse();
			response.setRequestId(request.getRequestId());
			response.setException(new MotanServiceException("process thread pool is full, reject",
					MotanErrorMsgConstant.SERVICE_REJECT));
			response.setProcessTime(System.currentTimeMillis() - processStartTime);
			e.getChannel().write(response);

			LoggerUtil
					.debug("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
							threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(),
							threadPoolExecutor.getCorePoolSize(), threadPoolExecutor.getMaximumPoolSize(),
							threadPoolExecutor.getTaskCount(), request.getRequestId());
		}
	}

	private void processRequest(ChannelHandlerContext ctx, Request request, long processStartTime) {
		Object result;
		try{
			result = messageHandler.handle(serverChannel, request);
		} catch (Exception e){
			LoggerUtil.error("NettyChannelHandler processRequest fail!request:" + MotanFrameworkUtil.toString(request), e);
			result = MotanFrameworkUtil.buildErrorResponse(request.getRequestId(), new MotanServiceException("process request fail. errmsg:" + e.getMessage()));
		}

		DefaultResponse response = null;

		if (!(result instanceof DefaultResponse)) {
			response = new DefaultResponse(result);
		} else {
			response = (DefaultResponse) result;
		}

		response.setRequestId(request.getRequestId());
		response.setProcessTime(System.currentTimeMillis() - processStartTime);

		if (ctx.getChannel().isConnected()) {
			ctx.getChannel().write(response);
		}
	}

	private void processResponse(ChannelHandlerContext ctx, MessageEvent e) {
		messageHandler.handle(serverChannel, e.getMessage());
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		LoggerUtil.error("NettyChannelHandler exceptionCaught: remote=" + ctx.getChannel().getRemoteAddress()
				+ " local=" + ctx.getChannel().getLocalAddress() + " event=" + e.getCause(), e.getCause());

		ctx.getChannel().close();
	}
}
