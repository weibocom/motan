package com.weibo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.weibo.api.motan.registry.consul.ConsulConstants;
import com.weibo.api.motan.registry.support.command.RpcCommand;
import com.weibo.api.motan.registry.support.command.RpcCommandUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.utils.ConsulClientWrapper;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
@Lazy
public class ConsulCommandService extends AbstractCommandService {
    @Autowired
    private ConsulClientWrapper clientWrapper;
    private ConsulClient consulClient;

    @PostConstruct
    void init() {
        consulClient = clientWrapper.getConsulClient();
    }

    /**
     * 获取所有指令
     *
     * @return
     */
    @Override
    public List<JSONObject> getAllCommands() {
        List<JSONObject> commands = new ArrayList<JSONObject>();
        Response<List<GetValue>> response = consulClient.getKVValues(ConsulConstants.CONSUL_MOTAN_COMMAND);
        List<GetValue> values = response.getValue();
        if (values != null) {
            for (GetValue value : values) {
                JSONObject node = new JSONObject();
                if (value.getValue() == null) {
                    continue;
                }
                String group = value.getKey().substring(ConsulConstants.CONSUL_MOTAN_COMMAND.length());
                String command = new String(Base64.decodeBase64(value.getValue()));
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
        Response<GetValue> response = consulClient.getKVValue(ConsulConstants.CONSUL_MOTAN_COMMAND + groupName);
        GetValue value = response.getValue();
        String command = "";
        if (value != null && value.getValue() != null) {
            command = new String(Base64.decodeBase64(value.getValue()));
        }
        return command;
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
        LoggerUtil.info(String.format("set command: group=%s, command=%s: ", group, JSON.toJSONString(command)));
        List<RpcCommand.ClientCommand> newCommandList = new ArrayList<RpcCommand.ClientCommand>();
        for (RpcCommand.ClientCommand clientCommand : command.getClientCommandList()) {
            List<String> newMergeGroups = new ArrayList<String>();
            for (String mergeGroup : clientCommand.getMergeGroups()) {
                mergeGroup = removeGroupNamePrefix(mergeGroup);
                newMergeGroups.add(mergeGroup);
            }
            clientCommand.setMergeGroups(newMergeGroups);
            newCommandList.add(clientCommand);
        }
        command.setClientCommandList(newCommandList);

        Response<Boolean> response = consulClient.setKVValue(
                ConsulConstants.CONSUL_MOTAN_COMMAND + removeDatacenterPrefix(group),
                RpcCommandUtil.commandToString(command));
        return response.getValue();
    }

    /**
     * 去除group的datacenter前缀
     *
     * @param group
     * @return
     */
    private String removeDatacenterPrefix(String group) {
        int index = group.indexOf(":");
        if (index > 0) {
            return group.substring(group.indexOf(":") + 1);
        } else {
            return group;
        }
    }

    /**
     * 去除group的motan标识前缀
     *
     * @param group
     * @return
     */
    private String removeGroupNamePrefix(String group) {
        if (group.contains(ConsulConstants.CONSUL_SERVICE_MOTAN_PRE)) {
            return removeDatacenterPrefix(group).substring(ConsulConstants.CONSUL_SERVICE_MOTAN_PRE.length());
        } else {
            return group;
        }
    }
}
