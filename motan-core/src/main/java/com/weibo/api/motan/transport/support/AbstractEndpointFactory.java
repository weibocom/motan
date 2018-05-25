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

package com.weibo.api.motan.transport.support;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * abstract endpoint factory
 * 
 * <pre>
 * 		一些约定：
 * 
 * 		1） service :
 * 			1.1） not share channel :  某个service暴露服务的时候，不期望和别的service共享服务，明哲自保，比如你说：我很重要，我很重要。
 * 
 * 			1.2） share channel ： 某个service 暴露服务的时候，如果有某个模块，但是拆成10个接口，可以使用这种方式，不过有一些约束条件：接口的几个serviceConfig配置需要保持一致。
 * 				
 * 				不允许差异化的配置如下：
 * 					protocol, codec , serialize, maxContentLength , maxServerConnection , maxWorkerThread, workerQueueSize, heartbeatFactory
 * 				
 * 		2）心跳机制：
 * 
 * 			不同的protocol的心跳包格式可能不一样，无法进行强制，那么通过可扩展的方式，依赖heartbeatFactory进行heartbeat包的创建，
 * 			同时对于service的messageHandler进行wrap heartbeat包的处理。 
 * 
 * 			对于service来说，把心跳包当成普通的request处理，因为这种heartbeat才能够探测到整个service处理的关键路径的可用状况
 * 
 * </pre>
 * 
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-5
 * 
 */
public abstract class AbstractEndpointFactory implements EndpointFactory {

    /** 维持share channel 的service列表 **/
    protected Map<String, Server> ipPort2ServerShareChannel = new HashMap<String, Server>();
    protected ConcurrentMap<Server, Set<String>> server2UrlsShareChannel = new ConcurrentHashMap<Server, Set<String>>();

    private EndpointManager heartbeatClientEndpointManager = null;

    public AbstractEndpointFactory() {
        heartbeatClientEndpointManager = new HeartbeatClientEndpointManager();
        heartbeatClientEndpointManager.init();
    }

    @Override
    public Server createServer(URL url, MessageHandler messageHandler) {
        messageHandler = getHeartbeatFactory(url).wrapMessageHandler(messageHandler);

        synchronized (ipPort2ServerShareChannel) {
            String ipPort = url.getServerPortStr();
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

            boolean shareChannel =
                    url.getBooleanParameter(URLParamType.shareChannel.getName(), URLParamType.shareChannel.getBooleanValue());

            if (!shareChannel) { // 独享一个端口
                LoggerUtil.info(this.getClass().getSimpleName() + " create no_share_channel server: url={}", url);

                // 如果端口已经被使用了，使用该server bind 会有异常
                return innerCreateServer(url, messageHandler);
            }

            LoggerUtil.info(this.getClass().getSimpleName() + " create share_channel server: url={}", url);

            Server server = ipPort2ServerShareChannel.get(ipPort);

            if (server != null) {
                // can't share service channel
                if (!MotanFrameworkUtil.checkIfCanShareServiceChannel(server.getUrl(), url)) {
                    throw new MotanFrameworkException(
                            "Service export Error: share channel but some config param is different, protocol or codec or serialize or maxContentLength or maxServerConnection or maxWorkerThread or heartbeatFactory, source="
                                    + server.getUrl() + " target=" + url, MotanErrorMsgConstant.FRAMEWORK_EXPORT_ERROR);
                }

                saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

                return server;
            }

            url = url.createCopy();
            url.setPath(""); // 共享server端口，由于有多个interfaces存在，所以把path设置为空

            server = innerCreateServer(url, messageHandler);

            ipPort2ServerShareChannel.put(ipPort, server);
            saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

            return server;
        }
    }

    @Override
    public Client createClient(URL url) {
        LoggerUtil.info(this.getClass().getSimpleName() + " create client: url={}", url);
        return createClient(url, heartbeatClientEndpointManager);
    }

    @Override
    public void safeReleaseResource(Server server, URL url) {
        safeReleaseResource(server, url, ipPort2ServerShareChannel, server2UrlsShareChannel);
    }

    @Override
    public void safeReleaseResource(Client client, URL url) {
        destory(client);
    }

    private <T extends Endpoint> void safeReleaseResource(T endpoint, URL url, Map<String, T> ipPort2Endpoint,
            ConcurrentMap<T, Set<String>> endpoint2Urls) {
        boolean shareChannel = url.getBooleanParameter(URLParamType.shareChannel.getName(), URLParamType.shareChannel.getBooleanValue());

        if (!shareChannel) {
            destory(endpoint);
            return;
        }

        synchronized (ipPort2Endpoint) {
            String ipPort = url.getServerPortStr();
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

            if (endpoint != ipPort2Endpoint.get(ipPort)) {
                destory(endpoint);
                return;
            }

            Set<String> urls = endpoint2Urls.get(endpoint);
            urls.remove(protocolKey);

            if (urls.isEmpty()) {
                destory(endpoint);
                ipPort2Endpoint.remove(ipPort);
                endpoint2Urls.remove(endpoint);
            }
        }
    }

    private <T> void saveEndpoint2Urls(ConcurrentMap<T, Set<String>> map, T endpoint, String namespace) {
        Set<String> sets = map.get(endpoint);

        if (sets == null) {
            sets = new HashSet<String>();
            sets.add(namespace);
            map.putIfAbsent(endpoint, sets); // 规避并发问题，因为有release逻辑存在，所以这里的sets预先add了namespace
            sets = map.get(endpoint);
        }

        sets.add(namespace);
    }

    private HeartbeatFactory getHeartbeatFactory(URL url) {
        String heartbeatFactoryName = url.getParameter(URLParamType.heartbeatFactory.getName(), URLParamType.heartbeatFactory.getValue());
        return getHeartbeatFactory(heartbeatFactoryName);
    }

    private HeartbeatFactory getHeartbeatFactory(String heartbeatFactoryName) {
        HeartbeatFactory heartbeatFactory = ExtensionLoader.getExtensionLoader(HeartbeatFactory.class).getExtension(heartbeatFactoryName);

        if (heartbeatFactory == null) {
            throw new MotanFrameworkException("HeartbeatFactory not exist: " + heartbeatFactoryName);
        }

        return heartbeatFactory;
    }

    private Client createClient(URL url, EndpointManager endpointManager) {
        Client client = innerCreateClient(url);

        endpointManager.addEndpoint(client);

        return client;
    }

    private <T extends Endpoint> void destory(T endpoint) {
        if (endpoint instanceof Client) {
            endpoint.close();
            heartbeatClientEndpointManager.removeEndpoint(endpoint);
        } else {
            endpoint.close();
        }
    }

    public Map<String, Server> getShallServerChannels() {
        return Collections.unmodifiableMap(ipPort2ServerShareChannel);
    }

    public EndpointManager getEndpointManager() {
        return heartbeatClientEndpointManager;
    }

    protected abstract Server innerCreateServer(URL url, MessageHandler messageHandler);

    protected abstract Client innerCreateClient(URL url);

}
