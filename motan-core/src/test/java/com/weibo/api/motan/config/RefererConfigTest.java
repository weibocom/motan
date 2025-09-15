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

package com.weibo.api.motan.config;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.mock.MockClient;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.protocol.example.MockWorld;
import com.weibo.api.motan.rpc.RpcContext;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.transport.DefaultMeshClient;
import com.weibo.api.motan.transport.MeshClient;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * refererConfig unit test.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-18
 */

public class RefererConfigTest extends BaseTestCase {

    private RefererConfig<IWorld> refererConfig = null;
    private ServiceConfig<IWorld> serviceConfig = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        RegistryConfig registryConfig = mockLocalRegistryConfig();

        serviceConfig = mockIWorldServiceConfig();
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM);

        refererConfig = mockIWorldRefererConfig();
        refererConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        refererConfig.setRegistry(registryConfig);

        refererConfig.setCheck("false");
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (refererConfig != null) {
            refererConfig.destroy();
        }
        if (serviceConfig != null) {
            serviceConfig.unexport();
        }
    }

    @Test
    public void testGetRef() {
        MockWorld mWorld = new MockWorld();
        serviceConfig.setRef(mWorld);
        serviceConfig.export();

        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(refererConfig.getClusterSupports().size(), 1);

        int times = 3;
        for (int i = 0; i < times; i++) {
            ref.world("test");
        }
        assertEquals(times, mWorld.stringCount.get());
        serviceConfig.unexport();

        // destroy
        refererConfig.destroy();
        assertFalse(refererConfig.getInitialized().get());
    }

    @Test
    public void testException() {
        IWorld ref = null;

        // protocol empty
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>();
        refererConfig.setProtocols(protocols);
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("protocol not set correctly"));
        }

        // protocol not exists
        protocols.add(mockProtocolConfig("notExist"));
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("get extension fail"));
        }
        protocols.add(mockProtocolConfig("notExist"));

        // method config wrong
        MethodConfig mConfig = new MethodConfig();
        mConfig.setName("notExist");
        refererConfig.setMethods(mConfig);
        try {
            ref = refererConfig.getRef();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("not found method"));
        }
    }

    @Test
    public void testMultiProtocol() {
        List<ProtocolConfig> protocols = getMultiProtocols(MotanConstants.PROTOCOL_INJVM, MotanConstants.PROTOCOL_MOTAN);
        refererConfig.setProtocols(protocols);
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(protocols.size(), refererConfig.getClusterSupports().size());

    }

    @Test
    public void testMultiRegitstry() {
        List<RegistryConfig> registries =
                getMultiRegister(MotanConstants.REGISTRY_PROTOCOL_LOCAL, MotanConstants.REGISTRY_PROTOCOL_ZOOKEEPER);
        refererConfig.setRegistries(registries);
        refererConfig.loadRegistryUrls();
        assertEquals(registries.size(), refererConfig.getRegistryUrls().size());
    }

    @Test
    public void testMeshClientRef() {
        int timeout = 300;
        URL mockMeshUrl = new URL("motan2", "localhost", 18002, MeshClient.class.getName(), DefaultMeshClient.getDefaultParams());
        mockMeshUrl.addParameter(URLParamType.endpointFactory.getName(), "mockEndpoint");
        DefaultMeshClient meshClient = new DefaultMeshClient(mockMeshUrl);
        meshClient.init();
        refererConfig.setRequestTimeout(timeout);
        refererConfig.setMeshClient(meshClient);
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertNotNull(refererConfig.getMeshClient());

        int times = 3;
        for (int i = 0; i < times; i++) {
            RpcContext.destroy();
            ref.world("test");
            assertEquals(String.valueOf(timeout), RpcContext.getContext().getRequest().getAttachments().get(MotanConstants.M2_TIMEOUT));
        }
        assertEquals(times, MockClient.urlMap.get(meshClient.getUrl()).get());

        // destroy
        refererConfig.destroy();
        assertFalse(refererConfig.getInitialized().get());
    }

    @Test
    public void testClusterGroup() {
        refererConfig.setSandboxGroups("sandbox1, sandbox2,,,,sandbox1");
        refererConfig.setBackupGroups("backup1,,backup2,backup1" + "," + group); // With repeated groups、 master group
        refererConfig.setGreyGroups("grey1, grey-group2");
        refererConfig.setRegistry(createRemoteRegistryConfig("mock", "mock", "127.0.0.1:4567", 0));

        // injvm protocol not process sandbox groups and backup groups
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(1, refererConfig.getClusterSupports().size());
        refererConfig.destroy();

        // multi sandbox groups and backup groups
        refererConfig.setProtocol(mockProtocolConfig("motan2"));
        ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(7, refererConfig.getClusterSupports().size());
        assertEquals(7, GlobalRuntime.getRuntimeClusters().size());
        for (Entry<String, Cluster<?>> entry : GlobalRuntime.getRuntimeClusters().entrySet()) {
            assertEquals(entry.getValue(), refererConfig.getClusterSupports().get(entry.getKey()).getCluster());
        }

        // destroy
        refererConfig.destroy();
        assertFalse(refererConfig.getInitialized().get());
        assertEquals(0, refererConfig.getClusterSupports().size());
        assertEquals(0, GlobalRuntime.getRuntimeClusters().size());
    }

    @Test
    public void testGroupSuffix() {
        refererConfig.setSandboxGroups("suffix:-sandbox1");
        refererConfig.setBackupGroups("suffix:-backup1");
        refererConfig.setRegistry(createRemoteRegistryConfig("mock", "mock", "127.0.0.1:4567", 0));
        refererConfig.setProtocol(mockProtocolConfig("motan2"));
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(3, refererConfig.getClusterSupports().size());

        for (Entry<String, Cluster<?>> entry : GlobalRuntime.getRuntimeClusters().entrySet()) {
            if (entry.getKey().contains(RefererConfig.MASTER_CLUSTER_KEY)) {
                assertEquals(RefererConfig.MASTER_CLUSTER_KEY + entry.getValue().getUrl().getIdentity(),
                        entry.getKey());
            } else if (entry.getKey().contains(RefererConfig.SANDBOX_CLUSTER_KEY)) {
                assertEquals(RefererConfig.SANDBOX_CLUSTER_KEY + entry.getValue().getUrl().getIdentity(),
                        entry.getKey());
                assertEquals(group + "-sandbox1", entry.getValue().getUrl().getGroup());
            } else if (entry.getKey().contains(RefererConfig.BACKUP_CLUSTER_KEY)) {
                assertEquals(RefererConfig.BACKUP_CLUSTER_KEY + entry.getValue().getUrl().getIdentity(),
                        entry.getKey());
                assertEquals(group + "-backup1", entry.getValue().getUrl().getGroup());
            } else {
                fail();
            }
        }
        refererConfig.destroy();
    }

    @Test
    public void testDefaultSandboxGroup() {
        // test default sandbox group
        RefererConfig.DEFAULT_SANDBOX_GROUPS = "default-sandbox";
        refererConfig.setRegistry(createRemoteRegistryConfig("mock", "mock", "127.0.0.1:4567", 0));
        refererConfig.setProtocol(mockProtocolConfig("motan2"));
        IWorld ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(2, refererConfig.getClusterSupports().size());
        boolean contains = false;
        for (String key : refererConfig.getClusterSupports().keySet()) {
            if (key.startsWith(RefererConfig.SANDBOX_CLUSTER_KEY)) {
                assertEquals(RefererConfig.DEFAULT_SANDBOX_GROUPS,
                        refererConfig.getClusterSupports().get(key).getUrl().getGroup());
                contains = true;
            }
        }
        assertTrue(contains);
        refererConfig.destroy();

        // test "none" sandbox group
        refererConfig.setSandboxGroups(RefererConfig.NONE_GROUP_STRING); // will over write default sandbox group, so no sandbox cluster
        ref = refererConfig.getRef();
        assertNotNull(ref);
        assertEquals(1, refererConfig.getClusterSupports().size());
        for (String key : refererConfig.getClusterSupports().keySet()) {
            assertTrue(key.startsWith(RefererConfig.MASTER_CLUSTER_KEY));
        }
        refererConfig.destroy();
        RefererConfig.DEFAULT_SANDBOX_GROUPS = "";
    }

}
