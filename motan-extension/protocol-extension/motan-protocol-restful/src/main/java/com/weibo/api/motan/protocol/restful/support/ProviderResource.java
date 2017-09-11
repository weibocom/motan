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

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.ResourceBuilder;
import org.jboss.resteasy.spi.metadata.ResourceClass;

import com.weibo.api.motan.rpc.Provider;

/**
 * 基于provider的resource
 *
 * @author zhouhaocheng
 *
 * @param <T>
 */
public class ProviderResource<T> implements ResourceFactory {
    private final Provider<T> provider;
    private final ResourceClass resourceClass;

    public ProviderResource(Provider<T> provider) {
        this.provider = provider;
        this.resourceClass = ResourceBuilder.rootResourceFromAnnotations(provider.getImpl().getClass());
    }

    public void registered(ResteasyProviderFactory factory) {
        factory.getInjectorFactory().createPropertyInjector(resourceClass, factory).inject(provider.getImpl());
    }

    public Object createResource(HttpRequest request, HttpResponse response, ResteasyProviderFactory factory) {
        return provider;
    }

    public void unregistered() {
    }

    public Class<?> getScannableClass() {
        return provider.getImpl().getClass();
    }

    public void requestFinished(HttpRequest request, HttpResponse response, Object resource) {
    }

}
