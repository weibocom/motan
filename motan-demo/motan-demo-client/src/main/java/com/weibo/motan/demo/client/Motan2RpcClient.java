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
import com.weibo.api.motan.util.MotanClientUtil;
import com.weibo.motan.demo.service.MotanDemoService;
import com.weibo.motan.demo.service.PbParamService;
import com.weibo.motan.demo.service.model.User;
import io.grpc.examples.routeguide.Point;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Motan2RpcClient {

    public static void main(String[] args) throws Throwable {
        motan2ApiCommonClientListDemo();
        motan2ApiCommonClientPojoDemo();
        motan2ApiCommonClientGenericParameterTypesDemo();


        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:motan2_demo_client.xml"});
        MotanDemoService service;
        // hessian
        service = (MotanDemoService) ctx.getBean("motanDemoReferer");
        print(service);

        // simple serialization
        service = (MotanDemoService) ctx.getBean("motanDemoReferer-simple");
        print(service);

        // breeze serialization
        service = (MotanDemoService) ctx.getBean("motanDemoReferer-breeze");
        print(service);

        // pb serialization
        PbParamService pbService = (PbParamService) ctx.getBean("motanDemoReferer-pb");
        System.out.println(pbService.getFeature(Point.newBuilder().setLatitude(123).setLongitude(456).build()));

        // common client
        CommonClient xmlClient = (CommonClient) ctx.getBean("motanDemoReferer-common-client");
        motan2XmlCommonClientDemo(xmlClient);
        motan2ApiCommonClientDemo();

        System.out.println("motan demo is finish.");
        System.exit(0);
    }

    public static void print(MotanDemoService service) throws InterruptedException {
        for (int i = 0; i < 3; i++) {
            System.out.println(service.hello("motan" + i));
            Thread.sleep(1000);
        }
    }

    public static void motan2XmlCommonClientDemo(CommonClient client) throws Throwable {
        System.out.println(client.call("hello", new Object[]{"a"}, String.class));

        User user = new User(1, "AAA");
        System.out.println(user);

        user = (User) client.call("rename", new Object[]{user, "BBB"}, User.class);
        System.out.println(user);

        ResponseFuture future = (ResponseFuture) client.asyncCall("rename", new Object[]{user, "CCC"}, User.class);
        user = (User) future.getValue();
        System.out.println(user);

        ResponseFuture future2 = (ResponseFuture) client.asyncCall("rename", new Object[]{user, "DDD"}, User.class);
        future2.addListener(future1 -> System.out.println(future1.getValue()));

        Request request = client.buildRequest("rename", new Object[]{user, "EEE"});
        request.setAttachment("a", "a");
        user = (User) client.call(request, User.class);
        System.out.println(user);

        // expect throw exception
//        client.call("rename", new Object[]{null, "FFF"}, void.class);
    }

    public static void motan2ApiCommonClientDemo() throws Throwable {
        RefererConfig<CommonClient> referer = new RefererConfig<>();

        // 设置服务端接口
        referer.setInterface(CommonClient.class);
        referer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");

        // 配置服务的group以及版本号
        referer.setGroup("motan-demo-rpc");
        referer.setVersion("1.0");
        referer.setRequestTimeout(1000);
        referer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");
        referer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan2");
        protocol.setName("motan2");
        referer.setProtocol(protocol);

        // 使用服务
        CommonClient client = referer.getRef();
        System.out.println(client.call("hello", new Object[]{"a"}, String.class));
    }

    public static void motan2ApiCommonClientPojoDemo() throws Throwable {
        RefererConfig<CommonClient> referer = new RefererConfig<>();

        // 设置服务端接口
        referer.setInterface(CommonClient.class);
        referer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");

        // 配置服务的group以及版本号
        referer.setGroup("motan-demo-rpc");
        referer.setVersion("1.0");
        referer.setRequestTimeout(1000);
        referer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");
        referer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan2");
        protocol.setName("motan2");
        referer.setProtocol(protocol);

        // 使用服务
        CommonClient client = referer.getRef();

        //使用Map代替POJO进行真正的泛化调用
        Map<String, Object> map = new HashMap<>();
        map.put("id", 1999);
        map.put("name", "dinglang");

        Request request = MotanClientUtil.buildRequest("com.weibo.motan.demo.service.MotanDemoService",
                "rename", "com.weibo.motan.demo.service.model.User,java.lang.String",
                new Object[]{map, "EEE"}, null);
        System.out.println(client.call(request, Object.class));
    }

    public static void motan2ApiCommonClientListDemo() throws Throwable {
        RefererConfig<CommonClient> referer = new RefererConfig<>();

        // 设置服务端接口
        referer.setInterface(CommonClient.class);
        referer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");

        // 配置服务的group以及版本号
        referer.setGroup("motan-demo-rpc");
        referer.setVersion("1.0");
        referer.setRequestTimeout(1000);
        referer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");
        referer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan2");
        protocol.setName("motan2");
        referer.setProtocol(protocol);

        // 使用服务
        CommonClient client = referer.getRef();


        List<Integer> integerList = new ArrayList<>();
        integerList.add(1);
        integerList.add(2);
        integerList.add(3);
        integerList.add(4);

        Request request = MotanClientUtil.buildRequest("com.weibo.motan.demo.service.MotanDemoService",
                "getUsers", "java.util.List",
                new Object[]{integerList}, null);
        System.out.println(client.call(request, Object.class));
    }

    public static void motan2ApiCommonClientGenericParameterTypesDemo() throws Throwable {
        RefererConfig<CommonClient> referer = new RefererConfig<>();

        // 设置服务端接口
        referer.setInterface(CommonClient.class);
        referer.setServiceInterface("com.weibo.motan.demo.service.MotanDemoService");

        // 配置服务的group以及版本号
        referer.setGroup("motan-demo-rpc");
        referer.setVersion("1.0");
        referer.setRequestTimeout(100000);
        referer.setAsyncInitConnection(false);

        // 配置注册中心直连调用
        RegistryConfig registry = new RegistryConfig();
        registry.setRegProtocol("direct");
        registry.setAddress("127.0.0.1:8001");
        referer.setRegistry(registry);

        // 配置RPC协议
        ProtocolConfig protocol = new ProtocolConfig();
        protocol.setId("motan2");
        protocol.setName("motan2");
        referer.setProtocol(protocol);

        // 使用服务
        CommonClient client = referer.getRef();

        Map<String, Object> map = new HashMap<>();
        map.put("id", 1999);
        map.put("name", "dinglang");

        Map<String, Object> user = new HashMap<>();
        user.put("id", 1998);
        user.put("name", "dylan");

        List<Object> list = new ArrayList<>();
        list.add(map);
        list.add(user);

        Request request = MotanClientUtil.buildRequest("com.weibo.motan.demo.service.MotanDemoService",
                "batchSave", "java.util.List",
                new Object[]{list}, null);
        System.out.println(client.call(request, Object.class));
    }

}
