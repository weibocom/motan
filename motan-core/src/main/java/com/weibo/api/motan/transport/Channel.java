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

import java.net.InetSocketAddress;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * 类说明
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-21
 */

public interface Channel {

    /**
     * get local socket address.
     * 
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * get remote socket address
     * 
     * @return
     */
    InetSocketAddress getRemoteAddress();

    /**
     * send request.
     *
     * @param request
     * @return response future
     * @throws TransportException
     */
    Response request(Request request) throws TransportException;

    /**
     * open the channel
     * 
     * @return
     */
    boolean open();

    /**
     * close the channel.
     */
    void close();

    /**
     * close the channel gracefully.
     */
    void close(int timeout);

    /**
     * is closed.
     * 
     * @return closed
     */
    boolean isClosed();

    /**
     * the node available status
     * 
     * @return
     */
    boolean isAvailable();

    /**
     * 
     * @return
     */
    URL getUrl();

}
