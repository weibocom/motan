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

import com.weibo.motan.demo.service.MotanDemoService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * spring调用远程服务
 * 如果使用配置中心，请加载motan_demo_client.xml
 * 如果不使用配置中心，请加载motan_demo_client_direct.xml
 *
 */
public class DemoRpcClient {

    public static void main(String[] args) throws InterruptedException {

        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:motan_demo_client_direct.xml"});

        MotanDemoService service = (MotanDemoService) ctx.getBean("motanDemoReferer");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            System.out.println(service.hello("motan" + i));
            Thread.sleep(500);
        }
        System.out.println("motan demo is finish.");
        System.exit(0);
    }

}
