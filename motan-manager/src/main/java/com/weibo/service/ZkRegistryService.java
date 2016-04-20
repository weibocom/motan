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

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.dao.ZookeeperClient;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("registryService")
public class ZkRegistryService implements RegistryService {
    private ZkClient zkClient;

    private ZookeeperClient zookeeperClient;

    public ZkRegistryService() {
        zookeeperClient = ZookeeperClient.getInstance();
        zkClient = zookeeperClient.getZkClient();
    }

    /**
     * Unit Test中使用
     */

    public ZkRegistryService(ZkClient zkClient, ZookeeperClient zookeeperClient) {
        this.zkClient = zkClient;
        this.zookeeperClient = zookeeperClient;
    }

    @Override
    public List<String> getGroups() {
        return zookeeperClient.getChildren(MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE);
    }

    @Override
    public List<String> getServicesByGroup(String group) {

        List<String> services = zookeeperClient.getChildren(toGroupPath(group));
        services.remove("command");
        return services;
    }

    @Override
    public List<JSONObject> getNodes(String group, String service, String nodeType) {
        List<JSONObject> result = new ArrayList<JSONObject>();
        List<String> nodes = zookeeperClient.getChildren(toNodeTypePath(group, service, nodeType));
        for (String nodeName : nodes) {
            JSONObject node = new JSONObject();
            String info = zkClient.readData(toNodePath(group, service, nodeType, nodeName), true);
            node.put("host", nodeName);
            node.put("info", info);
            result.add(node);
        }
        return result;
    }

    @Override
    public List<JSONObject> getAllNodes(String group) {
        List<JSONObject> results = new ArrayList<JSONObject>();
        List<String> services = getServicesByGroup(group);
        for (String serviceName : services) {
            JSONObject service = new JSONObject();
            service.put("service", serviceName);
            List<JSONObject> serverNode = getNodes(group, serviceName, "server");
            service.put("server", serverNode);
            List<JSONObject> clientNode = getNodes(group, serviceName, "client");
            service.put("client", clientNode);
            results.add(service);
        }
        return results;
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
