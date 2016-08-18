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
package com.weibo.api.motan.transport.netty4.yar;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.Server;
import com.weibo.api.motan.transport.netty4.http.Netty4HttpServer;
import com.weibo.api.motan.transport.support.AbstractEndpointFactory;

/**
 * 
 * @Description yar endpoint factory use netty4
 * @author zhanglei
 * @date 2016-5-31
 *
 */
@SpiMeta(name = "netty4yar")
public class Netty4YarEndpointFactory extends AbstractEndpointFactory {

    @Override
    protected Server innerCreateServer(URL url, MessageHandler messageHandler) {
        return new Netty4HttpServer(url, new YarMessageHandlerWarpper(messageHandler));
    }

    @Override
    protected Client innerCreateClient(URL url) {
        // TODO 
        throw new MotanFrameworkException("not yet implemented!");
    }

}
