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

package com.weibo.api.motan.mock;

import com.weibo.api.motan.rpc.*;

import java.lang.reflect.Method;

/**
 * @author zhanglei28
 * @date 2024/7/17.
 */
public class MockProvider<T> implements Provider<T> {
    public URL url;
    public boolean available;
    public DefaultResponse response;

    public MockProvider(URL url, DefaultResponse response) {
        this.url = url;
        this.response = response;
    }

    @Override
    public Response call(Request request) {
        return response;
    }

    @Override
    public void init() {
    }

    @Override
    public void destroy() {
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String desc() {
        return null;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Class<T> getInterface() {
        return null;
    }

    @Override
    public Method lookupMethod(String methodName, String methodDesc) {
        return null;
    }

    @Override
    public T getImpl() {
        return null;
    }
}
