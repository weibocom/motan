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

import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;

public class YarExporter<T> implements Exporter<T> {
    protected YarServer server;
    protected URL url;
    protected Provider<T> provider;
    
    
    public YarExporter(YarServer server, URL url, Provider<T> provider) {
        this.server = server;
        this.url = url;
        this.provider = provider;
        //TODO 验证provider提供的方法名和参数个数不能相同
    }

    @Override
    public void init() {
        if(!server.isOpen()){
            try {
                server.open();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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
    public String desc() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public URL getUrl() {
        // TODO Auto-generated method stub
        return url;
    }

    @Override
    public Provider<T> getProvider() {
        // TODO Auto-generated method stub
        return provider;
    }

    @Override
    public void unexport() {
        // TODO Auto-generated method stub
        
    }

}
