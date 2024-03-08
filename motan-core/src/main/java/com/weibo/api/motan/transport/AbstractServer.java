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

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-21
 */
public abstract class AbstractServer implements Server {
    protected InetSocketAddress localAddress;
    protected InetSocketAddress remoteAddress;

    protected URL url;
    protected Codec codec;

    protected volatile ChannelState state = ChannelState.UNINIT;


    public AbstractServer() {
    }

    public AbstractServer(URL url) {
        this.url = url;
        this.codec =
                ExtensionLoader.getExtensionLoader(Codec.class).getExtension(
                        url.getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
    }

    public void setRemoteAddress(InetSocketAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    @Override
    public Collection<Channel> getChannels() {
        throw new MotanFrameworkException(this.getClass().getName() + " getChannels() method unSupport " + url);
    }

    @Override
    public Channel getChannel(InetSocketAddress remoteAddress) {
        throw new MotanFrameworkException(this.getClass().getName() + " getChannel(InetSocketAddress) method unSupport " + url);
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    @Override
    public Map<String, Object> getRuntimeInfo() {
        Map<String, Object> infos = new HashMap<>();
        infos.put(RuntimeInfoKeys.URL_KEY, url.toFullStr());
        infos.put(RuntimeInfoKeys.CODEC_KEY, codec.getClass().getSimpleName());
        infos.put(RuntimeInfoKeys.STATE_KEY, state.name());
        return infos;
    }
}
