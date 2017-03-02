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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.api.motan.rpc.Future;
import com.weibo.api.motan.rpc.FutureListener;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.motan.demo.service.MotanDemoServiceAsync;

public class DemoRpcAsyncClient {

    /**
     * 使用motan异步调用方法： 1、在声明的service上增加@MotanAsync注解。 如MotanDemoService
     * 2、在项目pom.xml中增加build-helper-maven-plugin，用来把自动生成类的目录设置为source path。 参见motan-demo-api模块的pom声明。
     * 也可以不使用plugin，手动将target/generated-sources/annotations目录设置为source path。
     * 3、在client配置的motan:referer标签中配置interface为自动生成的以Async为后缀的对应service类。
     * 参见本模块的motan_demo_async_client.xml
     * 
     */
    public static void main(String[] args) {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {"classpath:motan_demo_async_client.xml"});

        MotanDemoServiceAsync service = (MotanDemoServiceAsync) ctx.getBean("motanDemoReferer");
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            // sync call
            System.out.println(service.hello("motan" + i));

            // async call
            ResponseFuture future = service.helloAsync("motan async " + i);
            System.out.println(future.getValue());

            // multi call
            ResponseFuture future1 = service.helloAsync("motan async multi-1-" + i);
            ResponseFuture future2 = service.helloAsync("motan async multi-2-" + i);
            System.out.println(future1.getValue() + ", " + future2.getValue());

            // async with listener
            ResponseFuture future3 = service.helloAsync("motan async multi-1-" + i);
            ResponseFuture future4 = service.helloAsync("motan async multi-2-" + i);
            FutureListener listener = new FutureListener() {
                @Override
                public void operationComplete(Future future) throws Exception {
                    System.out.println("async call "
                            + (future.isSuccess() ? "sucess! value:" + future.getValue() : "fail! exception:"
                                    + future.getException().getMessage()));
                }
            };
            future3.addListener(listener);
            future4.addListener(listener);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("motan demo is finish.");
        System.exit(0);

    }

}
