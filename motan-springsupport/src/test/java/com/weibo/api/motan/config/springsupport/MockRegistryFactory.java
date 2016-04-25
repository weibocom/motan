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

package com.weibo.api.motan.config.springsupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.support.AbstractRegistryFactory;
import com.weibo.api.motan.rpc.URL;


@SpiMeta(name = "mockRegistry")
public class MockRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(URL url) {
        return new Registry() {

            @Override
            public void register(URL url) {}

            @Override
            public void unregister(URL url) {}

            @Override
            public void available(URL url) {

            }

            @Override
            public void unavailable(URL url) {

            }

            @Override
            public Collection<URL> getRegisteredServiceUrls() {
                return null;
            }

            @Override
            public void subscribe(URL url, NotifyListener listener) {}

            @Override
            public void unsubscribe(URL url, NotifyListener listener) {}

            @Override
            public List<URL> discover(URL url) {
                return new ArrayList<URL>();
            }

            @Override
            public URL getUrl() {
                return new URL("mockRegistry", "127.0.0.1", 0, "");
            }
        };
    }

}
