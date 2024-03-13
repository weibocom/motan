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

package com.weibo.api.motan.runtime.meta;

import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.runtime.GlobalRuntime;

import java.lang.reflect.Method;

/**
 * server end MetaService implementation.
 *
 * @author zhanglei28
 * @date 2024/3/13.
 */
public class MetaServiceProvider implements Provider<MetaService> {
    // singleton
    private static MetaServiceProvider instance = new MetaServiceProvider();    // singleton

    private MetaServiceProvider() {
    }

    public static MetaServiceProvider getInstance() {
        return instance;
    }

    @Override
    public Response call(Request request) {
        DefaultResponse response = new DefaultResponse();
        response.setValue(GlobalRuntime.getDynamicMeta());
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
        return true;
    }

    @Override
    public String desc() {
        return null;
    }

    @Override
    public URL getUrl() {
        return null;
    }

    @Override
    public Class<MetaService> getInterface() {
        return MetaService.class;
    }

    @Override
    public Method lookupMethod(String methodName, String methodDesc) {
        return null;
    }

    @Override
    public MetaService getImpl() {
        return null;
    }
}
