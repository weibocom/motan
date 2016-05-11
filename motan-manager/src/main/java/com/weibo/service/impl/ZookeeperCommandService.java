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
import com.weibo.api.motan.registry.support.command.RpcCommand.ClientCommand;
import com.weibo.api.motan.registry.support.command.RpcCommandUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.dao.OperationRecordMapper;
import com.weibo.model.OperationRecord;
import com.weibo.service.CommandService;
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
public class ZookeeperCommandService implements CommandService {

    @Autowired(required = false)
    private OperationRecordMapper recordMapper;

    @Autowired
    private ZkClientWrapper clientWrapper;
    private ZkClient zkClient;

    public ZookeeperCommandService() {
    }

    public ZookeeperCommandService(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

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

    /**
     * 向指定group添加指令
     *
     * @param group
     * @param command
     * @return
     */
    @Override
    public boolean addCommand(String group, ClientCommand command) {
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            remoteCommand = new RpcCommand();
        }
        List<ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            clientCommandList = new ArrayList<ClientCommand>();
        }

        // 该方法只在流量切换界面被调用，此时指令序号默认是0
        int index = getRpcCommandMaxIndex(remoteCommand);
        command.setIndex(index + 1);
        clientCommandList.add(command);
        remoteCommand.setClientCommandList(clientCommandList);

        return setCommand(group, remoteCommand);
    }

    /**
     * 更新指定group的某条指令
     *
     * @param command
     * @param group
     * @return
     */
    @Override
    public boolean updateCommand(String group, ClientCommand command) {
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            LoggerUtil.info("update failed, command not found");
            return false;
        }
        List<ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            LoggerUtil.info("update failed, command not found");
            return false;
        }
        boolean found = false;
        for (ClientCommand cmd : clientCommandList) {
            if (cmd.getIndex().equals(command.getIndex())) {
                clientCommandList.remove(cmd);
                clientCommandList.add(command);
                found = true;
                break;
            }
        }
        if (!found) {
            LoggerUtil.info("update failed, command not found");
            return false;
        }
        remoteCommand.setClientCommandList(clientCommandList);
        return setCommand(group, remoteCommand);
    }

    /**
     * 删除指定group的某条指令
     *
     * @param group
     * @param index
     * @return
     */
    @Override
    public boolean deleteCommand(String group, int index) {
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            LoggerUtil.info("delete failed, command not found");
            return false;
        }
        List<ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            LoggerUtil.info("delete failed, command not found");
            return false;
        }
        boolean found = false;
        for (ClientCommand cmd : clientCommandList) {
            if (cmd.getIndex() == index) {
                clientCommandList.remove(cmd);
                found = true;
                break;
            }
        }
        if (!found) {
            LoggerUtil.info("delete failed, command not found");
            return false;
        }
        remoteCommand.setClientCommandList(clientCommandList);

        return setCommand(group, remoteCommand);
    }

    /**
     * 获取指令集中最大的指令序号
     *
     * @param rpcCommand
     * @return
     */
    @Override
    public int getRpcCommandMaxIndex(RpcCommand rpcCommand) {
        int i = 0;
        List<ClientCommand> clientCommandList = rpcCommand.getClientCommandList();
        if (clientCommandList == null) {
            return i;
        }
        for (ClientCommand command : clientCommandList) {
            int index = command.getIndex();
            i = Math.max(i, index);
        }
        return i;
    }

    /**
     * 预览指令
     *
     * @param group
     * @param clientCommand
     * @param previewIP
     * @return
     */
    @Override
    public List<JSONObject> previewCommand(String group, ClientCommand clientCommand, String previewIP) {
        // TODO: 2016/3/31 0031
        return null;
    }

    /**
     * 根据group和clientCommand生成指令
     *
     * @param group
     * @param clientCommand
     * @return
     */
    @Override
    public RpcCommand buildCommand(String group, ClientCommand clientCommand) {
        RpcCommand rpcCommand = new RpcCommand();
        List<ClientCommand> commandList = new ArrayList<ClientCommand>();
        commandList.add(clientCommand);
        rpcCommand.setClientCommandList(commandList);
        return rpcCommand;
    }

    @Override
    public List<OperationRecord> getAllRecord() {
        List<OperationRecord> records = new ArrayList<OperationRecord>();
        if (recordMapper != null) {
            records = recordMapper.selectAll();
        } else {
            return null;
        }
        return records;
    }

    private String getCommandPath(String groupName) {
        return MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE + MotanConstants.PATH_SEPARATOR + groupName + MotanConstants.ZOOKEEPER_REGISTRY_COMMAND;
    }

    private List<String> getChildren(String path) {
        List<String> children = new ArrayList<String>();
        if (zkClient.exists(path)) {
            children = zkClient.getChildren(path);
        }
        return children;
    }
}
