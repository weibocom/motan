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

package com.weibo.api.motan.mock;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.TransportException;

/**
 * 
 * @Description MockClient
 * @author zhanglei28
 * @date 2016年3月17日
 *
 */
public class MockClient implements Client {

    public static Response mockResponse;
    public static ConcurrentHashMap<URL, AtomicInteger> urlMap = new ConcurrentHashMap<URL, AtomicInteger>();
    URL url;

    public MockClient(URL url) {
        this.url = url;
        urlMap.putIfAbsent(url, new AtomicInteger());
        mockResponse = new DefaultResponse();
    }

    @Override
    public InetSocketAddress getLocalAddress() {

        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {

        return null;
    }

    @Override
    public Response request(Request request) throws TransportException {
        urlMap.get(url).incrementAndGet();
        // TODO 根据不同request 返回指定repsonse
        return mockResponse;
    }

    @Override
    public boolean open() {

        return true;
    }

    @Override
    public void close() {


    }

    @Override
    public void close(int timeout) {


    }

    @Override
    public boolean isClosed() {

        return false;
    }

    @Override
    public boolean isAvailable() {

        return true;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void heartbeat(Request request) {}


}
