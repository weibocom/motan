package com.weibo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ecwid.consul.v1.ConsulClient;
import com.weibo.api.motan.registry.support.command.RpcCommand;
import com.weibo.model.OperationRecord;
import com.weibo.service.CommandService;
import com.weibo.utils.ConsulClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
@Lazy
public class ConsulCommandService implements CommandService {
    @Autowired
    private ConsulClientWrapper clientWrapper;
    private ConsulClient consulClient;

    @PostConstruct
    void init() {
        consulClient = clientWrapper.getConsulClient();
    }

    @Override
    public List<JSONObject> getAllCommands() {
        return null;
    }

    @Override
    public String getCommands(String groupName) {
        return null;
    }

    @Override
    public boolean setCommand(String group, RpcCommand command) {
        return false;
    }

    @Override
    public boolean addCommand(String group, RpcCommand.ClientCommand command) {
        return false;
    }

    @Override
    public boolean updateCommand(String group, RpcCommand.ClientCommand command) {
        return false;
    }

    @Override
    public boolean deleteCommand(String group, int index) {
        return false;
    }

    @Override
    public int getRpcCommandMaxIndex(RpcCommand rpcCommand) {
        return 0;
    }

    @Override
    public List<JSONObject> previewCommand(String group, RpcCommand.ClientCommand clientCommand, String previewIP) {
        return null;
    }

    @Override
    public RpcCommand buildCommand(String group, RpcCommand.ClientCommand clientCommand) {
        return null;
    }

    @Override
    public List<OperationRecord> getAllRecord() {
        return null;
    }
}
