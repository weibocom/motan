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

package com.weibo.controller;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.registry.support.command.RpcCommand.ClientCommand;
import com.weibo.model.OperationRecord;
import com.weibo.service.CommandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by Zhang Yu on 2015/12/24 0024 18:50.
 */
@RestController
@RequestMapping(value = "/api/commands")
public class CommandController {

    @Resource(name = "${registry.type}" + "CommandService")
    private CommandService commandService;

    /**
     * 获取所有指令
     *
     * @return
     */
    @RequestMapping(value = "", method = RequestMethod.GET)
    public ResponseEntity<List<JSONObject>> getAllCommands() {
        List<JSONObject> result = commandService.getAllCommands();
        return new ResponseEntity<List<JSONObject>>(result, HttpStatus.OK);
    }

    /**
     * 获取指定group的指令列表
     *
     * @param group
     * @return
     */
    @RequestMapping(value = "/{group}", method = RequestMethod.GET)
    public ResponseEntity<String> getCommandsByGroup(@PathVariable("group") String group) {
        if (StringUtils.isEmpty(group)) {
            return new ResponseEntity<String>(HttpStatus.BAD_REQUEST);
        }

        String result = commandService.getCommands(group);
        return new ResponseEntity<String>(result, HttpStatus.OK);
    }

    /**
     * 向指定group添加指令
     *
     * @param group
     * @param clientCommand
     * @return
     */
    @RequestMapping(value = "/{group}", method = RequestMethod.POST)
    public ResponseEntity<Boolean> addCommand(@PathVariable("group") String group, @RequestBody ClientCommand clientCommand) {
        if (StringUtils.isEmpty(group) || clientCommand == null) {
            return new ResponseEntity<Boolean>(HttpStatus.BAD_REQUEST);
        }

        boolean result = commandService.addCommand(group, clientCommand);
        HttpStatus status;
        if (result) {
            status = HttpStatus.OK;
        } else {
            status = HttpStatus.NOT_MODIFIED;
        }
        return new ResponseEntity<Boolean>(result, status);
    }

    /**
     * 更新指定group的某条指令
     *
     * @param group
     * @param clientCommand
     * @return
     */
    @RequestMapping(value = "/{group}", method = RequestMethod.PUT)
    public ResponseEntity<Boolean> updateCommand(@PathVariable("group") String group, @RequestBody ClientCommand clientCommand) {
        if (StringUtils.isEmpty(group) || clientCommand == null) {
            return new ResponseEntity<Boolean>(HttpStatus.BAD_REQUEST);
        }


        boolean result = commandService.updateCommand(group, clientCommand);
        HttpStatus status;
        if (result) {
            status = HttpStatus.OK;
        } else {
            status = HttpStatus.NOT_MODIFIED;
        }
        return new ResponseEntity<Boolean>(result, status);
    }


    /**
     * 删除指定group的某条指令
     *
     * @param group
     * @param index
     * @return
     */
    @RequestMapping(value = "/{group}/{index}", method = RequestMethod.DELETE)
    public ResponseEntity<Boolean> deleteCommand(@PathVariable("group") String group, @PathVariable("index") int index) {
        if (StringUtils.isEmpty(group)) {
            return new ResponseEntity<Boolean>(HttpStatus.BAD_REQUEST);
        }

        boolean result = commandService.deleteCommand(group, index);
        HttpStatus status;
        if (result) {
            status = HttpStatus.OK;
        } else {
            status = HttpStatus.NOT_MODIFIED;
        }
        return new ResponseEntity<Boolean>(result, status);
    }

    /**
     * 预览指令
     *
     * @param group
     * @param clientCommand
     * @param previewIP
     * @return
     */
    @RequestMapping(value = "/{group}/preview", method = RequestMethod.POST)
    public ResponseEntity<List<JSONObject>> previewCommand(
            @PathVariable("group") String group,
            @RequestBody ClientCommand clientCommand,
            @RequestParam(value = "previewIP", required = false) String previewIP) {
        if (StringUtils.isEmpty(group) || clientCommand == null) {
            return new ResponseEntity<List<JSONObject>>(HttpStatus.BAD_REQUEST);
        }

        List<JSONObject> results = commandService.previewCommand(group, clientCommand, previewIP);
        return new ResponseEntity<List<JSONObject>>(results, HttpStatus.OK);
    }


    @RequestMapping(value = "/operationRecord", method = RequestMethod.GET)
    public ResponseEntity<List<OperationRecord>> getAllRecord() {
        List<OperationRecord> results = commandService.getAllRecord();
        if (results == null) {
            return new ResponseEntity<List<OperationRecord>>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<List<OperationRecord>>(results, HttpStatus.OK);
    }
}
