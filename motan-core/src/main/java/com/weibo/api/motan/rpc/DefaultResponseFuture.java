/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.common.FutureState;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;

/**
 * Created by zhanglei28 on 2017/9/11.
 */
public class DefaultResponseFuture implements ResponseFuture, Callbackable, Traceable {

    protected final Object lock = new Object();
    protected volatile FutureState state = FutureState.DOING;
    protected Object result = null;
    protected Exception exception = null;

    protected long createTime = System.currentTimeMillis();
    protected int timeout;
    protected long processTime = 0;
    protected Request request;
    protected List<FutureListener> listeners;
    protected String remoteInfo;
    protected Class<?> returnType;
    private Map<String, String> attachments;// rpc协议版本兼容时可以回传一些额外的信息
    private final TraceableContext traceableContext = new TraceableContext();
    private final DefaultCallbackHolder callbackHolder = new DefaultCallbackHolder();

    public DefaultResponseFuture(Request requestObj, int timeout, URL serverUrl) {
        this.request = requestObj;
        this.timeout = timeout;
        this.remoteInfo = serverUrl.getServerPortStr();
    }

    public DefaultResponseFuture(Request requestObj, int timeout, String remoteInfo) {
        this.request = requestObj;
        this.timeout = timeout;
        this.remoteInfo = remoteInfo;
    }


    @Override
    // for client end
    public void onSuccess(Response response) {
        this.result = response.getValue();
        this.processTime = response.getProcessTime();
        this.attachments = response.getAttachments();
        if (response instanceof Traceable) {
            traceableContext.setReceiveTime(((Traceable) response).getTraceableContext().getReceiveTime());
            traceableContext.traceInfoMap = ((Traceable) response).getTraceableContext().getTraceInfoMap();
        }

        done();
    }

    @Override
    // for client end
    public void onFailure(Response response) {
        this.exception = response.getException();
        this.processTime = response.getProcessTime();

        done();
    }

    @Override
    // for server end
    public void onSuccess(Object value) {
        this.result = value;
        done();
    }

    @Override
    // for server end
    public void onFailure(Exception e) {
        this.exception = e;
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
                    cancel(new MotanServiceException(this.getClass().getName() + " getValue InterruptedException : "
                            + MotanFrameworkUtil.toString(request) + " cost=" + (System.currentTimeMillis() - createTime), e));
                }

                return getValueOrThrowable();
            } else {
                long waitTime = timeout - (System.currentTimeMillis() - createTime);

                if (waitTime > 0) {
                    for (; ; ) {
                        try {
                            lock.wait(waitTime);
                        } catch (InterruptedException ignore) {
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
        Exception e =
                new MotanServiceException(this.getClass().getName() + " task cancel: remote info =" + remoteInfo + " "
                        + MotanFrameworkUtil.toString(request) + " cost=" + (System.currentTimeMillis() - createTime));
        return cancel(e);
    }

    protected boolean cancel(Exception e) {
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
                notifyNow = true;
            } else {
                if (listeners == null) {
                    listeners = new ArrayList<>(1);
                }

                listeners.add(listener);
            }
        }

        if (notifyNow) {
            notifyListener(listener);
        }
    }

    @Override
    public long getCreateTime() {
        return createTime;
    }

    @Override
    public void setReturnType(Class<?> clazz) {
        this.returnType = clazz;
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
            exception =
                    new MotanServiceException(this.getClass().getName() + " request timeout: remote info =" + remoteInfo
                            + " " + MotanFrameworkUtil.toString(request) + " cost=" + (System.currentTimeMillis() - createTime),
                            MotanErrorMsgConstant.SERVICE_TIMEOUT, false);

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
            LoggerUtil.error(this.getClass().getName() + " notifyListener Error: " + listener.getClass().getSimpleName(), t);
        }
    }

    private boolean isDoing() {
        return state.isDoingState();
    }

    protected boolean done() {
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

    @Override
    public long getRequestId() {
        return this.request.getRequestId();
    }

    private Object getValueOrThrowable() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new MotanServiceException(
                    exception.getMessage(), exception);
        }
        if (result != null && returnType != null && result instanceof DeserializableObject) {
            try {
                result = ((DeserializableObject) result).deserialize(returnType);
            } catch (IOException e) {
                LoggerUtil.error("deserialize response value fail! return type:" + returnType, e);
                throw new MotanFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
            }
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

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments != null ? attachments : Collections.emptyMap();
    }

    @Override
    public void setAttachment(String key, String value) {
        if (this.attachments == null) {
            this.attachments = new HashMap<>();
        }
        this.attachments.put(key, value);
    }

    @Override
    public byte getRpcProtocolVersion() {
        return RpcProtocolVersion.VERSION_1.getVersion();
    }

    @Override
    public void setRpcProtocolVersion(byte rpcProtocolVersion) {
    }

    @Override
    public TraceableContext getTraceableContext() {
        return traceableContext;
    }

    @Override
    public void setSerializeNumber(int number) {
    }

    @Override
    public int getSerializeNumber() {
        return 0;
    }

    @Override
    public void addFinishCallback(Runnable runnable, Executor executor) {
        callbackHolder.addFinishCallback(runnable, executor);
    }

    @Override
    public void onFinish() {
        callbackHolder.onFinish();
    }

    @Override
    public Callbackable getCallbackHolder() {
        return callbackHolder;
    }
}
