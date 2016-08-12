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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.AbstractExporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.Server;

/**
 * 
 * @Description YarExporter
 * @author zhanglei
 * @date 2016-5-31
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
        // FIXME to avoid parameters ambiguous in weak type language，parameters size of method with
        // same name must be different.
        validateInterface(provider.getInterface());
        server = endpointFactory.createServer(url, yarProtocol.initRequestRouter(url, provider));

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

    protected void validateInterface(Class<?> interfaceClazz) {
        HashMap<String, List<Integer>> tempMap = new HashMap<String, List<Integer>>();
        for (Method m : interfaceClazz.getDeclaredMethods()) {
            if (!tempMap.containsKey(m.getName())) {
                List<Integer> templist = new ArrayList<Integer>();
                templist.add(m.getParameterTypes().length);
                tempMap.put(m.getName(), templist);
            } else {
                List<Integer> templist = tempMap.get(m.getName());
                if (templist.contains(m.getParameterTypes().length)) {
                    throw new MotanFrameworkException("in yar protocol, methods with same name must have different params size !");
                } else {
                    templist.add(m.getParameterTypes().length);
                }
            }
        }
    }

}
