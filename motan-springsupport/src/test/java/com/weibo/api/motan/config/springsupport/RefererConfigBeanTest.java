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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.config.MethodConfig;
import com.weibo.api.motan.config.RefererConfig;

public class RefererConfigBeanTest extends BaseTest {
    RefererConfigBean<ITest> clientTest;
    RefererConfigBean<ITest> clientMethodTest;
    RefererConfigBean<ITest> clientDirectTest;



    @SuppressWarnings({"unchecked", "rawtypes"})
    @Before
    public void setUp() throws Exception {
        clientTest = (RefererConfigBean) cp.getBean("&clientTest");
        clientMethodTest = (RefererConfigBean) cp.getBean("&clientMethodTest");
        clientDirectTest = (RefererConfigBean) cp.getBean("&clientDirectTest");
    }

    @Test
    public void testSetMethodsListOfMethodConfig() {
        List<MethodConfig> methodConfigs = clientTest.getMethods();
        assertNull(methodConfigs);
        methodConfigs = clientMethodTest.getMethods();
        assertNotNull(methodConfigs);
        assertEquals(3, methodConfigs.size());
    }

    @Test
    public void testGetInitialized() {
        ITest test = (ITest) cp.getBean("clientTest");
        assertNotNull(test);
        test = (ITest) cp.getBean("clientMethodTest");
        assertNotNull(test);
    }

    @Test
    public void testBasicRefere() {
        assertNotNull(clientTest.getBasicReferer());
        assertNotNull(clientMethodTest.getBasicReferer());
    }

    @Test
    public void testDirectUrl() {
        assertEquals("127.0.0.1:7888", clientDirectTest.getDirectUrl());
    }

}
