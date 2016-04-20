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

import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-15
 */
public class MockReferer<T> implements Referer<T> {
    public Class<T> clz = null;
    public int active = 0;
    public boolean available = true;
    public String desc = this.getClass().getSimpleName();
    public URL url = null;
    public URL serviceUrl = null;

    public MockReferer() {

    }

    public MockReferer(URL serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Override
    public Class<T> getInterface() {
        return clz;
    }

    @Override
    public Response call(Request request) {
        return null;
    }

    @Override
    public void init() {}

    @Override
    public void destroy() {}

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String desc() {
        return desc;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int activeRefererCount() {
        return active;
    }

    @Override
    public URL getServiceUrl() {
        return serviceUrl;
    }

}
