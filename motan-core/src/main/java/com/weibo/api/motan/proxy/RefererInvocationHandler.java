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

package com.weibo.api.motan.proxy;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.api.motan.util.RequestIdGenerator;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @param <T>
 * @author maijunsheng
 */
public class RefererInvocationHandler<T> extends AbstractRefererHandler<T> implements InvocationHandler {

    public RefererInvocationHandler(Class<T> clz, List<Cluster<T>> clusters) {
        this.clz = clz;
        this.clusters = clusters;
        init();
        interfaceName = MotanFrameworkUtil.removeAsyncSuffix(clz.getName());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isLocalMethod(method)) {
            if ("toString".equals(method.getName())) {
                return clustersToString();
            }
            if ("equals".equals(method.getName())) {
                return proxyEquals(args[0]);
            }
            throw new MotanServiceException("can not invoke local method:" + method.getName());
        }

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setArguments(args);
        String methodName = method.getName();
        boolean async = false;
        if (methodName.endsWith(MotanConstants.ASYNC_SUFFIX) && method.getReturnType().equals(ResponseFuture.class)) {
            methodName = MotanFrameworkUtil.removeAsyncSuffix(methodName);
            async = true;
        }
        request.setMethodName(methodName);
        request.setParamtersDesc(ReflectUtil.getMethodParamDesc(method));
        request.setInterfaceName(interfaceName);

        return invokeRequest(request, getRealReturnType(async, this.clz, method, methodName), async);
    }

    /**
     * tostring,equals,hashCode,finalize等接口未声明的方法不进行远程调用
     *
     * @param method
     * @return
     */
    public boolean isLocalMethod(Method method) {
        if (method.getDeclaringClass().equals(Object.class)) {
            try {
                Method interfaceMethod = clz.getDeclaredMethod(method.getName(), method.getParameterTypes());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    private String clustersToString() {
        StringBuilder sb = new StringBuilder();
        for (Cluster<T> cluster : clusters) {
            sb.append("{protocol:").append(cluster.getUrl().getProtocol());
            List<Referer<T>> referers = cluster.getReferers();
            if (referers != null) {
                for (Referer<T> refer : referers) {
                    sb.append("[").append(refer.getUrl().toSimpleString()).append(", available:").append(refer.isAvailable()).append("]");
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private boolean proxyEquals(Object o) {
        if (o == null || this.clusters == null) {
            return false;
        }
        if (o instanceof List) {
            return this.clusters == o;
        } else {
            return o.equals(this.clusters);
        }
    }

    private Class<?> getRealReturnType(boolean asyncCall, Class<?> clazz, Method method, String methodName) {
        if (asyncCall) {
            try {
                Method m = clazz.getMethod(methodName, method.getParameterTypes());
                return m.getReturnType();
            } catch (Exception e) {
                LoggerUtil.warn("RefererInvocationHandler get real return type fail. err:" + e.getMessage());
                return method.getReturnType();
            }
        } else {
            return method.getReturnType();
        }
    }

}
