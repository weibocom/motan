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

package com.weibo.api.motan.util;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultResponseFuture;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.api.motan.rpc.RpcContext;

import java.util.concurrent.ThreadPoolExecutor;

import static com.weibo.api.motan.core.StandardThreadExecutor.DEFAULT_MAX_IDLE_TIME;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author zhanglei28
 * @date 2023/10/13.
 * @since 1.2.3
 */
public class AsyncUtil {
    private static final int DEFAULT_ASYNC_TIMEOUT = 1000; // This variable has no practical meaning on the server side, because the server side does not use Future's getValue method to block waiting for results.
    private static ThreadPoolExecutor defaultCallbackExecutor = new StandardThreadExecutor(20, 200,
            DEFAULT_MAX_IDLE_TIME, MILLISECONDS, 5000,
            new DefaultThreadFactory("defaultResponseCallbackPool-", true), new ThreadPoolExecutor.DiscardPolicy());

    public static synchronized void setDefaultCallbackExecutor(ThreadPoolExecutor defaultCallbackExecutor) {
        if (defaultCallbackExecutor == null) {
            throw new MotanFrameworkException("defaultCallbackExecutor cannot be null");
        }
        ThreadPoolExecutor temp = AsyncUtil.defaultCallbackExecutor;
        AsyncUtil.defaultCallbackExecutor = defaultCallbackExecutor;
        temp.shutdown();
    }

    public static ThreadPoolExecutor getDefaultCallbackExecutor() {
        return defaultCallbackExecutor;
    }

    public static ResponseFuture createResponseFutureForServerEnd() {
        Request request = RpcContext.getContext().getRequest();
        if (request == null) {
            throw new MotanFrameworkException("can not get request from RpcContext");
        }
        return new DefaultResponseFuture(request, DEFAULT_ASYNC_TIMEOUT, request.getAttachment(URLParamType.host.getName()));
    }
}
