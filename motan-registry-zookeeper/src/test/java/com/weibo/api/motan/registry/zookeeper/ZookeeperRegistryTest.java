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

package com.weibo.api.motan.registry.zookeeper;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZookeeperRegistryTest {
    private ZookeeperRegistry registry;
    private ZkClient zkClient;

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        // zookeeper://127.0.0.1:2181/com.weibo.api.motan.registry.RegistryService?group=yf_rpc
        URL url = new URL("zookeeper", "127.0.0.1", port, "com.weibo.api.motan.registry.RegistryService");


        EmbeddedZookeeper embeddedZookeeper = new EmbeddedZookeeper();
        embeddedZookeeper.start();

        zkClient = new ZkClient("127.0.0.1:" + port);
        registry = new ZookeeperRegistry(url, zkClient);
    }

    @Test
    public void testDoRegister() {
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        registry.doRegister(url);

        assertTrue(zkClient.exists(registry.toNodePath(url, ZkNodeType.UNAVAILABLE_SERVER)));
    }

    @Test
    public void testDoUnregister() {
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        registry.doUnregister(url);

        assertFalse(zkClient.exists(registry.toNodePath(url, ZkNodeType.UNAVAILABLE_SERVER)));
        assertFalse(zkClient.exists(registry.toNodePath(url, ZkNodeType.AVAILABLE_SERVER)));
    }

    @Test
    public void testDoSubscribe() {
        final URL serverUrl = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 0, "com.weibo.motan.demo.service.MotanDemoService");
        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(URL registryUrl, List<URL> urls) {
            }
        };
        registry.doSubscribe(serverUrl, notifyListener);

        ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = registry.getUrlListeners();
        assertTrue(urlListeners.containsKey(serverUrl));
        assertTrue(zkClient.exists(registry.toNodePath(serverUrl, ZkNodeType.CLIENT)));
    }

    @Test
    public void testDoUnsubscribe() {
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(URL registryUrl, List<URL> urls) {
            }
        };
        registry.doUnsubscribe(url, notifyListener);

        ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = registry.getUrlListeners();
        assertFalse(urlListeners.containsKey(url));
    }

    @Test
    public void testDoDiscover() {
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        registry.doRegister(url);
        registry.doAvailable(url);
        List<URL> urls = registry.doDiscover(url);

        assertTrue(urls.contains(url));
    }

    @Test
    public void testDoAvailable() throws Exception {
        final Set<URL> urls = new HashSet<URL>();
        URL url1 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        URL url2 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8002, "com.weibo.motan.demo.service.MotanDemoService");
        urls.add(url1);
        urls.add(url2);
        for (URL u : urls) {
            registry.register(u);
        }

        registry.available(url1);
        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));

        registry.available(null);
        for (URL u : urls) {
            assertTrue(zkClient.exists(registry.toNodePath(u, ZkNodeType.AVAILABLE_SERVER)));
            assertFalse(zkClient.exists(registry.toNodePath(u, ZkNodeType.UNAVAILABLE_SERVER)));
        }
    }

    @Test
    public void testDoUnavailable() throws Exception {
        final Set<URL> urls = new HashSet<URL>();
        URL url1 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, "com.weibo.motan.demo.service.MotanDemoService");
        URL url2 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8002, "com.weibo.motan.demo.service.MotanDemoService");
        urls.add(url1);
        urls.add(url2);
        for (URL u : urls) {
            registry.register(u);
        }

        registry.unavailable(url1);
        assertFalse(zkClient.exists(registry.toNodePath(url1, ZkNodeType.AVAILABLE_SERVER)));
        assertTrue(zkClient.exists(registry.toNodePath(url1, ZkNodeType.UNAVAILABLE_SERVER)));

        registry.unavailable(null);
        for (URL u : urls) {
            assertFalse(zkClient.exists(registry.toNodePath(u, ZkNodeType.AVAILABLE_SERVER)));
            assertTrue(zkClient.exists(registry.toNodePath(u, ZkNodeType.UNAVAILABLE_SERVER)));
        }
    }
}
