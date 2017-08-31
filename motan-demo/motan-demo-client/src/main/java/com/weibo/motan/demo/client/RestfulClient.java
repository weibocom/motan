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

import com.weibo.motan.demo.service.RestfulService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by zhanglei28 on 2017/8/30.
 */
public class RestfulClient {
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[]{"classpath:motan_demo_client_restful.xml"});

        // use restful
        RestfulService service1 = (RestfulService) ctx.getBean("restfulReferer");
        System.out.println(service1.getUsers(345).get(0).getName());

        // use motan
        RestfulService service2 = (RestfulService) ctx.getBean("motanReferer");
        System.out.println(service2.getUsers(789).get(0).getName());
    }

}
