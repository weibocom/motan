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

package com.weibo.api.motan.protocol.rpc;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * Created by zhanglei28 on 2017/9/1.
 */
public class DefaultRpcReferer<T> extends AbstractReferer<T> {
    protected Client client;
    protected EndpointFactory endpointFactory;

    public DefaultRpcReferer(Class<T> clz, URL url, URL serviceUrl) {
        super(clz, url, serviceUrl);

        endpointFactory =
                ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                        url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));

        client = endpointFactory.createClient(url);
    }

    @Override
    protected Response doCall(Request request) {
        try {
            // 为了能够实现跨group请求，需要使用server端的group。
            request.setAttachment(URLParamType.group.getName(), serviceUrl.getGroup());
            return client.request(request);
        } catch (TransportException exception) {
            throw new MotanServiceException("DefaultRpcReferer call Error: url=" + url.getUri(), exception);
        }
    }

    @Override
    protected void decrActiveCount(Request request, Response response) {
        if (response == null || !(response instanceof Future)) {
            activeRefererCount.decrementAndGet();
            return;
        }

        Future future = (Future) response;

        future.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future future) throws Exception {
                activeRefererCount.decrementAndGet();
            }
        });
    }

    @Override
    protected boolean doInit() {
        boolean result = client.open();

        return result;
    }

    @Override
    public boolean isAvailable() {
        return client.isAvailable();
    }

    @Override
    public void destroy() {
        endpointFactory.safeReleaseResource(client, url);
        LoggerUtil.info("DefaultRpcReferer destory client: url={}" + url);
    }
}
