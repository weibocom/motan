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
package com.weibo.api.motan.protocol.restful.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.weibo.api.motan.protocol.restful.EndpointFactory;
import com.weibo.api.motan.protocol.restful.RestServer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

public abstract class AbstractEndpointFactory implements EndpointFactory {
    /** 维持share channel 的service列表 **/
    protected final Map<String, RestServer> ipPort2ServerShareChannel = new HashMap<String, RestServer>();
    // 维持share channel 的client列表 <ip:port,client>
    protected final Map<String, ResteasyWebTarget> ipPort2ClientShareChannel = new HashMap<String, ResteasyWebTarget>();

    protected Map<RestServer, Set<String>> server2UrlsShareChannel = new HashMap<RestServer, Set<String>>();
    protected Map<ResteasyWebTarget, Set<String>> client2UrlsShareChannel = new HashMap<ResteasyWebTarget, Set<String>>();

    @Override
    public RestServer createServer(URL url) {
        String ipPort = url.getServerPortStr();
        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

        LoggerUtil.info(this.getClass().getSimpleName() + " create share_channel server: url={}", url);

        synchronized (ipPort2ServerShareChannel) {
            RestServer server = ipPort2ServerShareChannel.get(ipPort);

            if (server != null) {
                saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

                return server;
            }

            url = url.createCopy();
            url.setPath(""); // 共享server端口，由于有多个interfaces存在，所以把path设置为空

            server = innerCreateServer(url);

            server.start();

            ipPort2ServerShareChannel.put(ipPort, server);
            saveEndpoint2Urls(server2UrlsShareChannel, server, protocolKey);

            return server;
        }
    }

    @Override
    public ResteasyWebTarget createClient(URL url) {
        String ipPort = url.getServerPortStr();
        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

        LoggerUtil.info(this.getClass().getSimpleName() + " create share_channel client: url={}", url);

        synchronized (ipPort2ClientShareChannel) {
            ResteasyWebTarget client = ipPort2ClientShareChannel.get(ipPort);

            if (client != null) {
                saveEndpoint2Urls(client2UrlsShareChannel, client, protocolKey);
                return client;
            }

            client = innerCreateClient(url);

            ipPort2ClientShareChannel.put(ipPort, client);
            saveEndpoint2Urls(client2UrlsShareChannel, client, protocolKey);

            return client;
        }
    }

    @Override
    public void safeReleaseResource(RestServer server, URL url) {
        String ipPort = url.getServerPortStr();
        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

        synchronized (ipPort2ServerShareChannel) {
            if (server != ipPort2ServerShareChannel.get(ipPort)) {
                server.stop();
                return;
            }

            Set<String> urls = server2UrlsShareChannel.get(server);
            urls.remove(protocolKey);

            if (urls.isEmpty()) {
                server.stop();
                ipPort2ServerShareChannel.remove(ipPort);
                server2UrlsShareChannel.remove(server);
            }
        }
    }

    @Override
    public void safeReleaseResource(ResteasyWebTarget client, URL url) {
        String ipPort = url.getServerPortStr();
        String protocolKey = MotanFrameworkUtil.getProtocolKey(url);

        synchronized (ipPort2ClientShareChannel) {
            if (client != ipPort2ClientShareChannel.get(ipPort)) {
                client.getResteasyClient().close();
                return;
            }

            Set<String> urls = client2UrlsShareChannel.get(client);
            urls.remove(protocolKey);

            if (urls.isEmpty()) {
                client.getResteasyClient().close();
                ipPort2ClientShareChannel.remove(ipPort);
                client2UrlsShareChannel.remove(client);
            }
        }
    }

    private <T> void saveEndpoint2Urls(Map<T, Set<String>> map, T endpoint, String protocolKey) {
        Set<String> sets = map.get(endpoint);

        if (sets == null) {
            sets = new HashSet<String>();
            map.put(endpoint, sets);
        }

        sets.add(protocolKey);
    }

    protected abstract RestServer innerCreateServer(URL url);

    protected ResteasyWebTarget innerCreateClient(URL url) {
        ResteasyClient client = new ResteasyClientBuilder().build();
        String contextpath = url.getParameter("contextpath", "/");
        if (!contextpath.startsWith("/"))
            contextpath = "/" + contextpath;

        return client.target("http://" + url.getHost() + ":" + url.getPort() + contextpath);
    }

}
