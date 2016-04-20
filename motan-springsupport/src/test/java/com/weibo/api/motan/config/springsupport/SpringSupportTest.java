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
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;

public class SpringSupportTest extends BaseTest {


    @Test
    public void testProtocoConfigTest() {
        Map<String, ProtocolConfig> map = cp.getBeansOfType(ProtocolConfig.class);
        assertEquals(2, map.size());
        ProtocolConfig injvm = map.get("injvm");
        ProtocolConfig motan_rpc = map.get("motan");
        assertTrue(motan_rpc != null && injvm != null);
        assertEquals(injvm.isDefault().booleanValue(), true);

    }

    @Test
    public void testRegistryConfig() {
        Map<String, RegistryConfig> map = cp.getBeansOfType(RegistryConfig.class);
        assertEquals(2, map.size());
        RegistryConfig local = map.get("myLocal");
        RegistryConfig mockRegistry = map.get("mockRegistry");
        assertTrue(local != null && mockRegistry != null);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testServiceConfig() {
        Map<String, ServiceConfig> map = cp.getBeansOfType(ServiceConfig.class);
        assertEquals(3, map.size());
        ServiceConfig serviceTest = map.get("serviceTest");
        ServiceConfig serviceTestWithMethodConfig = map.get("serviceTestWithMethodConfig");
        ServiceConfig serviceTestInjvm = map.get("serviceTestInjvm");
        assertTrue(serviceTest != null && serviceTestWithMethodConfig != null && serviceTestInjvm != null);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testRefererConfig() {
        Map<String, RefererConfig> map = cp.getBeansOfType(RefererConfig.class);
        assertEquals(3, map.size());
        RefererConfig clientTest = map.get("&clientTest");
        RefererConfig clientMethodTest = map.get("&clientMethodTest");
        assertTrue(clientTest != null && clientMethodTest != null);
    }
}
