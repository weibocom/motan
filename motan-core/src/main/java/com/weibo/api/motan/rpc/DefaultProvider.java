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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;

import java.lang.reflect.Method;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-23
 */
@SpiMeta(name = "motan")
public class DefaultProvider<T> extends AbstractProvider<T> {
    protected T proxyImpl;
    protected boolean isAsync = false;

    public DefaultProvider(T proxyImpl, URL url, Class<T> clz) {
        super(url, clz);
        this.proxyImpl = proxyImpl;
        Class<?> asyncInterface = null;
        try {
            asyncInterface = Class.forName(clz.getName() + "Async");
            if (asyncInterface.isInterface() && asyncInterface.isAssignableFrom(proxyImpl.getClass())) {
                isAsync = true;
            }
        } catch (Exception ignore) {
        }

        if (isAsync) {
            initMethodMap(asyncInterface);
        } else {
            initMethodMap(clz);
        }
    }

    @Override
    public T getImpl() {
        return proxyImpl;
    }

    @Override
    public Response invoke(Request request) {
        DefaultResponse response = new DefaultResponse();

        String methodName = request.getMethodName();
        if (isAsync) { // change to async method
            methodName += "Async";
        }
        Method method = lookupMethod(methodName, request.getParamtersDesc());

        if (method == null) {
            LoggerUtil.error("can not found rpc method:" + request.getMethodName() + ", paramDesc:" + request.getParamtersDesc() + ", service:" + request.getInterfaceName());
            MotanServiceException exception =
                    new MotanServiceException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName()
                            + "(" + request.getParamtersDesc() + ")", MotanErrorMsgConstant.SERVICE_UNFOUND, false);

            response.setException(exception);
            return response;
        }

        boolean defaultThrowExceptionStack = URLParamType.transExceptionStack.getBooleanValue();
        try {
            Object value = method.invoke(proxyImpl, request.getArguments());
            if (value instanceof ResponseFuture) { // async method
                return (Response) value;
            }
            response.setValue(value);
        } catch (Exception e) {
            if (e.getCause() != null) {
                response.setException(new MotanBizException("provider call process error", e.getCause()));
            } else {
                response.setException(new MotanBizException("provider call process error", e));
            }

            // not print stack in error log when exception declared in method
            boolean logException = true;
            for (Class<?> clazz : method.getExceptionTypes()) {
                if (clazz.isInstance(response.getException().getCause())) {
                    logException = false;
                    defaultThrowExceptionStack = false;
                    break;
                }
            }
            if (logException) {
                LoggerUtil.error("Exception caught when during method invocation. request:" + request, e);
            } else {
                LoggerUtil.info("Exception caught when during method invocation. request:" + request + ", exception:" + response.getException().getCause().toString());
            }
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new MotanServiceException("provider has encountered a fatal error!", t.getCause()));
            } else {
                response.setException(new MotanServiceException("provider has encountered a fatal error!", t));
            }
            //对于Throwable,也记录日志
            LoggerUtil.error("Exception caught when during method invocation. request:" + request, t);
        }

        if (response.getException() != null) {
            //是否传输业务异常栈
            boolean transExceptionStack = this.url.getBooleanParameter(URLParamType.transExceptionStack.getName(), defaultThrowExceptionStack);
            if (!transExceptionStack) {//不传输业务异常栈
                ExceptionUtil.setMockStackTrace(response.getException().getCause());
            }
        }
        return response;
    }
}
