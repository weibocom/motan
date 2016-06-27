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

package com.weibo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.registry.support.command.RpcCommand;
import com.weibo.api.motan.registry.support.command.RpcCommandUtil;
import com.weibo.utils.ZkClientWrapper;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
@Lazy
public class ZookeeperCommandService extends AbstractCommandService {

    @Autowired
    private ZkClientWrapper clientWrapper;
    private ZkClient zkClient;

    @PostConstruct
    void init() {
        zkClient = clientWrapper.getZkClient();
    }

    /**
     * 获取所有指令
     *
     * @return
     */
    @Override
    public List<JSONObject> getAllCommands() {
        List<JSONObject> commands = new ArrayList<JSONObject>();
        List<String> groups = getChildren(MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE);
        for (String group : groups) {
            JSONObject node = new JSONObject();
            String command = getCommands(group);
            if (command != null) {
                node.put("group", group);
                node.put("command", RpcCommandUtil.stringToCommand(command));
                commands.add(node);
            }
        }
        return commands;
    }

    private List<String> getChildren(String path) {
        List<String> children = new ArrayList<String>();
        if (zkClient.exists(path)) {
            children = zkClient.getChildren(path);
        }
        return children;
    }

    /**
     * 获取指定group的指令列表
     *
     * @param groupName
     * @return
     */
    @Override
    public String getCommands(String groupName) {
        return zkClient.readData(getCommandPath(groupName), true);
    }

    private String getCommandPath(String groupName) {
        return MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE + MotanConstants.PATH_SEPARATOR + groupName + MotanConstants.ZOOKEEPER_REGISTRY_COMMAND;
    }

    /**
     * 更新指定group的指令列表
     *
     * @param command
     * @param group
     * @return
     */
    @Override
    public boolean setCommand(String group, RpcCommand command) {
        String path = getCommandPath(group);
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path, true);
        }
        try {
            zkClient.writeData(path, RpcCommandUtil.commandToString(command));
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
