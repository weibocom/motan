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

import com.weibo.api.motan.closable.Closable;
import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.Endpoint;
import com.weibo.api.motan.transport.EndpointManager;
import com.weibo.api.motan.transport.HeartbeatFactory;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 *
 */
public class HeartbeatClientEndpointManager implements EndpointManager{

    private ConcurrentMap<Client, HeartbeatFactory> endpoints = new ConcurrentHashMap<Client, HeartbeatFactory>();

    // 一般这个类创建的实例会比较少，如果共享的话，容易“被影响”，如果某个任务阻塞了
    private ScheduledExecutorService executorService = null;

    @Override
    public void init() {
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                for (Map.Entry<Client, HeartbeatFactory> entry : endpoints.entrySet()) {
                    Client endpoint = entry.getKey();

                    try {
                        // 如果节点是存活状态，那么没必要走心跳
                        if (endpoint.isAvailable()) {
                            continue;
                        }

                        HeartbeatFactory factory = entry.getValue();
                        endpoint.heartbeat(factory.createRequest());
                    } catch (Exception e) {
                        LoggerUtil.error("HeartbeatEndpointManager send heartbeat Error: url=" + endpoint.getUrl().getUri() + ", " + e.getMessage());
                    }
                }

            }
        }, MotanConstants.HEARTBEAT_PERIOD, MotanConstants.HEARTBEAT_PERIOD, TimeUnit.MILLISECONDS);
        ShutDownHook.registerShutdownHook(new Closable() {
            @Override
            public void close() {
                if (!executorService.isShutdown()) {
                    executorService.shutdown();
                }
            }
        });
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

    @Override
    public void addEndpoint(Endpoint endpoint) {
        if (!(endpoint instanceof Client)) {
            throw new MotanFrameworkException("HeartbeatClientEndpointManager addEndpoint Error: class not support " + endpoint.getClass());
        }

        Client client = (Client) endpoint;

        URL url = endpoint.getUrl();

        String heartbeatFactoryName = url.getParameter(URLParamType.heartbeatFactory.getName(), URLParamType.heartbeatFactory.getValue());

        HeartbeatFactory heartbeatFactory = ExtensionLoader.getExtensionLoader(HeartbeatFactory.class).getExtension(heartbeatFactoryName);

        if (heartbeatFactory == null) {
            throw new MotanFrameworkException("HeartbeatFactory not exist: " + heartbeatFactoryName);
        }

        endpoints.put(client, heartbeatFactory);
    }

    @Override
    public void removeEndpoint(Endpoint endpoint) {
        endpoints.remove(endpoint);
    }

    public Set<Client> getClients() {
        return Collections.unmodifiableSet(endpoints.keySet());
    }
}
