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

package com.weibo.api.motan.proxy.spi;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.proxy.ProxyFactory;
import com.weibo.api.motan.proxy.RefererInvocationHandler;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * jdk proxy
 *
 * @author maijunsheng
 *
 */
@SpiMeta(name = "jdk")
public class JdkProxyFactory implements ProxyFactory {

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clz, List<Cluster<T>> clusters) {
        return (T) Proxy.newProxyInstance(clz.getClassLoader(), new Class[]{clz}, new RefererInvocationHandler<>(clz, clusters));
    }
}
