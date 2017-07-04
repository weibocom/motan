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
package com.weibo.api.motan.protocol.restful.support.proxy;

import java.lang.reflect.Method;

import org.jboss.resteasy.client.jaxrs.ProxyConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ClientInvoker;
import org.jboss.resteasy.client.jaxrs.internal.proxy.extractors.ClientContext;

import com.weibo.api.motan.protocol.restful.support.RestfulClientResponse;
import com.weibo.api.motan.protocol.restful.support.RestfulUtil;
import com.weibo.api.motan.rpc.Request;

public class RestfulClientInvoker extends ClientInvoker {

    public RestfulClientInvoker(ResteasyWebTarget parent, Class<?> declaring, Method method, ProxyConfig config) {
        super(parent, declaring, method, config);
    }

    public Object invoke(Object[] args, Request req, RestfulClientResponse resp) {
        ClientInvocation request = createRequest(args, req);
        ClientResponse response = (ClientResponse) request.invoke();
        resp.setAttachments(RestfulUtil.decodeAttachments(response.getStringHeaders()));
        resp.setHttpResponse(response);

        ClientContext context = new ClientContext(request, response, entityExtractorFactory);
        return extractor.extractEntity(context);
    }

    protected ClientInvocation createRequest(Object[] args, Request request) {
        ClientInvocation inv = super.createRequest(args);
        RestfulUtil.encodeAttachments(inv.getHeaders().getHeaders(), request.getAttachments());
        return inv;
    }

}
