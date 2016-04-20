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

import java.lang.reflect.Method;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-23
 * 
 */
@SpiMeta(name = "motan")
public class DefaultProvider<T> extends AbstractProvider<T> {
    protected T proxyImpl;

    public DefaultProvider(T proxyImpl, URL url, Class<T> clz) {
        super(url, clz);
        this.proxyImpl = proxyImpl;
    }

    @Override
    public Response invoke(Request request) {
        DefaultResponse response = new DefaultResponse();

        Method method = lookup(request);

        if (method == null) {
            MotanServiceException exception =
                    new MotanServiceException("Service method not exist: " + request.getInterfaceName() + "." + request.getMethodName()
                            + "(" + request.getParamtersDesc() + ")", MotanErrorMsgConstant.SERVICE_UNFOUND);

            response.setException(exception);
            return response;
        }

        try {
            Object value = method.invoke(proxyImpl, request.getArguments());
            response.setValue(value);
        } catch (Exception e) {
            if (e.getCause() != null) {
                LoggerUtil.error("Exception caught when method invoke: " + e.getCause());
                response.setException(new MotanBizException("provider call process error", e.getCause()));
            } else {
                response.setException(new MotanBizException("provider call process error", e));
            }
        } catch (Throwable t) {
            // 如果服务发生Error，将Error转化为Exception，防止拖垮调用方
            if (t.getCause() != null) {
                response.setException(new MotanServiceException("provider has encountered a fatal error!", t.getCause()));
            } else {
                response.setException(new MotanServiceException("provider has encountered a fatal error!", t));
            }

        }
        // 传递rpc版本和attachment信息方便不同rpc版本的codec使用。
        response.setRpcProtocolVersion(request.getRpcProtocolVersion());
        response.setAttachments(request.getAttachments());
        return response;
    }

}
