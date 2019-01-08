package com.weibo.motan.demo.service;

import com.weibo.api.motan.transport.async.MotanAsync;

@MotanAsync
public interface MotanDemoService2 {
    String goodbye(String name);
}
