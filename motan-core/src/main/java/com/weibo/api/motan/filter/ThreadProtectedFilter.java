/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.filter;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @Description threadProtected filter, only for server end. when totalcount greater than
 *              totalLimit, the single method request count can not over methodLimit.
 * @author zhanglei
 * @date Nov 15, 2016
 *
 */
@SpiMeta(name = "threadProtected")
public class ThreadProtectedFilter implements InitializableFilter {
    protected static ConcurrentHashMap<String, AtomicInteger> portTotalMap = new ConcurrentHashMap<String, AtomicInteger>();
    protected ConcurrentHashMap<String, AtomicInteger> methodMap = new ConcurrentHashMap<String, AtomicInteger>();
    protected AtomicInteger totalCount;
    protected int maxThread;
    protected int totalLimit;
    protected int methodLimit;
    protected boolean isProvider = false;

    @Override
    public Response filter(Caller<?> caller, Request request) {
        if (isProvider) {
            String requestKey = MotanFrameworkUtil.getFullMethodString(request);
            AtomicInteger methodCount = methodMap.get(requestKey);
            if (methodCount == null) {
                methodMap.putIfAbsent(requestKey, new AtomicInteger());
                methodCount = methodMap.get(requestKey);
            }
            try {
                int tCount = totalCount.incrementAndGet();
                int mCount = methodCount.incrementAndGet();
                if (tCount > totalLimit && mCount > methodLimit) {
                    return reject(request.getInterfaceName() + "." + request.getMethodName(), mCount, tCount);
                }
                return caller.call(request);
            } finally {
                totalCount.decrementAndGet();
                methodCount.decrementAndGet();
            }
        } else {
            return caller.call(request);
        }

    }

    private Response reject(String method, int requestCounter, int totalCounter) {
        DefaultResponse response = new DefaultResponse();
        MotanServiceException exception =
                new MotanServiceException("ThreadProtectedFilter reject request: request_counter=" + requestCounter + " total_counter="
                        + totalCounter + " max_thread=" + maxThread, MotanErrorMsgConstant.SERVICE_REJECT);
        exception.setStackTrace(new StackTraceElement[0]);
        response.setException(exception);
        LoggerUtil.error("ThreadProtectedFilter reject request: request_method=" + method + " request_counter=" + requestCounter + " ="
                + totalCounter + " max_thread=" + maxThread);
        return response;
    }


    /**
     * 默认策略当接口线程池占用达到3/4或者空闲小于150时，限制单个方法请求不能超过总线程数的1/2 需要自定义方法并发限制可以通过actives参数配置
     */
    @Override
    public void init(Caller<?> caller) {
        if (caller instanceof Provider) {
            String port = String.valueOf(caller.getUrl().getPort());
            totalCount = portTotalMap.get(port);
            if (totalCount == null) {
                portTotalMap.putIfAbsent(port, new AtomicInteger());
                totalCount = portTotalMap.get(port);
            }
            maxThread = caller.getUrl().getIntParameter(URLParamType.maxWorkerThread.getName(), URLParamType.maxWorkerThread.getIntValue());
            totalLimit = maxThread > 600 ? maxThread - 150 : maxThread * 3 / 4;
            int active = caller.getUrl().getIntParameter(URLParamType.actives.getName(), URLParamType.actives.getIntValue());
            if (active > 0) {
                methodLimit = active;
            } else {
                methodLimit = maxThread / 2;
            }
            isProvider = true;
        }
    }

}
