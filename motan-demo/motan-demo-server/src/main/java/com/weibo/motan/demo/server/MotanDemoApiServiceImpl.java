package com.weibo.motan.demo.server;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.motan.demo.service.MotanDemoApiService;

/**
 * Created by yunzhu on 17/3/6.
 */

@MotanService(export = "motan:8003",group = "test-a")

public class MotanDemoApiServiceImpl implements MotanDemoApiService {
    @Override
    public void say(String str) {
        System.out.println(str);
    }
}
