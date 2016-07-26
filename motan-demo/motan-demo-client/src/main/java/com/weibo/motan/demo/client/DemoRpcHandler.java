package com.weibo.motan.demo.client;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.motan.demo.service.MotanDemoService;
import org.springframework.stereotype.Component;

/**
 * Created by fld on 16/6/1.
 */
@Component
public class DemoRpcHandler {

    @MotanReferer
    private MotanDemoService motanDemoService;

    public void test() {
        for (int i = 0; i < 10; i++) {
            System.out.println(motanDemoService.hello("motan handler" + i));
            LoggerUtil.info("motan handler" + i);
        }

    }
}
