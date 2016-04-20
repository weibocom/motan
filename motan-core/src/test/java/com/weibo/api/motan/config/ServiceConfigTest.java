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
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * Service config test
 *
 * @author fishermen zhanglei
 * @version V1.0 created at: 2013-6-17
 */

public class ServiceConfigTest extends BaseTestCase {

    private ServiceConfig<IWorld> serviceConfig = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        serviceConfig = mockIWorldServiceConfig();
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.setRegistry(mockLocalRegistryConfig());
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        if (serviceConfig != null) {
            serviceConfig.unexport();
        }
    }

    @Test
    public void testExport() {
        serviceConfig.export();

        assertTrue(serviceConfig.getExported().get());
        assertEquals(serviceConfig.getExporters().size(), 1);
        assertEquals(serviceConfig.getRegistereUrls().size(), 1);

    }

    @Test
    public void testExportException() {
        // registry null
        serviceConfig = mockIWorldServiceConfig();
        try {
            serviceConfig.export();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Should set registry"));
        }
        serviceConfig.setRegistry(mockLocalRegistryConfig());

        // export null
        try {
            serviceConfig.export();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("export should not empty"));
        }

        // protocol not exist
        serviceConfig.setProtocol(mockProtocolConfig("notExist"));
        serviceConfig.setExport("notExist" + ":" + 0);
        try {
            serviceConfig.export();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Protocol is null"));
        }

        // service already exist
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0);
        serviceConfig.export();
        assertTrue(serviceConfig.getExported().get());

        ServiceConfig<IWorld> newServiceConfig = mockIWorldServiceConfig();
        newServiceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        newServiceConfig.setRegistry(mockLocalRegistryConfig());
        newServiceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0);
        try {
            newServiceConfig.export();
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("for same service"));
        }
    }

    @Test
    public void testMethodConfig() {
        List<MethodConfig> methods = new ArrayList<MethodConfig>();
        MethodConfig mc = new MethodConfig();
        mc.setName("world");
        mc.setRetries(1);
        mc.setArgumentTypes("void");
        mc.setRequestTimeout(123);
        methods.add(mc);

        mc = new MethodConfig();
        mc.setName("worldSleep");
        mc.setRetries(2);
        mc.setArgumentTypes("java.lang.String,int");
        mc.setRequestTimeout(456);
        methods.add(mc);

        serviceConfig.setRetries(10);
        serviceConfig.setMethods(methods);
        serviceConfig.export();
        assertEquals(serviceConfig.getExporters().size(), 1);
        assertEquals(serviceConfig.getRegistereUrls().size(), 1);
        URL serviceUrl = serviceConfig.getExporters().get(0).getUrl();
        assertEquals(
                123,
                serviceUrl.getMethodParameter("world", "void", URLParamType.requestTimeout.getName(),
                        URLParamType.requestTimeout.getIntValue()).intValue());
        assertEquals(
                456,
                serviceUrl.getMethodParameter("worldSleep", "java.lang.String,int", URLParamType.requestTimeout.getName(),
                        URLParamType.requestTimeout.getIntValue()).intValue());
        assertEquals(1, serviceUrl.getMethodParameter("world", "void", URLParamType.retries.getName(), URLParamType.retries.getIntValue())
                .intValue());
        assertEquals(
                2,
                serviceUrl.getMethodParameter("worldSleep", "java.lang.String,int", URLParamType.retries.getName(),
                        URLParamType.retries.getIntValue()).intValue());

    }

    @Test
    public void testMultiProtocol() {
        serviceConfig.setProtocols(getMultiProtocols(MotanConstants.PROTOCOL_INJVM, MotanConstants.PROTOCOL_MOTAN));
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0 + "," + MotanConstants.PROTOCOL_MOTAN + ":8002");
        serviceConfig.export();
        assertEquals(serviceConfig.getExporters().size(), 2);

    }

    @Test
    public void testMultiRegitstry() {
        serviceConfig.setRegistries(getMultiRegister(MotanConstants.REGISTRY_PROTOCOL_LOCAL, MotanConstants.REGISTRY_PROTOCOL_ZOOKEEPER));
        List<URL> registryUrls = serviceConfig.loadRegistryUrls();
        assertEquals(2, registryUrls.size());
    }

    @Test
    public void testUnexport() {
        testExport();
        serviceConfig.unexport();
        assertFalse(serviceConfig.getExported().get());
        assertEquals(serviceConfig.getExporters().size(), 0);
        assertEquals(serviceConfig.getRegistereUrls().size(), 0);
    }


}
