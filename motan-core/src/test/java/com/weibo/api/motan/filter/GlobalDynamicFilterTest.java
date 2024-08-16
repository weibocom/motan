/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

package com.weibo.api.motan.filter;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.mock.MockProvider;
import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.rpc.*;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanglei28
 * @date 2024/7/10.
 */
public class GlobalDynamicFilterTest extends BaseTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testCondition() {
        // empty condition
        validateCondition(null, GlobalDynamicFilter.Condition.SIDE_SERVER, null, null, null, true);
        validateCondition(null, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, null, null, true);
        JSONObject condition = new JSONObject();
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, null, null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, null, null, true);

        // test side match
        condition.put("side", GlobalDynamicFilter.Condition.SIDE_SERVER);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, null, null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, null, null, false);

        // test ip match
        condition = new JSONObject();
        condition.put("ip", "10.123");
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, "10.123.12.12", null, null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.123.1.123", null, null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, "10.12.1.123", null, null, false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.32.12.123", null, null, false);

        // test method match
        condition = new JSONObject();
        condition.put("method", "testMethod(.)*");
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, "testMethod3", null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, "testMethodHello", null, true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, "method", null, false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, "methodHello", null, false);

        // test service match
        condition = new JSONObject();
        condition.put("service", "test(.)*Service");
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, null, "testService", true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, null, "testHelloService", true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, null, null, "testServiceHello", false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, null, null, "testHelloServiceHello", false);

        // combined condition matching
        condition = new JSONObject();
        condition.put("side", GlobalDynamicFilter.Condition.SIDE_CLIENT);
        condition.put("ip", "10.123");
        condition.put("method", "testMethod(.)*");
        condition.put("service", "test(.)*Service");
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, "10.123.12.12", "testMethod3", "testService", false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.123.1.123", "testMethodHello", "testHelloService", true);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_SERVER, "10.123.1.123", "testMethodHello", "testHelloService", false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.23.1.123", "testMethodHello", "testHelloService", false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.123.1.123", "testHello", "testHelloService", false);
        validateCondition(condition, GlobalDynamicFilter.Condition.SIDE_CLIENT, "10.123.1.123", "testMethodHello", "testHelloServiceHello", false);
    }

    private void validateCondition(JSONObject condition, String side, String ip, String method, String service, boolean isMatch) {
        String conditionString = null;
        if (condition != null) {
            conditionString = condition.toJSONString();
        }
        GlobalDynamicFilter.ConditionFilter conditionFilter = new GlobalDynamicFilter.ConditionFilter("testInnerFilter", conditionString);
        GlobalDynamicFilter.setDynamicFilter(conditionFilter);
        DefaultRequest request = new DefaultRequest();
        request.setMethodName(method);
        request.setInterfaceName(service);
        Caller<?> caller;
        URL url = new URL("motan2", ip, 8002, service);
        if (GlobalDynamicFilter.Condition.SIDE_SERVER.equals(side)) {
            Random random = new Random();
            if (random.nextBoolean()) { // randomly use different attachment key to set the remote IP
                request.setAttachment(MotanConstants.X_FORWARDED_FOR, ip);
            } else {
                request.setAttachment(URLParamType.host.getName(), ip);
            }
            url.setHost("127.0.0.1"); // set host to the local ip on the server side
            caller = new MockProvider<>(url, new DefaultResponse());
        } else {
            caller = new MockReferer<>(url, new DefaultResponse());
        }

        TestInnerFilter.countMap.clear();
        GlobalDynamicFilter globalDynamicFilter = new GlobalDynamicFilter();
        int times = 3;
        for (int i = 0; i < times; i++) {
            globalDynamicFilter.filter(caller, request);
        }
        if (isMatch) {
            assertEquals(times, TestInnerFilter.countMap.get(url).get());
        } else {
            assertNull(TestInnerFilter.countMap.get(url));
        }
    }

    public void testConcurrentFilter() throws InterruptedException {
        JSONObject condition = new JSONObject();
        condition.put("ip", "10.123");
        condition.put("method", "testMethod(.)*");
        condition.put("service", "test(.)*Service");
        GlobalDynamicFilter.ConditionFilter conditionFilter = new GlobalDynamicFilter.ConditionFilter("testInnerFilter", condition.toJSONString());
        GlobalDynamicFilter.setDynamicFilter(conditionFilter);

        int threads = 10;
        int batchSize = 30;
        AtomicInteger successCount = new AtomicInteger();
        CountDownLatch countDownLatch = new CountDownLatch(threads * 4); // threads * four types

        // client side match
        concurrentFilter(GlobalDynamicFilter.Condition.SIDE_CLIENT, threads, batchSize, countDownLatch, true, successCount);
        // server side match
        concurrentFilter(GlobalDynamicFilter.Condition.SIDE_SERVER, threads, batchSize, countDownLatch, true, successCount);
        // client side not match
        concurrentFilter(GlobalDynamicFilter.Condition.SIDE_CLIENT, threads, batchSize, countDownLatch, false, successCount);
        // server side not match
        concurrentFilter(GlobalDynamicFilter.Condition.SIDE_SERVER, threads, batchSize, countDownLatch, false, successCount);

        Thread.sleep(200);
        assertEquals(threads * 4, successCount.get());
    }


    private void concurrentFilter(String side, int threads, int batchSize, CountDownLatch countDownLatch, boolean isMatch, AtomicInteger successCount) {
        final DefaultRequest request = new DefaultRequest();
        request.setMethodName("testMethodHello");
        if (isMatch) {
            request.setInterfaceName("testHelloService");
        } else {
            request.setInterfaceName("testMockService1");
        }

        for (int i = 0; i < threads; i++) {
            URL url = new URL("motan2", "10.123.12.12", 8002, "testService" + side + isMatch + i);
            final Caller<?> caller;
            if (GlobalDynamicFilter.Condition.SIDE_CLIENT.equals(side)) {
                caller = new MockReferer<>(url, new DefaultResponse());
            } else {
                caller = new MockProvider<>(url, new DefaultResponse());
            }
            new Thread(() -> {
                try {
                    GlobalDynamicFilter globalDynamicFilter = new GlobalDynamicFilter();
                    countDownLatch.await();
                    for (int j = 0; j < batchSize; j++) {
                        globalDynamicFilter.filter(caller, request);
                    }
                    if (isMatch) {
                        assertEquals(batchSize, TestInnerFilter.countMap.get(url).get());
                    } else {
                        assertNull(TestInnerFilter.countMap.get(url));
                    }
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
            countDownLatch.countDown();
        }
    }

    public void testFilterChange() {
        // enable dynamic filter
        GlobalDynamicFilter.ConditionFilter conditionFilter = new GlobalDynamicFilter.ConditionFilter("testInnerFilter", null);
        GlobalDynamicFilter.setDynamicFilter(conditionFilter);
        assertEquals(conditionFilter.toJson(), GlobalDynamicFilter.getDynamicFilter().toJson());
        GlobalDynamicFilter globalDynamicFilter = new GlobalDynamicFilter();
        DefaultRequest request = new DefaultRequest();
        request.setMethodName("testMethod");
        request.setInterfaceName("testService");
        URL url = new URL("motan2", "localhost", 8002, "testService");
        globalDynamicFilter.filter(new MockProvider<>(url, new DefaultResponse()), request);
        assertEquals(1, TestInnerFilter.countMap.get(url).get());
        globalDynamicFilter.filter(new MockReferer<>(url, new DefaultResponse()), request);
        assertEquals(2, TestInnerFilter.countMap.get(url).get());

        // disable dynamic filter
        GlobalDynamicFilter.setDynamicFilter(null);
        assertNull(GlobalDynamicFilter.getDynamicFilter());
        TestInnerFilter.countMap.clear();
        globalDynamicFilter.filter(new MockProvider<>(url, new DefaultResponse()), request);
        globalDynamicFilter.filter(new MockReferer<>(url, new DefaultResponse()), request);
        assertNull(TestInnerFilter.countMap.get(url));
    }

    @SpiMeta(name = "testInnerFilter")
    public static class TestInnerFilter implements Filter {
        public final static ConcurrentHashMap<URL, AtomicInteger> countMap = new ConcurrentHashMap<>();

        @Override
        public Response filter(Caller<?> caller, Request request) {
            AtomicInteger count = countMap.get(caller.getUrl());
            if (count == null) {
                countMap.putIfAbsent(caller.getUrl(), new AtomicInteger(0));
                count = countMap.get(caller.getUrl());
            }
            count.incrementAndGet();
            return caller.call(request);
        }
    }
}