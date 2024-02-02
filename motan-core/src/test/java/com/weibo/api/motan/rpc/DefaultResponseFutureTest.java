/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.exception.MotanBizException;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Created by zhanglei28 on 2017/9/11.
 */
public class DefaultResponseFutureTest {
    static URL url = new URL("motan", "localhost", 18080, "testurl");

    @Test
    public void testNormal() {
        DefaultRequest request = new DefaultRequest();
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setValue("success");
        DefaultResponseFuture response = new DefaultResponseFuture(request, 100, url);
        response.onSuccess(defaultResponse);
        Object result = response.getValue();
        assertEquals(result, defaultResponse.getValue());
        assertTrue(response.isDone());
    }

    @Test
    public void testException() {
        DefaultRequest request = new DefaultRequest();
        DefaultResponseFuture response = new DefaultResponseFuture(request, 100, url);
        Exception exception = new Exception("hello");
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setException(exception);
        response.onFailure(defaultResponse);
        try {
            response.getValue();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(response.isDone());
    }

    @Test
    public void testTimeout() {
        DefaultRequest request = new DefaultRequest();
        DefaultResponseFuture response = new DefaultResponseFuture(request, 10, url);
        try {
            response.getValue();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(response.isCancelled());
    }

    @Test
    public void testCancel() {
        DefaultRequest request = new DefaultRequest();
        DefaultResponseFuture response = new DefaultResponseFuture(request, 10, url);
        response.cancel();
        try {
            response.getValue();
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
        assertTrue(response.isCancelled());
    }

    @Test
    public void testListener() {
        DefaultRequest request = new DefaultRequest();
        DefaultResponseFuture response = new DefaultResponseFuture(request, 100, url);
        final AtomicBoolean result = new AtomicBoolean(false);
        response.addListener(future -> result.set(future.isSuccess()));
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setValue(new Object());
        response.onSuccess(defaultResponse);
        assertTrue(result.get());
        response = new DefaultResponseFuture(request, 100, url);
        response.addListener(future -> result.set(future.isSuccess()));
        response.cancel();
        result.set(true);
        response.addListener(future -> result.set(future.isSuccess()));
        Assert.assertFalse(result.get());
    }

    @Test
    public void testTraceableAndCallbackHolder() throws InterruptedException {
        DefaultResponseFuture responseFuture = new DefaultResponseFuture(new DefaultRequest(), 100, "127.0.0.1");

        // add trace info
        long receiveTime = 123L;
        String key = "ddd";
        String value = "xxx";
        responseFuture.getTraceableContext().setReceiveTime(receiveTime);
        responseFuture.getTraceableContext().addTraceInfo(key, value);

        // add attachment
        responseFuture.setAttachment("aaa", "bbb");

        // add callback
        AtomicBoolean finish = new AtomicBoolean(false);
        responseFuture.addFinishCallback(() -> finish.set(true), null);

        // pass to DefaultResponse
        responseFuture.onSuccess("success"); // trigger future done
        DefaultResponse defaultResponse = DefaultResponse.fromServerEndResponseFuture(responseFuture);
        assertEquals(receiveTime, defaultResponse.getTraceableContext().getReceiveTime());
        assertEquals(value, defaultResponse.getTraceableContext().getTraceInfo(key));
        assertEquals("bbb", defaultResponse.getAttachments().get("aaa"));

        defaultResponse.onFinish();
        Thread.sleep(10);
        assertTrue(finish.get());
    }

    @Test
    public void testServerEndException() {
        DefaultResponseFuture responseFuture = new DefaultResponseFuture(new DefaultRequest(), 100, "127.0.0.1");
        String errMsg = "biz fail";
        responseFuture.onFailure(new RuntimeException(errMsg));
        DefaultResponse defaultResponse = DefaultResponse.fromServerEndResponseFuture(responseFuture);
        assertTrue(defaultResponse.getException() instanceof MotanBizException); // check exception type
        assertTrue(defaultResponse.getException().getCause() instanceof RuntimeException);
        assertEquals(errMsg, defaultResponse.getException().getCause().getMessage());

        // test not Collections.EMPTY_MAP. response can set attachment
        defaultResponse.setAttachment("aaa", "bbb");
    }
}