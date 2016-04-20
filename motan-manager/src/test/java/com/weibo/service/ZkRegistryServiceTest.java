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

package com.weibo.service;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.dao.ZookeeperClient;
import org.I0Itec.zkclient.ZkClient;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ZkRegistryServiceTest {
    public static JUnit4Mockery mockery = null;
    private static ZkClient mockZkClient = null;
    private static ZookeeperClient mockZooClient = null;
    final String group = "motan-demo-rpc";
    final String service = "com.weibo.motan.demo.service.MotanDemoService";
    private RegistryService registryService = null;

    @Before
    public void setUp() throws Exception {
        mockery = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        mockZkClient = mockery.mock(ZkClient.class);
        mockZooClient = mockery.mock(ZookeeperClient.class);
        registryService = new ZkRegistryService(mockZkClient, mockZooClient);


        final List<String> groups = new ArrayList<String>();
        groups.add(group);

        final List<String> services = new ArrayList<String>();
        services.add(service);

        final List<String> serverNodes = new ArrayList<String>();
        serverNodes.add("127.0.0.1");
        final List<String> clientNodes = new ArrayList<String>();
        clientNodes.add("127.0.0.1");

        mockery.checking(new Expectations() {
            {
                allowing(mockZooClient).getChildren("/motan");
                will(returnValue(groups));
                allowing(mockZooClient).getChildren(toGroupPath(group));
                will(returnValue(services));
                allowing(mockZooClient).getChildren(toNodeTypePath(group, service, "server"));
                will(returnValue(serverNodes));
                allowing(mockZooClient).getChildren(toNodeTypePath(group, service, "client"));
                will(returnValue(clientNodes));
                allowing(any(ZkClient.class)).method("exists");
                will(returnValue(true));
                allowing(any(ZkClient.class)).method("readData");
                will(returnValue("motan://127.0.0.1:8001/com.weibo.motan.demo.service.MotanDemoService?export=demoMotan:8002&protocol=motan&module=motan-demo-rpc&application=myMotanDemo&refreshTimestamp=1459216241466&maxContentLength=1048576&id=com.weibo.api.motan.config.springsupport.ServiceConfigBean&maxServerConnection=80000&maxWorkerThread=800&accessLog=true&requestTimeout=200&isDefault=true&minWorkerThread=20&group=motan-demo-rpc&nodeType=service&shareChannel=true&"));
            }
        });
    }

    @Test
    public void testGetGroups() throws Exception {
        registryService.getGroups();
    }

    @Test
    public void testGetServicesByGroup() throws Exception {
        registryService.getServicesByGroup(group);
    }

    @Test
    public void testGetNodes() throws Exception {
        registryService.getNodes(group, service, "server");
    }

    @Test
    public void testGetAllNodes() throws Exception {
        registryService.getAllNodes(group);
    }

    private String toGroupPath(String group) {
        return MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE + MotanConstants.PATH_SEPARATOR + group;
    }

    private String toServicePath(String group, String service) {
        return toGroupPath(group) + MotanConstants.PATH_SEPARATOR + service;
    }

    private String toNodeTypePath(String group, String service, String nodeType) {
        return toServicePath(group, service) + MotanConstants.PATH_SEPARATOR + nodeType;
    }

    private String toNodePath(String group, String service, String nodeType, String node) {
        return toNodeTypePath(group, service, nodeType) + MotanConstants.PATH_SEPARATOR + node;
    }
}