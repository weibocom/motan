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

package com.weibo.api.motan.transport.netty;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Future;
import com.weibo.api.motan.rpc.FutureListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Server;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
public class NettyResponseFutureTest extends TestCase {
    private static NettyClient client = new NettyClient(new URL("motan", "localhost", 18080, Server.class.getName()));

    @Test
    public void testNormal() {
        DefaultRequest request = new DefaultRequest();

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setValue("success");

        NettyResponseFuture response = new NettyResponseFuture(request, 100, client);

        response.onSuccess(defaultResponse);

        Object result = response.getValue();
        Assert.assertEquals(result, defaultResponse.getValue());
        Assert.assertTrue(response.isDone());
    }

    @Test
    public void testException() {
        DefaultRequest request = new DefaultRequest();

        NettyResponseFuture response = new NettyResponseFuture(request, 100, client);
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

        NettyResponseFuture response = new NettyResponseFuture(request, 10, client);

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

        NettyResponseFuture response = new NettyResponseFuture(request, 10, client);
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

        NettyResponseFuture response = new NettyResponseFuture(request, 100, client);

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

        response = new NettyResponseFuture(request, 100, client);

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

    public static void main(String[] args) throws Exception {
        final NettyResponseFuture future = new NettyResponseFuture(null, 1100, client);

        new Thread() {
            public void run() {
                try {
                    System.out.println("start get value");
                    Object result = future.getValue();
                    System.out.println("finish get value: " + result);
                } catch (Exception e) {
                    System.out.println("throwable get value: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }.start();

        Thread.sleep(1000);

        System.out.println("onComplete:" + future.getState());
        //
        // future.onComplete("hello");
        // System.out.println("onComplete:" + future.state);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setException(new Exception("exception ~~~~"));

        future.onFailure(defaultResponse);
        System.out.println("onError:" + future.getState());

        Thread.sleep(1000);
        System.out.println("finish");
    }
}
