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

package com.weibo.api.motan.registry.weibomesh;

import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanglei28
 * @date 2021/8/5.
 */
public class TestNotifyListener implements NotifyListener {
    public List<URL> urls;
    public AtomicInteger count = new AtomicInteger(0);

    @Override
    public void notify(URL registryUrl, List<URL> urls) {
        this.urls = urls;
        count.incrementAndGet();
    }
}