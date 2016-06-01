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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.rpc.AbstractExporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.Server;

/**
 * 
 * @Description YarExporter
 * @author zhanglei
 * @date 2016年5月31日
 *
 */
public class YarExporter<T> extends AbstractExporter<T> {
    protected Server server;
    private YarRpcProtocol yarProtocol;

    public YarExporter(URL url, Provider<T> provider, YarRpcProtocol yarProtocol) {
        super(provider, url);
        EndpointFactory endpointFactory =
                ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                        url.getParameter(URLParamType.endpointFactory.getName(), "netty4yar"));
        // set noheartbeat factory
        String heartbeatFactory = url.getParameter(URLParamType.heartbeatFactory.getName());
        if (heartbeatFactory == null) {
            url.addParameter(URLParamType.heartbeatFactory.getName(), "noHeartbeat");
        }
        server = endpointFactory.createServer(url, yarProtocol.initRequestRouter(url, provider));
        // TODO 验证provider提供的方法名和参数个数不能相同
    }


    @Override
    public void destroy() {
        server.close();
    }

    @Override
    public boolean isAvailable() {
        return server.isAvailable();
    }

    @Override
    public void unexport() {
        yarProtocol.unexport(url, provider);
    }

    @Override
    protected boolean doInit() {
        return server.open();
    }

}
