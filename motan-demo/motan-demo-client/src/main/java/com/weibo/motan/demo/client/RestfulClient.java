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
        System.out.println(service1.hello(345).get(0).getName());

        // use motan
        RestfulService service2 = (RestfulService) ctx.getBean("motanReferer");
        System.out.println(service2.hello(789).get(0).getName());
    }

}
