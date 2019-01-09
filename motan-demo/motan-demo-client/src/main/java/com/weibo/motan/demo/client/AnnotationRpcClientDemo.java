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

import com.weibo.api.motan.config.springsupport.BasicRefererConfigBean;
import com.weibo.api.motan.config.springsupport.ProtocolConfigBean;
import com.weibo.api.motan.config.springsupport.RegistryConfigBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Configuration
public class AnnotationRpcClientDemo {

    public static void main(String[] args) throws InterruptedException {

        ApplicationContext ctx = new ClassPathXmlApplicationContext(new
                String[]{"classpath:motan_demo_client_annotation.xml"});


        DemoRpcHandler handler = (DemoRpcHandler) ctx.getBean("demoRpcHandler");
        handler.test();


        System.out.println("motan demo is finish.");
        System.exit(0);
    }


    @Bean(name = "demoMotan")
    public ProtocolConfigBean demoMotanProtocolConfig() {
        ProtocolConfigBean config = new ProtocolConfigBean();
        //Id无需设置
//        config.setId("demoMotan");
        config.setName("motan");
        config.setDefault(true);
        config.setMaxContentLength(1048576);
        config.setHaStrategy("failover");
        config.setLoadbalance("roundrobin");
        return config;
    }

    @Bean(name = "demoMotan2")
    public ProtocolConfigBean protocolConfig2() {
        ProtocolConfigBean config = new ProtocolConfigBean();
        config.setName("motan");
        config.setMaxContentLength(1048576);
        config.setHaStrategy("failover");
        config.setLoadbalance("roundrobin");
        return config;
    }

    @Bean(name = "registry")
    public RegistryConfigBean registryConfig() {
        RegistryConfigBean config = new RegistryConfigBean();
//        config.setRegProtocol("zookeeper");
//        config.setAddress("127.0.0.1:2181");
        config.setRegProtocol("direct");
        config.setAddress("127.0.0.1:8002");
        return config;
    }


    @Bean(name = "motantestClientBasicConfig")
    public BasicRefererConfigBean baseRefererConfig() {
        BasicRefererConfigBean config = new BasicRefererConfigBean();
        config.setProtocol("demoMotan");
        config.setGroup("motan-demo-rpc");
        config.setModule("motan-demo-rpc");
        config.setApplication("myMotanDemo");
        config.setRegistry("registry");
        config.setCheck(false);
        config.setAccessLog(true);
        config.setRetries(2);
        config.setThrowException(true);
        return config;
    }

}
