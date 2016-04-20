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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ConcurrentHashSet;

public class ServiceConfigBeanTest extends BaseTest {
    ServiceConfig<ITest> serviceTest;
    ServiceConfig<ITest> serviceTest2;
    ServiceConfig<ITest> serviceTest3;

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void setUp() throws Exception {
        serviceTest = (ServiceConfig) cp.getBean("serviceTest");
        serviceTest2 = (ServiceConfig) cp.getBean("serviceTestWithMethodConfig");
        serviceTest3 = (ServiceConfig) cp.getBean("serviceTestInjvm");
    }

    @After
    public void tearDown() throws Exception {}

    @Test
    public void testGetRef() {
        ITest test = serviceTest.getRef();
        assertTrue(test instanceof TestImpl);
        assertEquals(test, serviceTest2.getRef());
        assertNotSame(test, serviceTest3.getRef());
    }

    @Test
    public void testExport() {
        assertTrue(serviceTest.getExported().get());
        assertTrue(serviceTest2.getExported().get());
        assertTrue(serviceTest3.getExported().get());
    }

    @Test
    public void testGetProtocolAndPort() {
        List<Exporter<ITest>> exporters = serviceTest.getExporters();
        assertEquals(2, exporters.size());
        boolean injvm = false;
        boolean motan = false;
        for (Exporter<ITest> exporter : exporters) {
            URL url = exporter.getUrl();
            if ("injvm".equals(url.getProtocol()) && url.getPort() == 0) {
                injvm = true;
            } else if ("motan".equals(url.getProtocol()) && url.getPort() == 7888) {
                motan = true;
            }
        }
        assertTrue(injvm && motan);

        exporters = serviceTest2.getExporters();
        URL url = exporters.get(0).getUrl();
        assertEquals(1, exporters.size());
        assertEquals("motan", url.getProtocol());
        assertEquals(18080, url.getPort().intValue());

    }

    @Test
    public void testGetRegistereUrls() {
        ConcurrentHashSet<URL> registries = serviceTest.getRegistereUrls();
        assertEquals(3, registries.size());// 每种协议在每个注册中心都会导出，injvm协议只导出localregistry。
        boolean local = false;
        boolean mock = false;
        for (URL url : registries) {
            if ("local".equals(url.getProtocol())) {
                local = true;
            }
            if ("mockRegistry".equals(url.getProtocol())) {
                mock = true;
            }
        }
        assertTrue(local && mock);
    }

}
