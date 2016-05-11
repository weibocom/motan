package com.weibo.utils;

import com.weibo.api.motan.exception.MotanFrameworkException;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Lazy
@Scope("singleton")
public class ZkClientWrapper {

    @Value("${registry.url}")
    private String registryUrl;
    private ZkClient zkClient;

    @PostConstruct
    void init() {
        try {
            zkClient = new ZkClient(registryUrl, 10000);
        } catch (Exception e) {
            throw new MotanFrameworkException("Fail to connect zookeeper, cause: " + e.getMessage());
        }
    }

    @PreDestroy
    void destory() {
        zkClient = null;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

}
