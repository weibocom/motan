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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * @param <T>
 * @author maijunsheng
 */
public class RefererInvocationHandler<T> extends AbstractRefererHandler<T> implements InvocationHandler {

    public RefererInvocationHandler(Class<T> clz, List<Caller<T>> callers) {
        this.clz = clz;
        this.callers = callers;
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
            if ("hashCode".equals(method.getName())) {
                return this.callers == null ? 0 : this.callers.hashCode();
            }
            throw new MotanServiceException("can not invoke local method:" + method.getName());
        }
        DefaultRequest request = new DefaultRequest();
        boolean async = fillDefaultRequest(request, method, args);
        return invokeRequest(request, getRealReturnType(async, this.clz, method, request.getMethodName()), async);
    }

    private String clustersToString() {
        StringBuilder sb = new StringBuilder();
        for (Caller<T> caller : callers) {
            sb.append("{protocol:").append(caller.getUrl().getProtocol()).append(",");
            sb.append(caller.toString()).append("}").append(",");
        }
        if (!callers.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private boolean proxyEquals(Object o) {
        if (o == null || this.callers == null) {
            return false;
        }
        if (o instanceof List) {
            return this.callers == o;
        } else {
            return o.equals(this.callers);
        }
    }
}
