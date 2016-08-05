/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.motan.demo.client;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.motan.demo.service.MotanDemoService;

@RestController
public class HelloController {

    @MotanReferer(basicReferer = "motantestClientBasicConfig", group = "testgroup", directUrl = "127.0.0.1:8002")
    MotanDemoService service;

    @RequestMapping("/")
    @ResponseBody
    public String home() {
        String result = service.hello("test");
        return result;
    }
}
