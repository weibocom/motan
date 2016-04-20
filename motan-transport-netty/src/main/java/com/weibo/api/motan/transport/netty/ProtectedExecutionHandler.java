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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.execution.ExecutionHandler;

import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 * 
 */
public class ProtectedExecutionHandler extends ExecutionHandler {
	private ThreadPoolExecutor threadPoolExecutor;

	ProtectedExecutionHandler(final ThreadPoolExecutor threadPoolExecutor) {
		super(threadPoolExecutor);
		this.threadPoolExecutor = threadPoolExecutor;
	}

	/**
	 * if RejectedExecutionException happen, send 503 exception to client
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext context, ChannelEvent e) throws Exception {
		try {
			super.handleUpstream(context, e);
		} catch (RejectedExecutionException rejectException) {
			if (e instanceof MessageEvent) {
				if (((MessageEvent) e).getMessage() instanceof Request) {
					Request request = (Request) ((MessageEvent) e).getMessage();
					DefaultResponse response = new DefaultResponse();
					response.setRequestId(request.getRequestId());
					response.setException(new MotanServiceException("process thread pool is full, reject",
							MotanErrorMsgConstant.SERVICE_REJECT));
					e.getChannel().write(response);

					LoggerUtil
							.debug("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
									threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(),
									threadPoolExecutor.getCorePoolSize(), threadPoolExecutor.getMaximumPoolSize(),
									threadPoolExecutor.getTaskCount(), request.getRequestId());
				}
			}
		}
	}

}
