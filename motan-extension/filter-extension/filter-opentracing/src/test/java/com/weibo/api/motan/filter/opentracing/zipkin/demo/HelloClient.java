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
package com.weibo.api.motan.filter.opentracing.zipkin.demo;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.api.motan.filter.opentracing.HelloService;
import com.weibo.api.motan.filter.opentracing.OpenTracingContext;

public class HelloClient {
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:motan_client.xml");
        
        // set tracer implementation
        OpenTracingContext.tracerFactory = new MyTracerFactory();
        
        // use motan
        HelloService service = (HelloService) ctx.getBean("helloService");
        for(int i = 0; i< 10; i++){
            try{
                System.out.println(service.sayHello("motan"));
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        System.exit(0);
    }
}
