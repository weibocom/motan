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
import com.weibo.service.RegistryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * Created by Zhang Yu on 2015/11/2 0002.
 */
@RestController
@RequestMapping(value = "/api")
public class ServerController {

    @Resource(name = "${registry.type}" + "RegistryService")
    private RegistryService registryService;

    /**
     * 获取所有group分组名称
     *
     * @return
     */
    @RequestMapping(value = "/groups", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getAllGroups() {
        List<String> result = registryService.getGroups();
        return new ResponseEntity<List<String>>(result, HttpStatus.OK);
    }

    /**
     * 获取group的所有service接口类名
     *
     * @param group
     * @return
     */
    @RequestMapping(value = "/{group}/services", method = RequestMethod.GET)
    public ResponseEntity<List<String>> getServicesByGroup(@PathVariable("group") String group) {
        if (StringUtils.isEmpty(group)) {
            return new ResponseEntity<List<String>>(HttpStatus.BAD_REQUEST);
        }

        List<String> services = registryService.getServicesByGroup(group);
        return new ResponseEntity<List<String>>(services, HttpStatus.OK);
    }

    /**
     * 获取group下某个service的节点信息
     *
     * @param group
     * @param service
     * @return
     */
    @RequestMapping(value = "/{group}/{service}/{nodeType}/nodes", method = RequestMethod.GET)
    public ResponseEntity<List<JSONObject>> getServiceNodes(@PathVariable("group") String group, @PathVariable("service") String service, @PathVariable("nodeType") String nodeType) {
        if (StringUtils.isEmpty(group) || StringUtils.isEmpty(service)) {
            return new ResponseEntity<List<JSONObject>>(HttpStatus.BAD_REQUEST);
        }

        List<JSONObject> result = registryService.getNodes(group, service, nodeType);
        return new ResponseEntity<List<JSONObject>>(result, HttpStatus.OK);
    }

    /**
     * 获取group下所有service的节点信息
     *
     * @param group
     * @return
     */
    @RequestMapping(value = "/{group}/nodes", method = RequestMethod.GET)
    public ResponseEntity<List<JSONObject>> getAllNodes(@PathVariable("group") String group) {
        if (StringUtils.isEmpty(group)) {
            return new ResponseEntity<List<JSONObject>>(HttpStatus.BAD_REQUEST);
        }

        List<JSONObject> results = registryService.getAllNodes(group);
        return new ResponseEntity<List<JSONObject>>(results, HttpStatus.OK);
    }
}
