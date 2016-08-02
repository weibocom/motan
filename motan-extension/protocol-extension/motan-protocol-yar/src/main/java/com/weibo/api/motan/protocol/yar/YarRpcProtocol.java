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

import java.util.concurrent.ConcurrentHashMap;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.ProviderMessageRouter;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

/**
 * 
 * @Description yar rpc protocol
 * @author zhanglei
 * @date 2016-5-25
 *
 */
@SpiMeta(name = "yar")
public class YarRpcProtocol extends AbstractProtocol {
    private ConcurrentHashMap<String, ProviderMessageRouter> ipPort2RequestRouter = new ConcurrentHashMap<String, ProviderMessageRouter>();

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        
        return new YarExporter<T>(url, provider, this);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        //TODO
        throw new MotanFrameworkException("not yet implemented!");
    }

    public ProviderMessageRouter initRequestRouter(URL url, Provider<?> provider) {
        String ipPort = url.getServerPortStr();
        ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);
        if (requestRouter == null) {
            ipPort2RequestRouter.putIfAbsent(ipPort, new YarMessageRouter());
            requestRouter = ipPort2RequestRouter.get(ipPort);
        }
        requestRouter.addProvider(provider);
        return requestRouter;
    }
    
    public void unexport(URL url, Provider<?> provider){
        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);
        String ipPort = url.getServerPortStr();

        Exporter<?> exporter = (Exporter<?>) exporterMap.remove(protocolKey);

        if (exporter != null) {
            exporter.destroy();
        }

        synchronized (ipPort2RequestRouter) {
            ProviderMessageRouter requestRouter = ipPort2RequestRouter.get(ipPort);

            if (requestRouter != null) {
                requestRouter.removeProvider(provider);
            }
        }

        LoggerUtil.info("yarRpcExporter unexport Success: url={}", url);
    }

}
