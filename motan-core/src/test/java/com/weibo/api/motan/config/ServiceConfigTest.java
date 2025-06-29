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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.URL;

import java.util.*;

import static com.weibo.api.motan.TestUtils.getModifiableEnvironment;
import static com.weibo.api.motan.common.MotanConstants.ENV_ADDITIONAL_GROUP;
import static com.weibo.api.motan.common.MotanConstants.ENV_RPC_REG_GROUP_SUFFIX;

/**
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

    public void testExport() {
        serviceConfig.export();

        assertTrue(serviceConfig.getExported().get());
        assertEquals(serviceConfig.getExporters().size(), 1);
        assertEquals(serviceConfig.getRegistryUrls().size(), 1);

    }

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
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("export should not empty"));
        }

        // protocol not exist
        serviceConfig.setProtocol(mockProtocolConfig("notExist"));
        serviceConfig.setExport("notExist" + ":" + 0);
        try {
            serviceConfig.export();
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("get extension fail"));
        }

        // service already exist
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0);
        serviceConfig.export();
        assertTrue(serviceConfig.getExported().get());
    }

    public void testMethodConfig() {
        List<MethodConfig> methods = new ArrayList<>();
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
        assertEquals(serviceConfig.getRegistryUrls().size(), 1);
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

    public void testMultiProtocol() {
        serviceConfig.setProtocols(getMultiProtocols(MotanConstants.PROTOCOL_INJVM, MotanConstants.PROTOCOL_MOTAN));
        serviceConfig.setExport(MotanConstants.PROTOCOL_INJVM + ":" + 0 + "," + MotanConstants.PROTOCOL_MOTAN + ":8002");
        serviceConfig.export();
        assertEquals(serviceConfig.getExporters().size(), 2);

    }

    public void testMultiRegistry() {
        serviceConfig.setRegistries(getMultiRegister(MotanConstants.REGISTRY_PROTOCOL_LOCAL, MotanConstants.REGISTRY_PROTOCOL_ZOOKEEPER));
        serviceConfig.loadRegistryUrls();
        assertEquals(2, serviceConfig.getRegistryUrls().size());
    }

    public void testMultiGroup() {
        serviceConfig.setGroup("motan-test1, motan-test2");
        serviceConfig.export();
        assertEquals(2, serviceConfig.getExporters().size());
    }

    public void testUnexport() {
        testExport();
        serviceConfig.unexport();
        assertFalse(serviceConfig.getExported().get());
        assertEquals(serviceConfig.getExporters().size(), 0);
    }

    public void testAdditionalGroup() throws Exception {
        serviceConfig.setGroup("");
        serviceConfig.export();
        assertEquals(1, serviceConfig.getExporters().size());
        assertEquals(URLParamType.group.getValue(), serviceConfig.getExporters().get(0).getUrl().getGroup());

        // default group with additional env
        reset();
        String envGroup = "envGroup";
        getModifiableEnvironment().put(ENV_ADDITIONAL_GROUP, envGroup);
        serviceConfig.setGroup("");
        serviceConfig.export();
        assertEquals(1, serviceConfig.getExporters().size());
        assertEquals(envGroup, serviceConfig.getExporters().get(0).getUrl().getGroup());

        // group + additional group
        reset();
        serviceConfig.export();
        assertEquals(2, serviceConfig.getExporters().size());
        checkGroupNames(group, envGroup);

        // multi group with additional multi group
        reset();
        envGroup = "envGroup1, envGroup2, sameGroup";
        getModifiableEnvironment().put(ENV_ADDITIONAL_GROUP, envGroup);
        serviceConfig.setGroup("motan-test1, motan-test2, sameGroup");
        serviceConfig.export();
        assertEquals(5, serviceConfig.getExporters().size());
        checkGroupNames("envGroup1", "envGroup2", "sameGroup", "motan-test1", "motan-test2");

        getModifiableEnvironment().remove(ENV_ADDITIONAL_GROUP);
    }

    private void checkGroupNames(String... expectGroupNames) {
        Set<String> groupNames = new HashSet<>();
        for (Exporter<?> exporter : serviceConfig.getExporters()) {
            groupNames.add(exporter.getUrl().getGroup());
        }
        assertEquals(expectGroupNames.length, groupNames.size());
        for (String name : expectGroupNames) {
            assertTrue(groupNames.contains(name));
        }
    }

    private void reset() throws Exception {
        tearDown();
        setUp();
    }

    public void testAppendGroupSuffix() throws Exception {
        String suffix = "-preview";
        // === without environment ===
        getModifiableEnvironment().remove(ENV_RPC_REG_GROUP_SUFFIX);
        // single group

        serviceConfig.setGroup("motan-test");
        serviceConfig.export();
        checkGroup(serviceConfig, Collections.singletonList("motan-test"));

        // multi groups
        reset();
        serviceConfig.setGroup("motan-test,motan-test2");
        serviceConfig.export();
        checkGroup(serviceConfig, Arrays.asList("motan-test", "motan-test2"));

        // === with suffix environment ===
        getModifiableEnvironment().put(ENV_RPC_REG_GROUP_SUFFIX, suffix);
        // inJvm protocol not append suffix
        reset();
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_INJVM));
        serviceConfig.export();
        checkGroup(serviceConfig, Collections.singletonList(group)); // group from setup

        // other protocol append suffix
        resetMotan2Protocol();
        serviceConfig.export();
        checkGroup(serviceConfig, Collections.singletonList(group + suffix));

        // multi groups
        resetMotan2Protocol();
        serviceConfig.setGroup("motan-test,motan-test2");
        serviceConfig.export();
        checkGroup(serviceConfig, Arrays.asList("motan-test" + suffix, "motan-test2" + suffix));

        // group already with suffix
        resetMotan2Protocol();
        serviceConfig.setGroup("motan-test" + suffix + ",motan-test2" + suffix);
        serviceConfig.export();
        checkGroup(serviceConfig, Arrays.asList("motan-test" + suffix, "motan-test2" + suffix));

        getModifiableEnvironment().remove(ENV_RPC_REG_GROUP_SUFFIX); // clear
    }

    private void resetMotan2Protocol() throws Exception {
        reset();
        serviceConfig.setProtocol(mockProtocolConfig(MotanConstants.PROTOCOL_MOTAN2));
        serviceConfig.setExport(MotanConstants.PROTOCOL_MOTAN2 + ":" + 8010);
    }

    private void checkGroup(ServiceConfig<?> serviceConfig, List<String> groups) {
        assertEquals(groups.size(), serviceConfig.getExporters().size());
        for (Exporter<?> exporter : serviceConfig.getExporters()) {
            assertTrue(groups.contains(exporter.getUrl().getGroup()));
        }
    }

    public void testSandboxMode() throws Exception {
        // 沙箱模式
        JSONArray serviceGroups = new JSONArray();
        JSONObject serviceGroup1 = new JSONObject();
        serviceGroup1.put("group", "sandbox1, sandbox2");
        serviceGroup1.put("service", "com.weibo.api.motan.protocol.*"); // matched by regular expressions
        serviceGroups.add(serviceGroup1);
        JSONObject serviceGroup2 = new JSONObject();
        serviceGroup2.put("group", "sandbox-nomatch");
        serviceGroup2.put("service", "com.weibo.api.motan.other.NotMatchService"); // will not match
        serviceGroups.add(serviceGroup2);

        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_SERVER_MODE, "sandbox");
        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS, serviceGroups.toJSONString());
        serviceConfig.export();
        assertEquals(2, serviceConfig.getExporters().size());
        assertEquals("sandbox1", serviceConfig.getExporters().get(0).getUrl().getGroup());
        assertEquals("sandbox2", serviceConfig.getExporters().get(1).getUrl().getGroup());

        clearSandboxEnv();
    }

    public void testSandboxModeException() throws Exception {
        JSONArray serviceGroups = new JSONArray();
        JSONObject serviceGroup1 = new JSONObject();
        serviceGroup1.put("group", "sandbox1");
        serviceGroup1.put("service", "com.weibo.api.motan.other.NotMatchService");
        serviceGroups.add(serviceGroup1);
        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS, serviceGroups.toJSONString());

        // MOTAN_CHANGE_REG_GROUPS will be ignored when server mode is not sandbox
        serviceConfig.export();
        assertEquals(1, serviceConfig.getExporters().size());
        reset();

        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_SERVER_MODE, "sandbox");
        try {
            serviceConfig.export();
            fail();
        } catch (MotanServiceException e) {
            assertTrue(e.getOriginMessage().contains("can not find sandbox group name in sandbox mode"));
        }

        clearSandboxEnv();
    }

    public void testSandboxGroup() throws Exception {
        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_SERVER_MODE, "sandbox");
        serviceConfig.setSandboxGroups("suffix:-sandbox"); // change to suffix group name
        serviceConfig.export();
        assertEquals(1, serviceConfig.getExporters().size());
        assertEquals(serviceConfig.getGroup() + "-sandbox", serviceConfig.getExporters().get(0).getUrl().getGroup());
        clearSandboxEnv();
    }

    private void clearSandboxEnv() throws Exception {
        ServiceConfig.clearChangeGroupFromEnv();
        getModifiableEnvironment().remove(MotanConstants.ENV_MOTAN_SERVER_MODE);
        getModifiableEnvironment().remove(MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS);
    }

}
