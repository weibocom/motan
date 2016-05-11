package com.weibo.utils;

import com.ecwid.consul.v1.ConsulClient;
import com.weibo.api.motan.exception.MotanFrameworkException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
@Lazy
@Scope("singleton")
public class ConsulClientWrapper {

    @Value("${registry.url}")
    private String registryUrl;
    private ConsulClient consulClient;

    @PostConstruct
    void init() {
        try {
            String[] arr = registryUrl.split(":");
            String host = arr[0];
            int port = Integer.parseInt(arr[1]);
            consulClient = new ConsulClient(host, port);
        } catch (Exception e) {
            throw new MotanFrameworkException("Fail to connect consul, cause: " + e.getMessage());
        }
    }

    @PreDestroy
    void destory() {
        consulClient = null;
    }

    public ConsulClient getConsulClient() {
        return consulClient;
    }

}
