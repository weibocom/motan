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

package com.weibo.api.motan.config.springsupport;

import java.util.Map;
import java.util.Map.Entry;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.Server;

public class BaseTest {

    ApplicationContext cp;
    public static JUnit4Mockery mockery = null;
    static MockEndpointFactory endpointFactory;


    @Before
    public void before() {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final Server mockServer = mockery.mock(Server.class);
        final Client mockClient = mockery.mock(Client.class);
        mockery.checking(new Expectations() {
            {
                allowing(mockClient).open();
                will(returnValue(true));
                allowing(mockClient).close();
                will(returnValue(null));
                allowing(mockClient).isAvailable();
                will(returnValue(true));

                allowing(mockServer).open();
                will(returnValue(true));
                allowing(mockServer).close();
                will(returnValue(null));
                allowing(mockServer).isAvailable();
                will(returnValue(true));
            }
        });

        ExtensionLoader loader = ExtensionLoader.getExtensionLoader(EndpointFactory.class);
        endpointFactory = (MockEndpointFactory) loader.getExtension("mockEndpoint");
        if (endpointFactory == null) {
            loader.addExtensionClass(MockEndpointFactory.class);
            endpointFactory = (MockEndpointFactory) loader.getExtension("mockEndpoint");
        }


        loader = ExtensionLoader.getExtensionLoader(RegistryFactory.class);
        MockRegistryFactory registryFactory = (MockRegistryFactory) loader.getExtension("mockRegistry");
        if (registryFactory == null) {
            loader.addExtensionClass(MockRegistryFactory.class);
        }


        endpointFactory.setClient(mockClient);
        endpointFactory.setServer(mockServer);
        cp = new ClassPathXmlApplicationContext("classpath:schemaTestContext.xml");
    }


    @After
    @SuppressWarnings("rawtypes")
    public void after() {
        Map<String, ServiceConfig> serviceMap = cp.getBeansOfType(ServiceConfig.class);
        for (Entry<String, ServiceConfig> entry : serviceMap.entrySet()) {
            entry.getValue().unexport();
        }
        Map<String, RefererConfig> refererMap = cp.getBeansOfType(RefererConfig.class);
        for (Entry<String, RefererConfig> entry : refererMap.entrySet()) {
            entry.getValue().destroy();
        }
        mockery = null;
        cp = null;
    }

}
