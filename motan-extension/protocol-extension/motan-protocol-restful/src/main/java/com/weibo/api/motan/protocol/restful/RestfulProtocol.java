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
package com.weibo.api.motan.protocol.restful;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.protocol.restful.support.ProviderResource;
import com.weibo.api.motan.protocol.restful.support.RestfulClientResponse;
import com.weibo.api.motan.protocol.restful.support.RestfulUtil;
import com.weibo.api.motan.protocol.restful.support.proxy.RestfulClientInvoker;
import com.weibo.api.motan.protocol.restful.support.proxy.RestfulProxyBuilder;
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
import com.weibo.api.motan.util.ReflectUtil;

/**
 * restful协议
 *
 * @author zhouhaocheng
 *
 */
@SpiMeta(name = "restful")
public class RestfulProtocol extends AbstractProtocol {

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        return new RestfulExporter<T>(provider, url);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        return new RestfulReferer<T>(clz, serviceUrl);
    }

    private class RestfulExporter<T> extends AbstractExporter<T> {
        private RestServer server;
        private EndpointFactory endpointFactory;

        public RestfulExporter(Provider<T> provider, URL url) {
            super(provider, url);

            endpointFactory = ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                    url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));
            server = endpointFactory.createServer(url);
        }

        @Override
        public void unexport() {
            server.getDeployment().getRegistry().removeRegistrations(provider.getInterface());

            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

            @SuppressWarnings("unchecked")
            Exporter<T> exporter = (Exporter<T>) exporterMap.remove(protocolKey);

            if (exporter != null) {
                exporter.destroy();
            }

            LoggerUtil.info("RestfulExporter unexport Success: url={}", url);
        }

        @Override
        public void destroy() {
            endpointFactory.safeReleaseResource(server, url);

            LoggerUtil.info("RestfulExporter destory Success: url={}", url);
        }

        @Override
        protected boolean doInit() {
            server.getDeployment().getRegistry().addResourceFactory(new ProviderResource<T>(provider));
            return true;
        }
    }

    private static class RestfulReferer<T> extends AbstractReferer<T> {
        private ResteasyWebTarget target;
        private EndpointFactory endpointFactory;

        private Map<Method, RestfulClientInvoker> delegate;

        public RestfulReferer(Class<T> clz, URL url) {
            super(clz, url);

            endpointFactory = ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                    url.getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));
            target = endpointFactory.createClient(url);
        }

        @Override
        public void destroy() {
            endpointFactory.safeReleaseResource(target, url);

            LoggerUtil.info("RestfulReferer destory client: url={}" + url);
        }

        @Override
        protected Response doCall(Request request) {
            RestfulClientResponse response = new RestfulClientResponse(request.getRequestId());
            try {
                Method method = getInterface().getMethod(request.getMethodName(),
                        ReflectUtil.forNames(request.getParamtersDesc()));

                Object value = delegate.get(method).invoke(request.getArguments(), request, response);

                response.setValue(value);
            } catch (Exception e) {
                Exception cause = RestfulUtil.getCause(response.getHttpResponse());
                response.setException(cause != null ? cause : e);
            }

            return response;
        }

        @Override
        protected boolean doInit() {
            // 此处不使用target.proxy(getInterface()),否则须使用filter方式来完成request/response中attachment的传递
            delegate = RestfulProxyBuilder.builder(getInterface(), target).build();

            return true;
        }

    }

}
