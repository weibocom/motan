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

package com.weibo.api.motan.transport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * TODO 可配置化策略
 * 
 * provider 消息处理分发：支持一定程度的自我防护
 * 
 * <pre>
 * 
 * 
 * 		1) 如果接口只有一个方法，那么直接return true 
 * 		2) 如果接口有多个方法，那么如果单个method超过 maxThread / 2 && totalCount >  (maxThread * 3 / 4)，那么return false;
 * 		3) 如果接口有多个方法(4个)，同时总的请求数超过 maxThread * 3 / 4，同时该method的请求数超过 maxThead * 1 / 4， 那么return false
 * 		4) 其他场景return true
 * 
 * </pre>
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-7
 * 
 */
public class ProviderProtectedMessageRouter extends ProviderMessageRouter {
    protected ConcurrentMap<String, AtomicInteger> requestCounters = new ConcurrentHashMap<String, AtomicInteger>();
    protected AtomicInteger totalCounter = new AtomicInteger(0);

    protected Object lock = new Object();

    public ProviderProtectedMessageRouter() {
        super();
    }

    public ProviderProtectedMessageRouter(Provider<?> provider) {
        super(provider);
    }

    @Override
    protected Response call(Request request, Provider<?> provider) {
        // 支持的最大worker thread数
        int maxThread =
                provider.getUrl().getIntParameter(URLParamType.maxWorkerThread.getName(), URLParamType.maxWorkerThread.getIntValue());

        String requestKey = MotanFrameworkUtil.getFullMethodString(request);

        try {
            int requestCounter = 0, totalCounter = 0;
            synchronized (lock) {
                requestCounter = incrRequestCounter(requestKey);
                totalCounter = incrTotalCounter();
            }

            if (isAllowRequest(requestCounter, totalCounter, maxThread, request)) {
                return super.call(request, provider);
            } else {
                // reject request
                return reject(request.getInterfaceName() + "." + request.getMethodName(), requestCounter, totalCounter, maxThread);
            }

        } finally {
            synchronized (lock) {
                decrTotalCounter();
                decrRequestCounter(requestKey);
            }
        }
    }

    private Response reject(String method, int requestCounter, int totalCounter, int maxThread) {
        DefaultResponse response = new DefaultResponse();
        MotanServiceException exception =
                new MotanServiceException("ThreadProtectedRequestRouter reject request: request_counter=" + requestCounter
                        + " total_counter=" + totalCounter + " max_thread=" + maxThread, MotanErrorMsgConstant.SERVICE_REJECT);
        exception.setStackTrace(new StackTraceElement[0]);
        response.setException(exception);
        LoggerUtil.error("ThreadProtectedRequestRouter reject request: request_method=" + method + " request_counter=" + requestCounter
                + " =" + totalCounter + " max_thread=" + maxThread);
        return response;
    }

    private int incrRequestCounter(String requestKey) {
        AtomicInteger counter = requestCounters.get(requestKey);

        if (counter == null) {
            counter = new AtomicInteger(0);
            requestCounters.putIfAbsent(requestKey, counter);
            counter = requestCounters.get(requestKey);
        }

        return counter.incrementAndGet();
    }

    private int decrRequestCounter(String requestKey) {
        AtomicInteger counter = requestCounters.get(requestKey);

        if (counter == null) {
            return 0;
        }
        return counter.decrementAndGet();
    }

    private int incrTotalCounter() {
        return totalCounter.incrementAndGet();
    }

    private int decrTotalCounter() {
        return totalCounter.decrementAndGet();
    }

    protected boolean isAllowRequest(int requestCounter, int totalCounter, int maxThread, Request request) {
        if (methodCounter.get() == 1) {
            return true;
        }

        // 该方法第一次请求，直接return true
        if (requestCounter == 1) {
            return true;
        }

        // 不简单判断 requsetCount > (maxThread / 2) ，因为假如有2或者3个method对外提供，
        // 但是只有一个接口很大调用量，而其他接口很空闲，那么这个时候允许单个method的极限到 maxThread * 3 / 4
        if (requestCounter > (maxThread / 2) && totalCounter > (maxThread * 3 / 4)) {
            return false;
        }

        // 如果总体线程数超过 maxThread * 3 / 4个，并且对外的method比较多，那么意味着这个时候整体压力比较大，
        // 那么这个时候如果单method超过 maxThread * 1 / 4，那么reject
        return !(methodCounter.get() >= 4 && totalCounter > (maxThread * 3 / 4) && requestCounter > (maxThread * 1 / 4));

    }
}
