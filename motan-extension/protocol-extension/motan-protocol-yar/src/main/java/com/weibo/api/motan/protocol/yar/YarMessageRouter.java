/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.yar;

import java.util.concurrent.ConcurrentHashMap;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.ProviderProtectedMessageRouter;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;
/**
 * 
 * @Description yar message router
 * @author zhanglei
 * @date 2016-6-8
 *
 */
public class YarMessageRouter extends ProviderProtectedMessageRouter {
    protected ConcurrentHashMap<String, Provider<?>> providerMap = new ConcurrentHashMap<String, Provider<?>>();

    @Override
    public Object handle(Channel channel, Object message) {
        YarRequest yarRequest = (YarRequest) message;

        String packagerName = yarRequest.getPackagerName();
        Provider<?> provider = providerMap.get(yarRequest.getRequestPath());
        if (provider == null) {
            throw new MotanServiceException("can not find service provider. request path:" + yarRequest.getRequestPath());
        }
        Class<?> clazz = provider.getInterface();
        Request request = YarProtocolUtil.convert(yarRequest, clazz);
        Response response = call(request, provider);
        YarResponse yarResponse = YarProtocolUtil.convert(response, packagerName);
        return yarResponse;
    }

    @Override
    public void addProvider(Provider<?> provider) {
        String path = YarProtocolUtil.getYarPath(provider.getInterface(), provider.getUrl());
        Provider<?> old = providerMap.putIfAbsent(path, provider);
        if (old != null) {
            throw new MotanFrameworkException("duplicate yar provider");
        }
    }

    @Override
    public void removeProvider(Provider<?> provider) {
        String path = YarProtocolUtil.getYarPath(provider.getInterface(), provider.getUrl());
        providerMap.remove(path);
    }

}
