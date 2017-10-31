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

package com.weibo.api.motan.config.handler;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsg;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.support.ProtocolFilterDecorator;
import com.weibo.api.motan.proxy.ProxyFactory;
import com.weibo.api.motan.proxy.RefererInvocationHandler;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.StringTools;

import java.util.Collection;
import java.util.List;


/**
 * 
 * Handle refUrl to get referers, assemble to a cluster, create a proxy
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-31
 */

@SpiMeta(name = MotanConstants.DEFAULT_VALUE)
public class SimpleConfigHandler implements ConfigHandler {

    @Override
    public <T> ClusterSupport<T> buildClusterSupport(Class<T> interfaceClass, List<URL> registryUrls) {
        ClusterSupport<T> clusterSupport = new ClusterSupport<T>(interfaceClass, registryUrls);
        clusterSupport.init();

        return clusterSupport;
    }

    @Override
    public <T> T refer(Class<T> interfaceClass, List<Cluster<T>> clusters, String proxyType) {
        ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getExtension(proxyType);
        return proxyFactory.getProxy(interfaceClass, new RefererInvocationHandler<T>(interfaceClass, clusters));
    }

    @Override
    public <T> Exporter<T> export(Class<T> interfaceClass, T ref, List<URL> registryUrls) {

        String serviceStr = StringTools.urlDecode(registryUrls.get(0).getParameter(URLParamType.embed.getName()));
        URL serviceUrl = URL.valueOf(serviceStr);

        // export service
        // 利用protocol decorator来增加filter特性
        String protocolName = serviceUrl.getParameter(URLParamType.protocol.getName(), URLParamType.protocol.getValue());
        Protocol orgProtocol = ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(protocolName);
        Provider<T> provider = getProvider(orgProtocol, ref, serviceUrl, interfaceClass);

        Protocol protocol = new ProtocolFilterDecorator(orgProtocol);
        Exporter<T> exporter = protocol.export(provider, serviceUrl);

        // register service
        register(registryUrls, serviceUrl);

        return exporter;
    }

    protected <T> Provider<T> getProvider(Protocol protocol, T proxyImpl, URL url, Class<T> clz){
        if (protocol instanceof ProviderFactory){
            return ((ProviderFactory)protocol).newProvider(proxyImpl, url, clz);
        } else{
            return new DefaultProvider<T>(proxyImpl, url, clz);
        }
    }

    @Override
    public <T> void unexport(List<Exporter<T>> exporters, Collection<URL> registryUrls) {
        try {
            unRegister(registryUrls);
        } catch (Exception e1) {
            LoggerUtil.warn("Exception when unregister urls:" + registryUrls);
        }
        try {
            for (Exporter<T> exporter : exporters) {
                exporter.unexport();
            }
        } catch (Exception e) {
            LoggerUtil.warn("Exception when unexport exporters:" + exporters);
        }
    }

    private void register(List<URL> registryUrls, URL serviceUrl) {

        for (URL url : registryUrls) {
            // 根据check参数的设置，register失败可能会抛异常，上层应该知晓
            RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(url.getProtocol());
            if (registryFactory == null) {
                throw new MotanFrameworkException(new MotanErrorMsg(500, MotanErrorMsgConstant.FRAMEWORK_REGISTER_ERROR_CODE,
                        "register error! Could not find extension for registry protocol:" + url.getProtocol()
                                + ", make sure registry module for " + url.getProtocol() + " is in classpath!"));
            }
            Registry registry = registryFactory.getRegistry(url);
            registry.register(serviceUrl);
        }
    }

    private void unRegister(Collection<URL> registryUrls) {
        for (URL url : registryUrls) {
            // 不管check的设置如何，做完所有unregistry，做好清理工作
            try {
                String serviceStr = StringTools.urlDecode(url.getParameter(URLParamType.embed.getName()));
                URL serviceUrl = URL.valueOf(serviceStr);

                RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(url.getProtocol());
                Registry registry = registryFactory.getRegistry(url);
                registry.unregister(serviceUrl);
            } catch (Exception e) {
                LoggerUtil.warn(String.format("unregister url false:%s", url), e);
            }
        }
    }

}
