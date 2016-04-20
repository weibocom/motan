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

package com.weibo.api.motan.transport;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.URL;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-5
 * 
 */
@Spi(scope = Scope.SINGLETON)
public interface EndpointFactory {

    /**
     * create remote server
     * 
     * @param url
     * @param messageHandler
     * @return
     */
    Server createServer(URL url, MessageHandler messageHandler);

    /**
     * create remote client
     * 
     * @param url
     * @return
     */
    Client createClient(URL url);

    /**
     * safe release server
     * 
     * @param server
     * @param url
     */
    void safeReleaseResource(Server server, URL url);

    /**
     * safe release client
     * 
     * @param client
     * @param url
     */
    void safeReleaseResource(Client client, URL url);

}
