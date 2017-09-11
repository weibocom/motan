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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.client.jaxrs.ProxyConfig;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.util.IsHttpMethod;

public class RestfulProxyBuilder<T> {
    private final Class<T> iface;
    private final ResteasyWebTarget webTarget;
    private ClassLoader loader = Thread.currentThread().getContextClassLoader();
    private MediaType serverConsumes;
    private MediaType serverProduces;

    public static <T> RestfulProxyBuilder<T> builder(Class<T> iface, WebTarget webTarget) {
        return new RestfulProxyBuilder<T>(iface, (ResteasyWebTarget) webTarget);
    }

    public static Map<Method, RestfulClientInvoker> proxy(final Class<?> iface, WebTarget base,
            final ProxyConfig config) {
        if (iface.isAnnotationPresent(Path.class)) {
            Path path = iface.getAnnotation(Path.class);
            if (!path.value().equals("") && !path.value().equals("/")) {
                base = base.path(path.value());
            }
        }

        HashMap<Method, RestfulClientInvoker> methodMap = new HashMap<Method, RestfulClientInvoker>();
        for (Method method : iface.getMethods()) {
            RestfulClientInvoker invoker = createClientInvoker(iface, method, (ResteasyWebTarget) base, config);
            methodMap.put(method, invoker);
        }

        return methodMap;
    }

    private static <T> RestfulClientInvoker createClientInvoker(Class<T> clazz, Method method, ResteasyWebTarget base,
            ProxyConfig config) {
        Set<String> httpMethods = IsHttpMethod.getHttpMethods(method);
        if (httpMethods == null || httpMethods.size() != 1) {
            throw new RuntimeException(
                    "You must use at least one, but no more than one http method annotation on: " + method.toString());
        }

        RestfulClientInvoker invoker = new RestfulClientInvoker(base, clazz, method, config);
        invoker.setHttpMethod(httpMethods.iterator().next());
        return invoker;
    }

    private RestfulProxyBuilder(Class<T> iface, ResteasyWebTarget webTarget) {
        this.iface = iface;
        this.webTarget = webTarget;
    }

    public RestfulProxyBuilder<T> classloader(ClassLoader cl) {
        this.loader = cl;
        return this;
    }

    public RestfulProxyBuilder<T> defaultProduces(MediaType type) {
        this.serverProduces = type;
        return this;
    }

    public RestfulProxyBuilder<T> defaultConsumes(MediaType type) {
        this.serverConsumes = type;
        return this;
    }

    public RestfulProxyBuilder<T> defaultProduces(String type) {
        this.serverProduces = MediaType.valueOf(type);
        return this;
    }

    public RestfulProxyBuilder<T> defaultConsumes(String type) {
        this.serverConsumes = MediaType.valueOf(type);
        return this;
    }

    public Map<Method, RestfulClientInvoker> build() {
        return proxy(iface, webTarget, new ProxyConfig(loader, serverConsumes, serverProduces));
    }

}
