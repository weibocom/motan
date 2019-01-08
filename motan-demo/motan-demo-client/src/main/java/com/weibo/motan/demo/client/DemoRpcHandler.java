package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.motan.demo.service.MotanDemoService2;
import org.springframework.stereotype.Component;

/**
 * Created by fld on 16/6/1.
 */
@Component
public class DemoRpcHandler extends DemoBaseRpcHandler {
    @MotanReferer
    private MotanDemoService2 motanDemoService2;

    @Override
    protected void goodbye(String name) {
        System.out.println(motanDemoService2.goodbye(name));
    }
}
