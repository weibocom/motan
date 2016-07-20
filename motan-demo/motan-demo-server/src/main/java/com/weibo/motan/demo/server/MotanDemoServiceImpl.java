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

package com.weibo.motan.demo.server;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.motan.demo.service.MotanDemoService;

@MotanService(export = "8002")
public class MotanDemoServiceImpl implements MotanDemoService {

    public String hello(String name) {
        System.out.println(name);
        return "Hello " + name + "!";
    }

}
