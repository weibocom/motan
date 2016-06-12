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

package com.weibo.api.motan.registry.support;

import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by axb on 16/6/12.
 */
public class DirectRegistry extends AbstractRegistry {

    ConcurrentHashMap<URL, Object> subscribeUrls = new ConcurrentHashMap();

    public DirectRegistry(URL url) {
        super(url);
    }

    @Override
    protected void doRegister(URL url) {
        // do nothing
    }

    @Override
    protected void doUnregister(URL url) {
        // do nothing
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        subscribeUrls.putIfAbsent(url, 1);
        listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        subscribeUrls.remove(url);
        listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected List<URL> doDiscover(URL subscribeUrl) {
        return createSubscribeUrl();
    }

    private List<URL> createSubscribeUrl() {
        URL url = this.getUrl();
        List result = new ArrayList();
        for (URL subscribeUrl : subscribeUrls.keySet()) {
            URL tmp = subscribeUrl.createCopy();
            tmp.setHost(url.getHost());
            tmp.setPort(url.getPort());
            result.add(tmp);
        }
        return result;
    }

    @Override
    protected void doAvailable(URL url) {
        // do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        // do nothing
    }
}
