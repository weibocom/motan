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
import com.weibo.api.motan.registry.support.command.RpcCommand;
import com.weibo.api.motan.registry.support.command.RpcCommand.ClientCommand;
import com.weibo.model.OperationRecord;

import java.util.List;

/**
 * Created by Zhang Yu on 2015/12/24 0024 18:52.
 */
public interface CommandService {
    List<JSONObject> getAllCommands();

    String getCommands(String groupName);

    boolean setCommand(String group, RpcCommand command);

    boolean addCommand(String group, ClientCommand command);

    boolean updateCommand(String group, ClientCommand command);

    boolean deleteCommand(String group, int index);

    int getRpcCommandMaxIndex(RpcCommand rpcCommand);

    List<JSONObject> previewCommand(String group, ClientCommand clientCommand, String previewIP);

    RpcCommand buildCommand(String group, ClientCommand clientCommand);

    List<OperationRecord> getAllRecord();
}
