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

package com.weibo.api.motan.protocol.injvm;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.AbstractExporter;
import com.weibo.api.motan.rpc.AbstractReferer;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * JVM 节点内部的调用
 * 
 * <pre>
 * 		1) provider 和 referer 相对应 
 * 		2) provider 需要在被consumer refer 之前需要 export
 * </pre>
 * 
 * @author maijunsheng
 * 
 */
@SpiMeta(name = "injvm")
public class InjvmProtocol extends AbstractProtocol {

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        return new InJvmExporter<T>(provider, url);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        return new InjvmReferer<T>(clz, url, serviceUrl);
    }

    /**
     * injvm provider
     * 
     * @author maijunsheng
     * 
     * @param <T>
     */
    class InJvmExporter<T> extends AbstractExporter<T> {
        public InJvmExporter(Provider<T> provider, URL url) {
            super(provider, url);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void unexport() {
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

            Exporter<T> exporter = (Exporter<T>) exporterMap.remove(protocolKey);

            if (exporter != null) {
                exporter.destroy();
            }

            LoggerUtil.info("InJvmExporter unexport Success: url=" + url);
        }

        @Override
        protected boolean doInit() {
            return true;
        }

        @Override
        public void destroy() {}
    }

    /**
     * injvm consumer
     * 
     * @author maijunsheng
     * 
     * @param <T>
     */
    class InjvmReferer<T> extends AbstractReferer<T> {
        private Exporter<T> exporter;

        public InjvmReferer(Class<T> clz, URL url, URL serviceUrl) {
            super(clz, url, serviceUrl);
        }

        @Override
        protected Response doCall(Request request) {
            if (exporter == null) {
                throw new MotanServiceException("InjvmReferer call Error: provider not exist, url=" + url.getUri(),
                        MotanErrorMsgConstant.SERVICE_UNFOUND);
            }

            return exporter.getProvider().call(request);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean doInit() {
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

            exporter = (Exporter<T>) exporterMap.get(protocolKey);

            if (exporter == null) {
                LoggerUtil.error("InjvmReferer init Error: provider not exist, url=" + url);
                return false;
            }

            return true;
        }

        @Override
        public void destroy() {}
    }
}
