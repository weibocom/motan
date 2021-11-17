/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.registry.weibomesh;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.netty4.NettyServer;
import com.weibo.api.motan.transport.support.DefaultRpcHeartbeatFactory;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.UrlUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2021/8/5.
 */
public class MeshRegistryTest {
    URL registryUrl;
    MeshRegistry registry;
    URL subUrl;
    MockRegistry mockProxyRegistry;
    int copy;
    int requestTimeout;
    MockMessageHandler handler;

    @Before
    public void setUp() throws Exception {
        copy = 3; //默认副本数
        requestTimeout = 100;
        URL agentMockUrl = new URL("motan2", "localhost", 0, "testpath", new HashMap<>());
        agentMockUrl.addParameter(URLParamType.codec.getName(), "motan2");
        handler = new MockMessageHandler();
        NettyServer mockAgent = new NettyServer(agentMockUrl, handler);
        mockAgent.open();

        int agentPort = agentMockUrl.getPort();
        registryUrl = new URL("weibomesh", "localhost", agentPort, "", new HashMap<>());
        registryUrl.addParameter(MeshRegistry.MESH_PARAM_COPY, String.valueOf(copy));
        registryUrl.addParameter(URLParamType.requestTimeout.getName(), String.valueOf(requestTimeout));
        registryUrl.addParameter(URLParamType.registrySessionTimeout.getName(), String.valueOf(requestTimeout));
        subUrl = new URL("motan2", "localhost", 0, "com.weibo.test.TestService");
        registry = new MeshRegistry(registryUrl, new MockMeshTransport());

        List<URL> notifyUrls = new ArrayList();
        notifyUrls.add(new URL("motan2", "127.0.0.1", 8999, "testService"));
        Map<URL, List<URL>> nodesMap = new HashMap();
        nodesMap.put(subUrl, notifyUrls);
        mockProxyRegistry = new MockRegistry(nodesMap);

        registry.setProxyRegistry(mockProxyRegistry);
        //因为测试流程原因，单测时需要手动触发健康检测
        registry.initHealthCheck();

        //开关默认值
        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_SWITCHER_NAME, true);
        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, true);
    }

    @Test
    public void testRegister() throws Exception {
        registry.register(subUrl);
        assertTrue(registry.getRegisteredServiceUrls().contains(subUrl));
        assertFalse(mockProxyRegistry.getRegisteredServiceUrls().contains(subUrl));

        registry.unregister(subUrl);
        assertFalse(registry.getRegisteredServiceUrls().contains(subUrl));

        // switcher off
        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_SWITCHER_NAME, false);
        registry.register(subUrl);
        assertTrue(registry.getRegisteredServiceUrls().contains(subUrl));
        assertTrue(mockProxyRegistry.getRegisteredServiceUrls().contains(subUrl));

        registry.unregister(subUrl);
        assertFalse(registry.getRegisteredServiceUrls().contains(subUrl));
        assertFalse(mockProxyRegistry.getRegisteredServiceUrls().contains(subUrl));
    }


    @Test
    public void testDoSubscribe() throws Exception {
        TestNotifyListener notifyListener = new TestNotifyListener();
        registry.doSubscribe(subUrl, notifyListener);
        // 验证通知是否为agent节点
        assertEquals(notifyListener.urls.size(), copy);
        assertTrue(isAgentUrl(notifyListener.urls.get(0)));
        assertEquals(notifyListener.urls.get(0).getPath(), subUrl.getPath());
        assertEquals(notifyListener.urls.get(0).getGroup(), subUrl.getGroup());

        // 验证降级开关
        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_SWITCHER_NAME, false);
        Thread.sleep(50l);
        assertEquals(notifyListener.urls, mockProxyRegistry.discover(subUrl));

        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_SWITCHER_NAME, true);
        Thread.sleep(50l);
        assertEquals(notifyListener.urls.size(), copy);

        // health check
        handler.enableHeartbeat = false;
        Thread.sleep(5 * requestTimeout);
        assertEquals(notifyListener.urls, mockProxyRegistry.discover(subUrl));

        handler.enableHeartbeat = true;
        Thread.sleep(5 * requestTimeout);
        assertEquals(notifyListener.urls.size(), copy);

        // health check switcher
        handler.enableHeartbeat = false;
        Thread.sleep(5 * requestTimeout);
        assertEquals(notifyListener.urls, mockProxyRegistry.discover(subUrl));

        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, false);
        Thread.sleep(50);
        assertEquals(notifyListener.urls.size(), copy);

        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, true);
        Thread.sleep(5 * requestTimeout);
        assertEquals(notifyListener.urls, mockProxyRegistry.discover(subUrl));

        handler.enableHeartbeat = true;
        Thread.sleep(5 * requestTimeout);
        assertEquals(notifyListener.urls.size(), copy);
    }

    @Test
    public void testDoUnsubscribe() throws Exception {
        TestNotifyListener notifyListener = new TestNotifyListener();
        registry.doSubscribe(subUrl, notifyListener);
        int count = notifyListener.count.get();
        assertEquals(1, count);
        registry.doUnsubscribe(subUrl, notifyListener);
        MotanSwitcherUtil.setSwitcherValue(MeshRegistry.MESH_REGISTRY_SWITCHER_NAME, false);
        Thread.sleep(50l);
        assertEquals(count, notifyListener.count.get());
    }

    @Test
    public void testDoDiscover() throws Exception {
        List<URL> result = registry.discover(subUrl);
        assertEquals(copy, result.size());

        // 未订阅节点（没有backup节点）
        registry.setUseMesh(false);
        result = registry.doDiscover(subUrl);
        assertEquals(copy, result.size());

        TestNotifyListener notifyListener = new TestNotifyListener();
        registry.doSubscribe(subUrl, notifyListener);
        Thread.sleep(50l);
        result = registry.doDiscover(subUrl);
        assertEquals(mockProxyRegistry.discover(subUrl), result);
    }

    @Test
    public void testProxyRegistry() {
        String proxyRegistryString = "direct%3A%2F%2Flocalhost%3A9982%2Fcom.weibo.api.motan.registry.RegistryService%3Fpath%3Dcom.weibo.api.motan.registry.RegistryService%26protocol%3Ddirect%26address%3Dlocalhost%253A9982%26name%3Dregistry%26dynamic%3Dfalse%26id%3Dregistry%26refreshTimestamp%3D1628763334951%26";
        registryUrl.addParameter(URLParamType.proxyRegistryUrlString.getName(), proxyRegistryString);
        registry = new MeshRegistry(registryUrl, new MockMeshTransport());
        Registry proxyRegistry = registry.getProxyRegistry();
        assertNotNull(proxyRegistry);
        List<URL> urls = UrlUtils.stringToURLs(proxyRegistryString);
        assertEquals(urls.get(0), proxyRegistry.getUrl());
    }

    @Test
    public void testDynamic() throws Exception {
        String mport = "8888";
        MockMeshTransport transport = new MockMeshTransport();
        registryUrl.addParameter(URLParamType.dynamic.getName(), "true");
        registryUrl.addParameter(URLParamType.meshMPort.getName(), mport);
        registry = new MeshRegistry(registryUrl, transport);
        registry.doRegister(subUrl);
        Thread.sleep(300l);
        assertEquals(1, transport.records.size());
        assertEquals("http://" + registryUrl.getHost() + ":" + mport + MeshRegistry.MESH_REGISTER_URL, transport.records.get(0).getLeft());
        assertEquals(Util.UrlToJson(subUrl), transport.records.get(0).getRight());

        registry.doSubscribe(subUrl, new TestNotifyListener());
        Thread.sleep(300l);
        assertEquals(2, transport.records.size());
        assertEquals("http://" + registryUrl.getHost() + ":" + mport + MeshRegistry.MESH_SUBSCRIBE_URL, transport.records.get(1).getLeft());
        assertEquals(Util.UrlToJson(subUrl), transport.records.get(1).getRight());
    }

    private boolean isAgentUrl(URL url) {
        return registry.getUrl().getHost().equals(url.getHost()) && (registry.getUrl().getPort().equals(url.getPort()));
    }

    class MockMeshTransport implements MeshTransport {
        public int code = 200;
        List<Pair<String, String>> records = new ArrayList();

        @Override
        public ManageResponse getManageRequest(String url) throws MotanFrameworkException {
            throw new RuntimeException("not implement");
        }

        @Override
        public ManageResponse postManageRequest(String url, Map<String, String> params) throws MotanFrameworkException {
            throw new RuntimeException("not implement");
        }

        @Override
        public ManageResponse postManageRequest(String url, String content) throws MotanFrameworkException {
            records.add(new MutablePair(url, content));
            return new ManageResponse(code, "");
        }
    }

    class MockRegistry implements Registry {
        URL registryUrl = new URL("mock", "localhost", 0, "mockpath");
        Map<URL, List<URL>> nodesMap;
        Map<URL, NotifyListener> listenerMap = new HashMap();
        Set<URL> registedUrls = new HashSet();

        public void setNodesMap(Map<URL, List<URL>> nodesMap) {
            this.nodesMap = nodesMap;
        }

        public MockRegistry(Map<URL, List<URL>> nodesMap) {
            this.nodesMap = nodesMap;
        }

        @Override
        public void subscribe(URL url, NotifyListener listener) {
            listenerMap.put(url, listener);
            listener.notify(registryUrl, discover(url));
        }

        @Override
        public void unsubscribe(URL url, NotifyListener listener) {
            listenerMap.remove(url);
        }

        @Override
        public List<URL> discover(URL url) {
            return nodesMap.get(url);
        }

        @Override
        public void register(URL url) {
            registedUrls.add(url);
        }

        @Override
        public void unregister(URL url) {
            registedUrls.remove(url);
        }

        @Override
        public void available(URL url) {
        }

        @Override
        public void unavailable(URL url) {
        }

        @Override
        public Collection<URL> getRegisteredServiceUrls() {
            return registedUrls;
        }

        @Override
        public URL getUrl() {
            return null;
        }

        public Map<URL, NotifyListener> getListenerMap() {
            return listenerMap;
        }
    }

    class MockMessageHandler implements MessageHandler {
        public boolean enableHeartbeat = true;
        public int heartbeatCount;

        @Override
        public Object handle(Channel channel, Object message) {
            if (enableHeartbeat && DefaultRpcHeartbeatFactory.isHeartbeatRequest(message)) {
                Response response = DefaultRpcHeartbeatFactory.getDefaultHeartbeatResponse(((Request) message).getRequestId());
                response.setRpcProtocolVersion(((Request) message).getRpcProtocolVersion());
                heartbeatCount++;
                return response;
            }
            try { // mock timeout
                Thread.sleep(3000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}