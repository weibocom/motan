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

package com.weibo.api.motan.mock;

import java.net.InetSocketAddress;
import java.util.Collection;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.Server;
import com.weibo.api.motan.transport.TransportException;

/**
 * 
 * @Description MockServer
 * @author zhanglei28
 * @date 2016年3月17日
 *
 */
public class MockServer implements Server {
    URL url;

    public MockServer(URL url) {
        this.url = url;
    }

    @Override
    public InetSocketAddress getLocalAddress() {

        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {

        return null;
    }

    @Override
    public Response request(Request request) throws TransportException {

        return null;
    }

    @Override
    public boolean open() {

        return true;
    }

    @Override
    public void close() {


    }

    @Override
    public void close(int timeout) {


    }

    @Override
    public boolean isClosed() {

        return false;
    }

    @Override
    public boolean isAvailable() {

        return false;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isBound() {

        return false;
    }

    @Override
    public Collection<Channel> getChannels() {

        return null;
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {

        return null;
    }

}
