/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.proxy.CommonClient;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.motan.demo.service.MotanDemoService;
import com.weibo.motan.demo.service.PbParamService;
import com.weibo.motan.demo.service.model.User;
import io.grpc.examples.routeguide.Point;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Motan1RpcClient {

    public static void main(String[] args) throws Throwable {
        //motan（V1）协议使用CommonClient实现泛化调用
        RefererConfig<CommonClient> motanDemoServiceReferer = new RefererConfig<CommonClient>();

        // 设置接口及实现类
        motanDemoServiceReferer.setInterface(CommonClient.class);
        motanDemoServiceReferer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");


        // 配置服务的group以及版本号
        motanDemoServiceReferer.setGroup("motan-demo-rpc");
        motanDemoServiceReferer.setVersion("1.0");
        motanDemoServiceReferer.setRequestTimeout(1000);
        motanDemoServiceReferer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();

        //use direct registry
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");

        // use ZooKeeper registry
//        registry.setRegProtocol("zk");
//        registry.setAddress("127.0.0.1:2181");
        motanDemoServiceReferer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan");
        protocol.setName("motan");
        motanDemoServiceReferer.setProtocol(protocol);
        // motanDemoServiceReferer.setDirectUrl("localhost:8002");  // 注册中心直连调用需添加此配置

        // 使用服务
        CommonClient service = motanDemoServiceReferer.getRef();

        System.out.println(service.callV1("hello", new Object[]{"a"},"java.lang.String", String.class));
    }

}
