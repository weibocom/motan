package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.motan.demo.service.MotanDemoService;

public abstract class DemoBaseRpcHandler {
    @MotanReferer
    private MotanDemoService motanDemoService;

    public void greeting() {
        for (int i = 0; i < 10; i++) {
            hello("motan handler " + i);
            goodbye("motan handler " + i);
            LoggerUtil.info("motan handler " + i);
        }
    }

    private void hello(String name) {
        System.out.println(motanDemoService.hello(name));
    }

    protected abstract void goodbye(String name);
}
