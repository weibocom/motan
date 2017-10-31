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

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

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
        Assert.assertEquals(result, defaultResponse.getValue());
        Assert.assertTrue(response.isDone());
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
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
        Assert.assertTrue(response.isDone());
    }

    @Test
    public void testTimeout() {
        DefaultRequest request = new DefaultRequest();

        DefaultResponseFuture response = new DefaultResponseFuture(request, 10, url);

        try {
            response.getValue();
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        Assert.assertTrue(response.isCancelled());
    }

    @Test
    public void testCancel() {
        DefaultRequest request = new DefaultRequest();

        DefaultResponseFuture response = new DefaultResponseFuture(request, 10, url);
        response.cancel();

        try {
            response.getValue();
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

        Assert.assertTrue(response.isCancelled());
    }

    @Test
    public void testListener() {
        DefaultRequest request = new DefaultRequest();

        DefaultResponseFuture response = new DefaultResponseFuture(request, 100, url);

        final AtomicBoolean result = new AtomicBoolean(false);

        response.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    result.set(true);
                } else {
                    result.set(false);
                }
            }
        });

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setValue(new Object());
        response.onSuccess(defaultResponse);

        Assert.assertTrue(result.get());

        response = new DefaultResponseFuture(request, 100, url);

        response.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    result.set(true);
                } else {
                    result.set(false);
                }
            }
        });

        response.cancel();

        result.set(true);

        response.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                if (future.isSuccess()) {
                    result.set(true);
                } else {
                    result.set(false);
                }
            }
        });

        Assert.assertFalse(result.get());

    }
}