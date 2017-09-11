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
package com.weibo.api.motan.protocol.restful.support;

import javax.ws.rs.InternalServerErrorException;

import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.MethodInjectorImpl;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.ReflectUtil;

public class RestfulInjectorFactory extends InjectorFactoryImpl {

    @Override
    public MethodInjector createMethodInjector(ResourceLocator method, ResteasyProviderFactory factory) {
        return new RestfulMethodInjector(method, factory);
    }

    private static class RestfulMethodInjector extends MethodInjectorImpl {

        public RestfulMethodInjector(ResourceLocator resourceMethod, ResteasyProviderFactory factory) {
            super(resourceMethod, factory);
        }

        @Override
        public Object invoke(HttpRequest request, HttpResponse httpResponse, Object resource)
                throws Failure, ApplicationException {
            if (!Provider.class.isInstance(resource)) {
                return super.invoke(request, httpResponse, resource);
            }

            Object[] args = injectArguments(request, httpResponse);

            RestfulContainerRequest req = new RestfulContainerRequest();
            req.setInterfaceName(method.getResourceClass().getClazz().getName());
            req.setMethodName(method.getMethod().getName());
            req.setParamtersDesc(ReflectUtil.getMethodParamDesc(method.getMethod()));
            req.setArguments(args);

            req.setHttpRequest(request);
            req.setAttachments(RestfulUtil.decodeAttachments(request.getMutableHeaders()));

            try {
                Response resp = Provider.class.cast(resource).call(req);

                RestfulUtil.encodeAttachments(httpResponse.getOutputHeaders(), resp.getAttachments());

                return resp.getValue();
            } catch (Exception e) {
                if (e != null && e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }

                throw new InternalServerErrorException("provider call process error:" + e.getMessage(), e);
            }
        }

    }

}
