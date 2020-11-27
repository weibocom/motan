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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.StatisticCallback;
import com.weibo.api.motan.util.StatsUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO 可配置化策略
 * <p>
 * provider 消息处理分发：支持一定程度的自我防护
 * <p>
 * <pre>
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
 */
@SpiMeta(name = "motan")
public class DefaultProtectedStrategy implements ProviderProtectedStrategy, StatisticCallback {
    protected ConcurrentMap<String, AtomicInteger> requestCounters = new ConcurrentHashMap<>();
    protected ConcurrentMap<String, AtomicInteger> rejectCounters = new ConcurrentHashMap<>();
    protected AtomicInteger totalCounter = new AtomicInteger(0);
    protected AtomicInteger rejectCounter = new AtomicInteger(0);
    protected AtomicInteger methodCounter = new AtomicInteger(1);

    public DefaultProtectedStrategy() {
        StatsUtil.registryStatisticCallback(this);
    }

    @Override
    public void setMethodCounter(AtomicInteger methodCounter) {
        this.methodCounter = methodCounter;
    }

    @Override
    public Response call(Request request, Provider<?> provider) {
        // 支持的最大worker thread数
        int maxThread = provider.getUrl().getIntParameter(URLParamType.maxWorkerThread.getName(), URLParamType.maxWorkerThread.getIntValue());

        String requestKey = MotanFrameworkUtil.getFullMethodString(request);

        try {
            int requestCounter = incrCounter(requestKey, requestCounters);
            int totalCounter = incrTotalCounter();
            if (isAllowRequest(requestCounter, totalCounter, maxThread)) {
                return provider.call(request);
            } else {
                // reject request
                return reject(request.getInterfaceName() + "." + request.getMethodName(), requestCounter, totalCounter, maxThread, request);
            }
        } finally {
            decrTotalCounter();
            decrCounter(requestKey, requestCounters);
        }
    }

    private Response reject(String method, int requestCounter, int totalCounter, int maxThread, Request request) {
        String message = "ThreadProtectedRequestRouter reject request: request_method=" + method + " request_counter=" + requestCounter
                + " total_counter=" + totalCounter + " max_thread=" + maxThread;
        MotanServiceException exception = new MotanServiceException(message, MotanErrorMsgConstant.SERVICE_REJECT, false);
        DefaultResponse response = MotanFrameworkUtil.buildErrorResponse(request, exception);
        LoggerUtil.error(exception.getMessage());
        incrCounter(method, rejectCounters);
        rejectCounter.incrementAndGet();
        return response;
    }

    private int incrCounter(String requestKey, ConcurrentMap<String, AtomicInteger> counters) {
        AtomicInteger counter = counters.get(requestKey);
        if (counter == null) {
            counter = new AtomicInteger(0);
            counters.putIfAbsent(requestKey, counter);
            counter = counters.get(requestKey);
        }
        return counter.incrementAndGet();
    }

    private int decrCounter(String requestKey, ConcurrentMap<String, AtomicInteger> counters) {
        AtomicInteger counter = counters.get(requestKey);
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

    public boolean isAllowRequest(int requestCounter, int totalCounter, int maxThread) {

        // 方法总数为1或该方法第一次请求, 直接return true
        if (methodCounter.get() == 1 || requestCounter == 1) {
            return true;
        }

        // 不简单判断 requsetCount > (maxThread / 2) ，因为假如有2或者3个method对外提供，
        // 但是只有一个接口很大调用量，而其他接口很空闲，那么这个时候允许单个method的极限到 maxThread * 3 / 4
        if (requestCounter > (maxThread / 2) && totalCounter > (maxThread * 3 / 4)) {
            return false;
        }

        // 如果总体线程数超过 maxThread * 3 / 4个，并且对外的method比较多，那么意味着这个时候整体压力比较大，
        // 那么这个时候如果单method超过 maxThread * 1 / 4，那么reject
        return !(methodCounter.get() >= 4 && totalCounter > (maxThread * 3 / 4) && requestCounter > (maxThread / 4));
    }

    @Override
    public String statisticCallback() {
        int count = rejectCounter.getAndSet(0);
        if (count > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append("type:").append("motan").append(" ")
                    .append("name:").append("reject_request").append(" ")
                    .append("total_count:").append(totalCounter.get()).append(" ")
                    .append("reject_count:").append(count).append(" ");
            for (Map.Entry<String, AtomicInteger> entry : rejectCounters.entrySet()) {
                String key = entry.getKey();
                int cnt = entry.getValue().getAndSet(0);
                builder.append(key).append("_reject:").append(cnt).append(" ");
            }
            return builder.toString();
        } else {
            return null;
        }
    }
}
