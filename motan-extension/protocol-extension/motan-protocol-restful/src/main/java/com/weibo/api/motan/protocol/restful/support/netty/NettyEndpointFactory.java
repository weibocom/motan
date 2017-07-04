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
package com.weibo.api.motan.protocol.restful.support.netty;

import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.protocol.restful.EmbedRestServer;
import com.weibo.api.motan.protocol.restful.RestServer;
import com.weibo.api.motan.protocol.restful.support.AbstractEndpointFactory;
import com.weibo.api.motan.protocol.restful.support.RestfulInjectorFactory;
import com.weibo.api.motan.protocol.restful.support.RpcExceptionMapper;
import com.weibo.api.motan.rpc.URL;

@SpiMeta(name = "netty")
public class NettyEndpointFactory extends AbstractEndpointFactory {

    @Override
    protected RestServer innerCreateServer(URL url) {
        NettyJaxrsServer server = new NettyJaxrsServer();
        server.setMaxRequestSize(url.getIntParameter(URLParamType.maxContentLength.getName(),
                URLParamType.maxContentLength.getIntValue()));

        ResteasyDeployment deployment = new ResteasyDeployment();

        server.setDeployment(deployment);
        server.setExecutorThreadCount(url.getIntParameter(URLParamType.maxWorkerThread.getName(),
                URLParamType.maxWorkerThread.getIntValue()));
        server.setPort(url.getPort());
        server.setRootResourcePath("");
        server.setSecurityDomain(null);

        deployment.setInjectorFactoryClass(RestfulInjectorFactory.class.getName());
        deployment.getProviderClasses().add(RpcExceptionMapper.class.getName());

        return new EmbedRestServer(server);
    }

}
