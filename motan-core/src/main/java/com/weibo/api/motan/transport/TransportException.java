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

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author fishermen
 * @version V1.0 created at: 2013-5-21
 */

public class TransportException extends IOException {

    private static final long serialVersionUID = 7057762354907226994L;

    private InetSocketAddress localAddress;
    private InetSocketAddress remoteAddress;

    public TransportException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message) {
        super(message);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public TransportException(InetSocketAddress localAddress, InetSocketAddress remoteAddress, String message, Throwable cause) {
        super(message, cause);
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

}
