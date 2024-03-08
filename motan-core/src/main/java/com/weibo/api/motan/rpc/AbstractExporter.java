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

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.runtime.RuntimeInfoKeys;

import java.util.HashMap;
import java.util.Map;

/**
 * abstract exporter
 *
 * @author maijunsheng
 * @version 创建时间：2013-5-21
 * AbstractExporter
 */
public abstract class AbstractExporter<T> extends AbstractNode implements Exporter<T> {
    protected Provider<T> provider;

    public AbstractExporter(Provider<T> provider, URL url) {
        super(url);
        this.provider = provider;
    }

    public Provider<T> getProvider() {
        return provider;
    }

    @Override
    public String desc() {
        return "[" + this.getClass().getSimpleName() + "] url=" + url;
    }

    /**
     * update real listened port
     *
     * @param port real listened port
     */
    protected void updateRealServerPort(int port) {
        getUrl().setPort(port);
    }

    @Override
    public Map<String, Object> getRuntimeInfo() {
        Map<String, Object> infos = new HashMap<>();
        infos.put(RuntimeInfoKeys.URL_KEY, url.toFullStr());
        infos.put(RuntimeInfoKeys.STATE_KEY, init ? "init" : "unInit");
        infos.put(RuntimeInfoKeys.PROVIDER_KEY, provider.getRuntimeInfo());
        return infos;
    }
}
