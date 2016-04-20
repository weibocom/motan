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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.weibo.api.motan.common.FutureState;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;
import com.weibo.api.motan.rpc.Future;
import com.weibo.api.motan.rpc.FutureListener;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * netty response
 * 
 * <pre>
 * 		1） getValue() :  
 * 
 * 			if (request is timeout or request is cancel or get exception)
 * 				 throw exception; 
 * 			else 
 * 				 return value;
 * 
 * 		2） getException() :
 * 		
 * 			if (task is doing) :
 * 				 return null
 * 			if (task is done and get exception):
 * 				return exception
 * 
 * </pre>
 * 
 * @author maijunsheng
 * @version 创建时间：2013-5-31
 * 
 */
public class NettyResponseFuture implements Response, Future {
	private volatile FutureState state = FutureState.DOING;

	private Object lock = new Object();

	private Object result = null;
	private Exception exception = null;

	private long createTime = System.currentTimeMillis();
	private int timeout = 0;
	private long processTime = 0;

	private Request request;
	private List<FutureListener> listeners;
	private Channel channel;

	public NettyResponseFuture(Request requestObj, int timeout, Channel channel) {
		this.request = requestObj;
		this.timeout = timeout;
		this.channel = channel;
	}

	public void onSuccess(Response response) {
		this.result = response.getValue();
		this.processTime = response.getProcessTime();

		done();
	}

	public void onFailure(Response response) {
		this.exception = response.getException();
		this.processTime = response.getProcessTime();

		done();
	}

	@Override
	public Object getValue() {
		synchronized (lock) {
			if (!isDoing()) {
				return getValueOrThrowable();
			}

			if (timeout <= 0) {
				try {
					lock.wait();
				} catch (Exception e) {
					cancel(new MotanServiceException("NettyResponseFuture getValue InterruptedException : "
							+ MotanFrameworkUtil.toString(request) + " cost="
							+ (System.currentTimeMillis() - createTime), e));
				}

				// don't need to notifylisteners, because onSuccess or
				// onFailure or cancel method already call notifylisteners
				return getValueOrThrowable();
			} else {
				long waitTime = timeout - (System.currentTimeMillis() - createTime);

				if (waitTime > 0) {
					for (;;) {
						try {
							lock.wait(waitTime);
						} catch (InterruptedException e) {
						}

						if (!isDoing()) {
							break;
						} else {
							waitTime = timeout - (System.currentTimeMillis() - createTime);
							if (waitTime <= 0) {
								break;
							}
						}
					}
				}

				if (isDoing()) {
					timeoutSoCancel();
				}
			}
			return getValueOrThrowable();
		}
	}

	@Override
	public Exception getException() {
		return exception;
	}

	@Override
	public boolean cancel() {
		Exception e = new MotanServiceException("NettyResponseFuture task cancel: serverPort="
				+ channel.getUrl().getServerPortStr() + " " + MotanFrameworkUtil.toString(request) + " cost="
				+ (System.currentTimeMillis() - createTime));
		return cancel(e);
	}
	
	private boolean cancel(Exception e) {
		synchronized (lock) {
			if (!isDoing()) {
				return false;
			}

			state = FutureState.CANCELLED;
			exception = e;
			lock.notifyAll();
		}

		notifyListeners();
		return true;
	}

	@Override
	public boolean isCancelled() {
		return state.isCancelledState();
	}

	@Override
	public boolean isDone() {
		return state.isDoneState();
	}

	@Override
	public boolean isSuccess() {
		return isDone() && (exception == null);
	}

	@Override
	public void addListener(FutureListener listener) {
		if (listener == null) {
			throw new NullPointerException("FutureListener is null");
		}

		boolean notifyNow = false;
		synchronized (lock) {
			if (!isDoing()) {
				// is success, failure, timeout or cancel, don't add into
				// listeners, just notify
				notifyNow = true;
			} else {
				if (listeners == null) {
					listeners = new ArrayList<FutureListener>(1);
				}

				listeners.add(listener);
			}
		}

		if (notifyNow) {
			notifyListener(listener);
		}
	}

	public long getCreateTime() {
		return createTime;
	}

	public Object getRequestObj() {
		return request;
	}

	public FutureState getState() {
		return state;
	}

	private void timeoutSoCancel() {
		this.processTime = System.currentTimeMillis() - createTime;

		synchronized (lock) {
			if (!isDoing()) {
				return;
			}
			
			state = FutureState.CANCELLED;
			exception = new MotanServiceException("NettyResponseFuture request timeout: serverPort="
					+ channel.getUrl().getServerPortStr() + " " + MotanFrameworkUtil.toString(request) + " cost="
					+ (System.currentTimeMillis() - createTime), MotanErrorMsgConstant.SERVICE_TIMEOUT);
			
			lock.notifyAll();
		}

		notifyListeners();
	}

	private void notifyListeners() {
		if (listeners != null) {
			for (FutureListener listener : listeners) {
				notifyListener(listener);
			}
		}
	}

	private void notifyListener(FutureListener listener) {
		try {
			listener.operationComplete(this);
		} catch (Throwable t) {
			LoggerUtil.error("NettyResponseFuture notifyListener Error: " + listener.getClass().getSimpleName(), t);
		}
	}

	private boolean isDoing() {
		return state.isDoingState();
	}

	private boolean done() {
		synchronized (lock) {
			if (!isDoing()) {
				return false;
			}

			state = FutureState.DONE;
			lock.notifyAll();
		}

		notifyListeners();
		return true;
	}

	public long getRequestId() {
		return this.request.getRequestId();
	}

	private Object getValueOrThrowable() {
		if (exception != null) {
			throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new MotanServiceException(
					exception.getMessage(), exception);
		}

		return result;
	}

	@Override
	public long getProcessTime() {
		return processTime;
	}

	@Override
	public void setProcessTime(long time) {
		this.processTime = time;
	}
	
	public int getTimeout() {
	    return timeout;
	}

    @Override
    public Map<String, String> getAttachments() {
        // 不需要使用
        return Collections.EMPTY_MAP;
    }

    @Override
    public void setAttachment(String key, String value) {}

    @Override
    public void setRpcProtocolVersion(byte rpcProtocolVersion) {}

    @Override
    public byte getRpcProtocolVersion() {
        return RpcProtocolVersion.VERSION_1.getVersion();
    }
}
