package com.weibo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.registry.zookeeper.ZookeeperRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.service.RegistryService;
import org.I0Itec.zkclient.ZkClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class ZkRegistryServiceTest {
    private RegistryService registryService;
    private EmbeddedZookeeper embeddedZookeeper;
    private ZkClient zkClient;
    private String group = "default_rpc";
    private String service1 = "com.weibo.motan.demoService";
    private String service2 = "com.weibo.motan.demoService2";

    @Before
    public void setUp() throws Exception {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);
        int port = Integer.parseInt(properties.getProperty("clientPort"));
        in.close();

        URL zkUrl = new URL("zookeeper", "127.0.0.1", port, "com.weibo.api.motan.registry.RegistryService");
        URL clientUrl = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 0, "com.weibo.motan.demoService");
        URL url1 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8001, service1);
        URL url2 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8002, service1);
        URL url3 = new URL(MotanConstants.PROTOCOL_MOTAN, "127.0.0.1", 8003, service2);

        embeddedZookeeper = new EmbeddedZookeeper();
        embeddedZookeeper.start();
        Thread.sleep(1000);
        zkClient = new ZkClient("127.0.0.1:" + port, 5000);
        ZookeeperRegistry registry = new ZookeeperRegistry(zkUrl, zkClient);

        registry.register(url1);
        registry.register(url2);
        registry.register(url3);
        registry.subscribe(clientUrl, null);

        registryService = new ZookeeperRegistryService(zkClient);
    }

    @After
    public void tearDown() throws Exception {
        zkClient.deleteRecursive(MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE);
        embeddedZookeeper = null;
    }

    @Test
    public void getGroups() throws Exception {
        List<String> groups = registryService.getGroups();
        assertTrue(groups.size() == 1);
        assertTrue(groups.contains(group));
    }

    @Test
    public void getServicesByGroup() throws Exception {
        List<String> services = registryService.getServicesByGroup(group);
        assertTrue(services.size() == 2);
        assertTrue(services.contains(service1));
        assertTrue(services.contains(service2));
    }

    @Test
    public void getNodes() throws Exception {
        List<JSONObject> nodes = registryService.getNodes(group, service1, "unavailableServer");
        assertTrue(nodes.size() == 2);

        List<String> addresses = new ArrayList<String>();
        for (JSONObject node : nodes) {
            addresses.add((String) node.get("host"));
        }
        assertTrue(addresses.contains("127.0.0.1:8001"));
        assertTrue(addresses.contains("127.0.0.1:8002"));
    }

    @Test
    public void getAllNodes() throws Exception {
        List<JSONObject> nodes = registryService.getAllNodes(group);
        assertTrue(nodes.size() == 2);
    }

}