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

package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.motan.demo.service.MotanDemoService;

public class MotanApiClientDemo {

    public static void main(String[] args) {
        RefererConfig<MotanDemoService> motanDemoServiceReferer = new RefererConfig<MotanDemoService>();

        // 设置接口及实现类
        motanDemoServiceReferer.setInterface(MotanDemoService.class);

        // 配置服务的group以及版本号
        motanDemoServiceReferer.setGroup("motan-demo-rpc");
        motanDemoServiceReferer.setVersion("1.0");
        motanDemoServiceReferer.setRequestTimeout(1000);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();

        //use direct registry
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8002");

        // use ZooKeeper registry
//        registry.setRegProtocol("zookeeper");
//        registry.setAddress("127.0.0.1:2181");
        motanDemoServiceReferer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan");
        protocol.setName("motan");
        motanDemoServiceReferer.setProtocol(protocol);
        // motanDemoServiceReferer.setDirectUrl("localhost:8002");  // 注册中心直连调用需添加此配置

        // 使用服务
        MotanDemoService service = motanDemoServiceReferer.getRef();
        System.out.println(service.hello("motan"));

        System.exit(0);
    }
}
