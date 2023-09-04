/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
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

package com.weibo.api.motan.proxy;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.MeshClient;
import com.weibo.api.motan.util.MotanClientUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author zhanglei28
 * @date 2022/12/28.
 */
public class MeshClientRefererInvocationHandler<T> extends AbstractRefererHandler<T> implements InvocationHandler, CommonClient {
    protected MeshClient meshClient;
    protected URL refUrl;

    /**
     * only for InvocationHandler
     */
    public MeshClientRefererInvocationHandler(Class<T> clz, URL refUrl, MeshClient meshClient) {
        this.clz = clz;
        this.refUrl = refUrl;
        this.interfaceName = MotanFrameworkUtil.removeAsyncSuffix(clz.getName());
        this.meshClient = meshClient;
    }

    /**
     * only for CommonClient
     */
    public MeshClientRefererInvocationHandler(URL refUrl, MeshClient meshClient) {
        this.refUrl = refUrl;
        this.interfaceName = refUrl.getPath();
        this.meshClient = meshClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isLocalMethod(method)) {
            if ("toString".equals(method.getName())) {
                return innerToString();
            }
            if ("equals".equals(method.getName())) {
                return refUrl.equals(args[0]);
            }
            if ("hashCode".equals(method.getName())) {
                return refUrl.hashCode();
            }
            throw new MotanServiceException("can not invoke local method:" + method.getName());
        }

        DefaultRequest request = new DefaultRequest();
        boolean async = fillDefaultRequest(request, method, args);
        return invokeRequest(request, getRealReturnType(async, this.clz, method, request.getMethodName()), async);
    }

    Object invokeRequest(Request request, Class returnType, boolean async) throws Throwable {
        fillWithContext(request, async);
        setDefaultAttachments(request, URLParamType.application.getName(),
                URLParamType.group.getName(), URLParamType.module.getName(), URLParamType.version.getName());
        // set request timeout
        String timeout = refUrl.getMethodParameter(request.getMethodName(), request.getParamtersDesc(), URLParamType.requestTimeout.getName());
        if (timeout != null) {
            request.setAttachment(MotanConstants.M2_TIMEOUT, timeout);
        }
        return call(meshClient, refUrl, request, returnType, async);
    }

    private void setDefaultAttachments(Request request, String... keys) {
        for (String key : keys) {
            String value = refUrl.getParameter(key);
            if (StringUtils.isNotEmpty(value)) {
                request.setAttachment(key, value);
            }
        }
    }

    private String innerToString() {
        return "referer: " + refUrl.toFullStr() + " - mesh client: " + meshClient.getUrl().toFullStr();
    }

    @Override
    public Object call(String methodName, Object[] arguments, Class returnType) throws Throwable {
        return invokeRequest(buildRequest(methodName, arguments), returnType, false);
    }

    @Override
    public Object asyncCall(String methodName, Object[] arguments, Class returnType) throws Throwable {
        return invokeRequest(buildRequest(methodName, arguments), returnType, true);
    }

    @Override
    public Object call(Request request, Class returnType) throws Throwable {
        return invokeRequest(request, returnType, false);
    }

    @Override
    public Object asyncCall(Request request, Class returnType) throws Throwable {
        return invokeRequest(request, returnType, true);
    }

    @Override
    public Request buildRequest(String methodName, Object[] arguments) {
        return buildRequest(interfaceName, methodName, arguments);
    }

    @Override
    public Request buildRequest(String interfaceName, String methodName, Object[] arguments) {
        return MotanClientUtil.buildRequest(interfaceName, methodName, arguments);
    }

}
