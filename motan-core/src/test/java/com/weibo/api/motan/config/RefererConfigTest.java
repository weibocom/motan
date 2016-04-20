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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.protocol.example.MockWorld;
import com.weibo.api.motan.rpc.URL;

/**
 * 
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
            assertTrue(e.getMessage().contains("Protocol is null"));
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
        List<URL> registryUrls = refererConfig.loadRegistryUrls();
        assertEquals(registries.size(), registryUrls.size());
    }
}
