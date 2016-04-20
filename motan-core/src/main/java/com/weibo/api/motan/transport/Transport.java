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

import java.util.concurrent.Future;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-21
 * 
 */
public interface Transport {

    /**
     * remote transport
     * 
     * @return
     */
    Future<byte[]> transport(byte[] request) throws TransportException;

    /**
     * 判断transport的available状态
     * 
     * @return
     */
    boolean isAvailable();

    /**
     * 判断transport的connect状态
     * 
     * @return
     */
    boolean isConnect();

    /**
     * transport connect
     */
    boolean connect();

    /**
     * close transport
     */
    void close();

    /**
     * close transport
     */
    void close(int timeout);

    /**
     * transport is close?
     * 
     * @return
     */
    boolean isClose();
}
