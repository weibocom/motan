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

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Response received via rpc.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
public class DefaultResponse implements Response, Traceable, Callbackable, Serializable {
    private static final long serialVersionUID = 4281186647291615871L;
    private Object value;
    private Exception exception;
    private long requestId;
    private long processTime;
    private int timeout;
    private Map<String, String> attachments;// rpc协议版本兼容时可以回传一些额外的信息
    private byte rpcProtocolVersion = RpcProtocolVersion.VERSION_1.getVersion();
    private int serializeNumber = 0;// default serialization is hessian2
    private TraceableContext traceableContext = new TraceableContext();
    private Callbackable callbackHolder = new DefaultCallbackHolder();

    public DefaultResponse() {
    }

    public DefaultResponse(long requestId) {
        this.requestId = requestId;
    }

    // for client end. Blocking to get value or throw exception
    public DefaultResponse(Response response) {
        this.value = response.getValue();
        this.exception = response.getException();
        this.requestId = response.getRequestId();
        this.processTime = response.getProcessTime();
        this.timeout = response.getTimeout();
        this.rpcProtocolVersion = response.getRpcProtocolVersion();
        this.serializeNumber = response.getSerializeNumber();
        this.attachments = response.getAttachments();
        updateTraceableContextFromResponse(response);
    }

    public DefaultResponse(Object value) {
        this.value = value;
    }

    public DefaultResponse(Object value, long requestId) {
        this.value = value;
        this.requestId = requestId;
    }

    public static DefaultResponse fromServerEndResponseFuture(ResponseFuture responseFuture) {
        DefaultResponse response = new DefaultResponse();
        if (responseFuture.getException() != null) { // change to biz exception
            response.setException(new MotanBizException("provider call process error", responseFuture.getException()));
        } else {
            response.setValue(responseFuture.getValue());
        }
        response.updateTraceableContextFromResponse(responseFuture);
        response.updateCallbackHolderFromResponse(responseFuture);
        return response;
    }

    @Override
    public Object getValue() {
        if (exception != null) {
            throw (exception instanceof RuntimeException) ? (RuntimeException) exception : new MotanServiceException(
                    exception.getMessage(), exception);
        }

        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
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
        return this.timeout;
    }

    @Override
    public Map<String, String> getAttachments() {
        return attachments != null ? attachments : Collections.emptyMap();
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
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
        return rpcProtocolVersion;
    }

    @Override
    public void setRpcProtocolVersion(byte rpcProtocolVersion) {
        this.rpcProtocolVersion = rpcProtocolVersion;
    }

    @Override
    public void setSerializeNumber(int number) {
        this.serializeNumber = number;
    }

    @Override
    public int getSerializeNumber() {
        return serializeNumber;
    }

    /**
     * 未指定线程池时，统一使用默认线程池执行。默认线程池满时采用丢弃策略，不保证任务一定会被执行。
     * 如果默认线程池不满足需求时，可以自行携带executor。
     *
     * @param runnable 准备在response on finish时执行的任务
     * @param executor 指定执行任务的线程池
     */
    public void addFinishCallback(Runnable runnable, Executor executor) {
        callbackHolder.addFinishCallback(runnable, executor);
    }

    @Override
    public void onFinish() {
        callbackHolder.onFinish();
    }

    @Override
    public TraceableContext getTraceableContext() {
        return traceableContext;
    }

    @Override
    public Callbackable getCallbackHolder() {
        return callbackHolder;
    }

    // only for constructor
    private void updateTraceableContextFromResponse(Response response) {
        if (response instanceof Traceable) {
            TraceableContext tempTraceableContext = ((Traceable) response).getTraceableContext();
            if (tempTraceableContext != null) {
                traceableContext = tempTraceableContext;
            }
        }
    }

    // only for constructor
    private void updateCallbackHolderFromResponse(Response response) {
        if (response instanceof Callbackable) {
            Callbackable holder = ((Callbackable) response).getCallbackHolder();
            if (holder != null) {
                callbackHolder = holder;
            }
        }
    }
}
