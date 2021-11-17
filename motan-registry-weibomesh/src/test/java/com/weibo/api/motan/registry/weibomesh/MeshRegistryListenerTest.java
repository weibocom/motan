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

import com.weibo.api.motan.rpc.URL;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author zhanglei28
 * @date 2021/8/5.
 */
public class MeshRegistryListenerTest {
    MeshRegistryListener listener;
    MeshRegistry registry;
    URL subUrl;

    @Before
    public void setUp() throws Exception {
        URL registryUrl = new URL("weibomesh", "localhost", 9981, "");
        subUrl = new URL("motan2", "localhost", 0, "com.weibo.test.TestService");
        registry = new MeshRegistry(registryUrl, new DefaultHttpMeshTransport());
        listener = new MeshRegistryListener(registry, subUrl);
    }

    @Test
    public void testInit() {
        // test copy size
        int copy = 0;
        for (int i = 0; i < 5; i++) {
            copy = ThreadLocalRandom.current().nextInt(20);
            registry.getUrl().addParameter(MeshRegistry.MESH_PARAM_COPY, String.valueOf(copy));
            registry.getUrl().clearCacheInfo();
            listener = new MeshRegistryListener(registry, subUrl);
            assertEquals(listener.getMeshNodes().size(), copy);
        }
    }

    @Test
    public void testNotify() {
        TestNotifyListener notifyListener = new TestNotifyListener();
        listener.addListener(notifyListener);
        assertNull(listener.getBackupNodes());

        List<URL> notifyUrls = new ArrayList();
        notifyUrls.add(new URL("motan2", "127.0.0.1", 8999, "testService"));

        // usemesh, 更新backupNodes，但不通知notifyListener
        registry.setUseMesh(true);
        listener.notify(registry.getUrl(), notifyUrls);
        assertEquals(notifyUrls, listener.getBackupNodes());
        assertEquals(0, notifyListener.count.get());

        // not use mesh，更新backupNodes，同时通知notifyListener
        registry.setUseMesh(false);
        listener.notify(registry.getUrl(), notifyUrls);
        assertEquals(notifyUrls, listener.getBackupNodes());
        assertEquals(1, notifyListener.count.get());
        assertEquals(notifyUrls, notifyListener.urls);

        // multi notify
        registry.setUseMesh(false);
        notifyListener.count.set(0);
        for (int i = 0; i < 3; i++) {
            listener.notify(registry.getUrl(), notifyUrls);
            assertEquals(i + 1, notifyListener.count.get());
            assertEquals(notifyUrls, notifyListener.urls);
            assertEquals(notifyUrls, listener.getBackupNodes());
        }

        // empty notify
        notifyListener.count.set(0);
        listener.notify(registry.getUrl(), new ArrayList<>());
        assertEquals(0, notifyListener.count.get());
    }


    @Test
    public void testDoNotify() throws Exception {
        TestNotifyListener notifyListener = new TestNotifyListener();
        listener.addListener(notifyListener);
        List<URL> notifyUrls = new ArrayList();
        notifyUrls.add(new URL("motan2", "127.0.0.1", 8999, "testService"));


        // use mesh
        listener.doNotify(true);
        assertEquals(1, notifyListener.count.get());
        assertEquals(listener.getMeshNodes(), notifyListener.urls);

        // not use mesh && 没有backupNodes
        listener.doNotify(false);
        assertEquals(1, notifyListener.count.get());


        // 有backupNodes
        registry.setUseMesh(true);
        listener.notify(registry.getUrl(), notifyUrls);
        listener.doNotify(false);
        assertEquals(2, notifyListener.count.get());
        assertEquals(listener.getBackupNodes(), notifyListener.urls);
    }

    @Test
    public void testGetUrls() throws Exception {
        registry.setUseMesh(true);
        List<URL> urls = listener.getUrls();
        assertEquals(listener.getMeshNodes(), urls);

        registry.setUseMesh(false);
        urls = listener.getUrls();
        assertEquals(listener.getMeshNodes(), urls);

        List<URL> notifyUrls = new ArrayList();
        notifyUrls.add(new URL("motan2", "127.0.0.1", 8999, "testService"));
        listener.notify(registry.getUrl(), notifyUrls);

        registry.setUseMesh(true);
        urls = listener.getUrls();
        assertEquals(listener.getMeshNodes(), urls);

        registry.setUseMesh(false);
        urls = listener.getUrls();
        assertEquals(listener.getBackupNodes(), urls);
    }
}