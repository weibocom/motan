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

package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.protocol.rpc.DefaultRpcExporter;
import com.weibo.api.motan.protocol.rpc.DefaultRpcReferer;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.transport.TransportException;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

import static com.weibo.api.motan.common.MotanConstants.M2_PROXY_PROTOCOL;

/**
 * Created by zhanglei28 on 2017/4/27.
 * 协议默认配置codec
 */
@SpiMeta(name = "motan2")
public class MotanV2Protocol extends AbstractProtocol {

    public static final String DEFAULT_CODEC = "motan2";
    private ConcurrentHashMap<String, ProviderMessageRouter> ipPort2RequestRouter = new ConcurrentHashMap<String, ProviderMessageRouter>();

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        setDefaultCodec(url);
        return new DefaultRpcExporter<T>(provider, url, this.ipPort2RequestRouter, this.exporterMap);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        setDefaultCodec(url);
        return new V2RpcReferer<T>(clz, url, serviceUrl);
    }

    private void setDefaultCodec(URL url) {
        String codec = url.getParameter(URLParamType.codec.getName());
        if (StringUtils.isBlank(codec)) {
            url.getParameters().put(URLParamType.codec.getName(), DEFAULT_CODEC);
        }
    }

    /**
     * rpc referer
     *
     * @param <T>
     * @author maijunsheng
     */
    class V2RpcReferer<T> extends DefaultRpcReferer<T> {

        public V2RpcReferer(Class<T> clz, URL url, URL serviceUrl) {
            super(clz, url, serviceUrl);
        }

        @Override
        protected Response doCall(Request request) {
            try {
                // use server end group
                request.setAttachment(URLParamType.group.getName(), serviceUrl.getGroup());
                request.setAttachment(M2_PROXY_PROTOCOL, this.url.getProtocol()); // add proxy protocol for request agent
                return client.request(request);
            } catch (TransportException exception) {
                throw new MotanServiceException("DefaultRpcReferer call Error: url=" + url.getUri(), exception);
            }
        }

    }
}
