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

package com.weibo.api.motan;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.protocol.example.MockWorld;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * base test
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-23
 */

public class BaseTestCase extends TestCase {

    protected static String application = "api";
    protected static String module = "test";

    protected String localAddress = null;

    protected static String group = "test-yf";

    public static JUnit4Mockery mockery = null;

    @Before
    @Override
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        InetAddress address = NetUtils.getLocalAddress();
        if (address != null) {
            localAddress = address.getHostAddress();
        }

    }

    @After
    @Override
    public void tearDown() throws Exception {}

    public void testBase() {}

    protected static RefererConfig<IWorld> mockIWorldRefererConfig() {
        RefererConfig<IWorld> rc = new RefererConfig<IWorld>();
        rc.setInterface(IWorld.class);
        rc.setApplication(application);
        rc.setModule(module);
        rc.setGroup(group);
        return rc;
    }

    protected static ServiceConfig<IWorld> mockIWorldServiceConfig() {
        ServiceConfig<IWorld> serviceConfig = new ServiceConfig<IWorld>();
        serviceConfig.setRef(new MockWorld());
        serviceConfig.setApplication(application);
        serviceConfig.setModule(module);
        serviceConfig.setCheck("true");
        serviceConfig.setInterface(IWorld.class);
        serviceConfig.setGroup(group);
        serviceConfig.setShareChannel(true);

        return serviceConfig;
    }

    protected static ProtocolConfig mockProtocolConfig(String protocolName) {
        ProtocolConfig pc = createProtocol(protocolName);
        pc.setEndpointFactory("mockEndpoint");
        return pc;
    }


    protected static <T> ServiceConfig<T> createServiceConfig(Class<T> clz, T impl) {
        ServiceConfig<T> serviceConfig = new MockServiceConfig<T>();
        serviceConfig.setRef(impl);
        serviceConfig.setApplication(application);
        serviceConfig.setModule(module);
        serviceConfig.setCheck("true");
        serviceConfig.setInterface(clz);
        serviceConfig.setGroup(group);
        serviceConfig.setShareChannel(true);
        serviceConfig.setVersion("2.0");

        return serviceConfig;
    }

    protected static <T> ServiceConfig<T> createServiceConfig(Class<T> clz, T impl, String group, String version, ProtocolConfig protocl,
            RegistryConfig registryConfig, String export) {
        ServiceConfig<T> serviceConfig = new MockServiceConfig<T>();
        serviceConfig.setRef(impl);
        serviceConfig.setApplication(application);
        serviceConfig.setModule(module);
        serviceConfig.setCheck("true");
        serviceConfig.setInterface(clz);
        serviceConfig.setGroup(group);
        serviceConfig.setShareChannel(true);
        serviceConfig.setVersion(version);
        serviceConfig.setProtocol(protocl);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setExport(export);
        return serviceConfig;
    }

    protected static <T> RefererConfig<T> createRefererConfig(Class<T> clz) {
        RefererConfig<T> rc = new RefererConfig<T>();
        rc.setInterface(clz);
        rc.setApplication(application);
        rc.setModule(module);
        rc.setGroup(group);
        rc.setRequestTimeout(2000);
        rc.setVersion("2.0");
        return rc;
    }

    protected static ProtocolConfig createProtocol(String protocolName) {
        ProtocolConfig pc = new ProtocolConfig();
        pc.setName(protocolName);
        pc.setId(pc.getName());
        return pc;
    }

    protected static RegistryConfig mockLocalRegistryConfig() {
        return createLocalRegistryConfig(MotanConstants.REGISTRY_PROTOCOL_LOCAL, MotanConstants.REGISTRY_PROTOCOL_LOCAL);
    }

    protected static RegistryConfig createLocalRegistryConfig(String protocol, String name) {
        RegistryConfig rc = new RegistryConfig();
        rc.setRegProtocol(protocol);
        rc.setName(name);
        rc.setId(rc.getName());

        return rc;
    }

    protected static RegistryConfig createRemoteRegistryConfig(String protocol, String name, String address, int port) {
        RegistryConfig rc = new RegistryConfig();
        rc.setRegProtocol(protocol);
        rc.setName(name);
        rc.setId(rc.getName());
        rc.setAddress(address);
        rc.setPort(port);

        return rc;
    }

    protected static List<ProtocolConfig> getMultiProtocols(String... protocolNames) {
        List<ProtocolConfig> protocols = new ArrayList<ProtocolConfig>();
        for (String protocol : protocolNames) {
            protocols.add(mockProtocolConfig(protocol));
        }
        return protocols;
    }

    protected static List<RegistryConfig> getMultiRegister(String... registerName) {
        List<RegistryConfig> registries = new ArrayList<RegistryConfig>();
        for (String register : registerName) {
            RegistryConfig registryConfig = createLocalRegistryConfig(register, register);
            registries.add(registryConfig);
        }
        return registries;
    }
}


class MockServiceConfig<T> extends ServiceConfig<T> {
    private static final long serialVersionUID = 7965700855475224943L;

    protected boolean serviceExists(URL url) {
        return false;
    }
}
