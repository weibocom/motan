package com.weibo.service;

import com.ecwid.consul.v1.ConsulClient;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.dao.SingletonConsulClient;
import com.weibo.dao.SingletonZKClient;
import com.weibo.service.impl.ConsulCommandService;
import com.weibo.service.impl.ConsulRegistryService;
import com.weibo.service.impl.ZkCommandService;
import com.weibo.service.impl.ZkRegistryService;
import com.weibo.utils.ManagerConstants;
import org.I0Itec.zkclient.ZkClient;

public enum ServiceFactory {
    INSTANCE;

    public RegistryService createRegistryService() {
        if (ManagerConstants.REGISTRY.equals("zookeeper")) {
            ZkClient zkClient = SingletonZKClient.INSTANCE.getZkClient();
            return new ZkRegistryService(zkClient);
        } else if (ManagerConstants.REGISTRY.equals("consul")) {
            ConsulClient consulClient = SingletonConsulClient.INSTANCE.getConsulClient();
            return new ConsulRegistryService(consulClient);
        } else {
            throw new MotanFrameworkException(String.format("not supported registry type of %s.", ManagerConstants.REGISTRY));
        }
    }

    public CommandService createCommandService() {
        if (ManagerConstants.REGISTRY.equals("zookeeper")) {
            ZkClient zkClient = SingletonZKClient.INSTANCE.getZkClient();
            return new ZkCommandService(zkClient);
        } else if (ManagerConstants.REGISTRY.equals("consul")) {
            ConsulClient consulClient = SingletonConsulClient.INSTANCE.getConsulClient();
            return new ConsulCommandService(consulClient);
        } else {
            throw new MotanFrameworkException(String.format("not supported registry type of %s.", ManagerConstants.REGISTRY));
        }
    }
}
