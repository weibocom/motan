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

package com.weibo.api.motan.protocol;

import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * abstract protocol
 * 
 * @author maijunsheng
 * 
 */
public abstract class AbstractProtocol implements Protocol {
    protected ConcurrentHashMap<String, Exporter<?>> exporterMap = new ConcurrentHashMap<String, Exporter<?>>();

    public Map<String, Exporter<?>> getExporterMap() {
        return Collections.unmodifiableMap(exporterMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Exporter<T> export(Provider<T> provider, URL url) {
        if (url == null) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " export Error: url is null",
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        if (provider == null) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " export Error: provider is null, url=" + url,
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

        synchronized (exporterMap) {
            Exporter<T> exporter = (Exporter<T>) exporterMap.get(protocolKey);

            if (exporter != null) {
                throw new MotanFrameworkException(this.getClass().getSimpleName() + " export Error: service already exist, url=" + url,
                        MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }

            exporter = createExporter(provider, url);
            exporter.init();

            exporterMap.put(protocolKey, exporter);

            LoggerUtil.info(this.getClass().getSimpleName() + " export Success: url=" + url);

            return exporter;
        }


    }

    public <T> Referer<T> refer(Class<T> clz, URL url) {
        return refer(clz, url, url);
    }

    @Override
    public <T> Referer<T> refer(Class<T> clz, URL url, URL serviceUrl) {
        if (url == null) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " refer Error: url is null",
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }

        if (clz == null) {
            throw new MotanFrameworkException(this.getClass().getSimpleName() + " refer Error: class is null, url=" + url,
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        long start = System.currentTimeMillis();
        Referer<T> referer = createReferer(clz, url, serviceUrl);
        referer.init();

        LoggerUtil.info(this.getClass().getSimpleName() + " refer Success: url=" + url + ", cost:" + (System.currentTimeMillis() - start));

        return referer;
    }

    protected abstract <T> Exporter<T> createExporter(Provider<T> provider, URL url);

    protected abstract <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl);

    @Override
    public void destroy() {
        for (String key : exporterMap.keySet()) {
            Node node = exporterMap.remove(key);

            if (node != null) {
                try {
                    node.destroy();

                    LoggerUtil.info(this.getClass().getSimpleName() + " destroy node Success: " + node);
                } catch (Throwable t) {
                    LoggerUtil.error(this.getClass().getSimpleName() + " destroy Error", t);
                }
            }
        }
    }
}
