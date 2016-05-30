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
package com.weibo.api.motan.protocol.yar;

import java.util.concurrent.ConcurrentHashMap;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * @Description yar rpc protocol
 * @author zhanglei
 * @date 2016年5月25日
 *
 */
@SpiMeta(name = "yar")
public class YarRpcProtocol extends AbstractProtocol {
    protected ConcurrentHashMap<Integer, YarServer> serverMap = new ConcurrentHashMap<Integer, YarServer>();

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        // TODO Auto-generated method stub
        //TODO sharechannel
        YarServer server = serverMap.get(url.getPort());
        if(server == null){
            serverMap.putIfAbsent(url.getPort(), new YarServer(url));
            server = serverMap.get(url.getPort());
        }
        server.addProvider(provider);
        try {
            server.open();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new YarExporter<T>(server, url, provider);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        // TODO Auto-generated method stub
        return null;
    }

}
