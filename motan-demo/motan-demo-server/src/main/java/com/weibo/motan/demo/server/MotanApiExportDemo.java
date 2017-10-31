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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.motan.demo.service.MotanDemoService;

public class MotanApiExportDemo {

    public static void main(String[] args) throws InterruptedException {
        ServiceConfig<MotanDemoService> motanDemoService = new ServiceConfig<MotanDemoService>();

        // 设置接口及实现类
        motanDemoService.setInterface(MotanDemoService.class);
        motanDemoService.setRef(new MotanDemoServiceImpl());

        // 配置服务的group以及版本号
        motanDemoService.setGroup("motan-demo-rpc");
        motanDemoService.setVersion("1.0");

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();

        //use local registry
        registry.setRegProtocol("local");

        // use ZooKeeper registry
//        registry.setRegProtocol("zookeeper");
//        registry.setAddress("127.0.0.1:2181");

        // registry.setCheck("false"); //是否检查是否注册成功
        motanDemoService.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan");
        protocol.setName("motan");
        motanDemoService.setProtocol(protocol);

        motanDemoService.setExport("motan:8002");
        motanDemoService.export();

        MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true);

        System.out.println("server start...");
    }

}
