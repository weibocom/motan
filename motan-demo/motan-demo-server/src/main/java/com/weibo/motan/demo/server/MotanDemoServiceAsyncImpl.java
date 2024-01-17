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

package com.weibo.motan.demo.server;

import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.api.motan.util.AsyncUtil;
import com.weibo.motan.demo.service.MotanDemoServiceAsync;
import com.weibo.motan.demo.service.model.User;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MotanDemoServiceAsyncImpl implements MotanDemoServiceAsync {
    ExecutorService testExecutorService = Executors.newCachedThreadPool();

    @Override
    public String hello(String name) throws MotanServiceException {
        System.out.println(name);
        return "Hello " + name + "!";
    }

    @Override
    public User rename(User user, String name) {
        Objects.requireNonNull(user);
        System.out.println(user.getId() + " rename " + user.getName() + " to " + name);
        user.setName(name);
        return user;
    }

    @Override
    public ResponseFuture helloAsync(String name) {
        System.out.println("in async hello");
        final ResponseFuture motanResponseFuture = AsyncUtil.createResponseFutureForServerEnd();
        testExecutorService.submit(() -> motanResponseFuture.onSuccess(this.hello(name)));
        return motanResponseFuture;
    }

    @Override
    public ResponseFuture renameAsync(User user, String name) {
        System.out.println("in async rename");
        final ResponseFuture motanResponseFuture = AsyncUtil.createResponseFutureForServerEnd();
        testExecutorService.submit(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                motanResponseFuture.onSuccess(this.rename(user, name));
            } catch (Exception e) {
                motanResponseFuture.onFailure(e);
            }
        });
        return motanResponseFuture;
    }
}
