package com.weibo.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.registry.support.command.RpcCommand;
import com.weibo.api.motan.registry.support.command.RpcCommandUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.dao.OperationRecordMapper;
import com.weibo.model.OperationRecord;
import com.weibo.service.CommandService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractCommandService implements CommandService {

    @Autowired(required = false)
    private OperationRecordMapper recordMapper;

    /**
     * 向指定group添加指令
     *
     * @param group
     * @param command
     * @return
     */
    @Override
    public boolean addCommand(String group, RpcCommand.ClientCommand command) {
        LoggerUtil.info(String.format("add command: group=%s, command=%s: ", group, JSON.toJSONString(command)));
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            remoteCommand = new RpcCommand();
        }
        List<RpcCommand.ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            clientCommandList = new ArrayList<RpcCommand.ClientCommand>();
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
    public boolean updateCommand(String group, RpcCommand.ClientCommand command) {
        LoggerUtil.info(String.format("update command: group=%s, command=%s: ", group, JSON.toJSONString(command)));
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            LoggerUtil.info("update failed, command not found");
            return false;
        }
        List<RpcCommand.ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            LoggerUtil.info("update failed, command not found");
            return false;
        }
        boolean found = false;
        for (RpcCommand.ClientCommand cmd : clientCommandList) {
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
        LoggerUtil.info(String.format("delete command: group=%s, index=%d: ", group, index));
        RpcCommand remoteCommand = RpcCommandUtil.stringToCommand(getCommands(group));
        if (remoteCommand == null) {
            LoggerUtil.info("delete failed, command not found");
            return false;
        }
        List<RpcCommand.ClientCommand> clientCommandList = remoteCommand.getClientCommandList();
        if (clientCommandList == null) {
            LoggerUtil.info("delete failed, command not found");
            return false;
        }
        boolean found = false;
        for (RpcCommand.ClientCommand cmd : clientCommandList) {
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
        return 0;
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
    public List<JSONObject> previewCommand(String group, RpcCommand.ClientCommand clientCommand, String previewIP) {
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
    public RpcCommand buildCommand(String group, RpcCommand.ClientCommand clientCommand) {
        RpcCommand rpcCommand = new RpcCommand();
        List<RpcCommand.ClientCommand> commandList = new ArrayList<RpcCommand.ClientCommand>();
        commandList.add(clientCommand);
        rpcCommand.setClientCommandList(commandList);
        return rpcCommand;
    }

    /**
     * 获取指令操作记录
     *
     * @return
     */
    @Override
    public List<OperationRecord> getAllRecord() {
        List<OperationRecord> records;
        if (recordMapper != null) {
            records = recordMapper.selectAll();
        } else {
            return null;
        }
        return records;
    }
}
