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

import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.weibo.api.motan.core.StandardThreadExecutor.DEFAULT_MAX_IDLE_TIME;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Response received via rpc.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
public class DefaultResponse implements Response, Traceable, Callbackable, Serializable {
    private static final long serialVersionUID = 4281186647291615871L;
    protected static ThreadPoolExecutor defaultCallbackExecutor = new StandardThreadExecutor(20, 200,
            DEFAULT_MAX_IDLE_TIME, MILLISECONDS, 5000,
            new DefaultThreadFactory("defaultResponseCallbackPool-", true), new ThreadPoolExecutor.DiscardPolicy());

    private Object value;
    private Exception exception;
    private long requestId;
    private long processTime;
    private int timeout;
    private Map<String, String> attachments;// rpc协议版本兼容时可以回传一些额外的信息
    private byte rpcProtocolVersion = RpcProtocolVersion.VERSION_1.getVersion();
    private int serializeNumber = 0;// default serialization is hessian2
    private List<Pair<Runnable, Executor>> taskList = new ArrayList<>();
    private AtomicBoolean isFinished = new AtomicBoolean();
    private TraceableContext traceableContext = new TraceableContext();

    public DefaultResponse() {
    }

    public DefaultResponse(long requestId) {
        this.requestId = requestId;
    }

    public DefaultResponse(Response response) {
        this.value = response.getValue();
        this.exception = response.getException();
        this.requestId = response.getRequestId();
        this.processTime = response.getProcessTime();
        this.timeout = response.getTimeout();
        this.rpcProtocolVersion = response.getRpcProtocolVersion();
        this.serializeNumber = response.getSerializeNumber();
        this.attachments = response.getAttachments();
        if (response instanceof Traceable) {
            traceableContext.setReceiveTime(((Traceable) response).getTraceableContext().getReceiveTime());
            traceableContext.traceInfoMap = ((Traceable) response).getTraceableContext().getTraceInfoMap();
        }
    }

    public DefaultResponse(Object value) {
        this.value = value;
    }

    public DefaultResponse(Object value, long requestId) {
        this.value = value;
        this.requestId = requestId;
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
     * @param runnable 准备在response on finish时执行的任务
     * @param executor 指定执行任务的线程池
     */
    public void addFinishCallback(Runnable runnable, Executor executor) {
        if (!isFinished.get()) {
            taskList.add(Pair.of(runnable, executor));
        }
    }

    @Override
    public void onFinish() {
        if (!isFinished.compareAndSet(false, true)) {
            return;
        }
        for (Pair<Runnable, Executor> pair : taskList) {
            Runnable runnable = pair.getKey();
            Executor executor = pair.getValue();
            if (executor == null) {
                executor = defaultCallbackExecutor;
            }
            try {
                executor.execute(runnable);
            } catch (Exception e) {
                LoggerUtil.error("Callbackable response exec callback task error, e: ", e);
            }
        }
    }

    @Override
    public TraceableContext getTraceableContext() {
        return traceableContext;
    }
}
