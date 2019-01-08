package com.weibo.motan.demo.server;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.motan.demo.service.MotanDemoService2;

@MotanService(export = "demoMotan:8002")
public class MotanDemoService2Impl implements MotanDemoService2 {
    @Override
    public String goodbye(String name) {
        System.out.println(name);
        return "Goodbye " + name + "!";
    }
}
