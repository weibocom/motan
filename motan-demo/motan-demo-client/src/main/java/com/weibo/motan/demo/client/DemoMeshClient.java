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

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.transport.MeshClient;
import com.weibo.api.motan.util.MotanClientUtil;
import com.weibo.motan.demo.service.MotanDemoService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DemoMeshClient {

    public static void main(String[] args) throws Throwable {
        //使用MeshClient需要先部署mesh agent(可以参考https://github.com/weibocom/motan-go)。
        // 在mesh agent中已经配置好对应的服务。
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:demo_mesh_client.xml"});
        MotanDemoService service = (MotanDemoService) ctx.getBean("refererWithMeshClient");
        System.out.println(service.hello("motan"));

        // 直接使用 MeshClient
        MeshClient client = (MeshClient) ctx.getBean("testMeshClient");
        Request request = MotanClientUtil.buildRequest("com.weibo.motan.demo.service.MotanDemoService",
                "hello", new Object[]{"motan"}, null);
        // sync call
        System.out.println(client.call(request, String.class));

        // async call
        System.out.println(client.asyncCall(request, String.class).getValue());
        System.exit(0);
    }

// agent yaml配置样例参考：
//motan-agent:
//  application: agent-test
//motan-registry:
//  direct-registry:
//    protocol: direct
//    host: localhost
//    port: 8002
//motan-refer:
//  test-demo:
//    registry: direct-registry
//    path: com.weibo.motan.demo.service.MotanDemoService
//    group: motan-demo-rpc
}
