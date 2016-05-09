package com.weibo.service;

import com.ecwid.consul.v1.ConsulClient;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.service.impl.ConsulCommandService;
import com.weibo.service.impl.ConsulRegistryService;
import com.weibo.service.impl.ZkCommandService;
import com.weibo.service.impl.ZkRegistryService;
import com.weibo.utils.ManagerConstants;
import org.I0Itec.zkclient.ZkClient;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Scope("singleton")
@Component
public class ServiceFactory {
    private RegistryService registryService;
    private CommandService commandService;

    @PostConstruct
    private void init() {
        if (ManagerConstants.REGISTRY.equals("zookeeper")) {
            try {
                ZkClient zkClient = new ZkClient(ManagerConstants.REGISTRY_URL, 10000);
                registryService = new ZkRegistryService(zkClient);
                commandService = new ZkCommandService(zkClient);
            } catch (Exception e) {
                throw new MotanFrameworkException("Fail to init zookeeper, cause: " + e.getMessage());
            }
        } else if (ManagerConstants.REGISTRY.equals("consul")) {
            try {
                String address = ManagerConstants.REGISTRY_URL;
                String[] arr = address.split(":");
                String host = arr[0];
                int port = Integer.parseInt(arr[1]);
                ConsulClient consulClient = new ConsulClient(host, port);
                registryService = new ConsulRegistryService(consulClient);
                commandService = new ConsulCommandService(consulClient);
            } catch (Exception e) {
                throw new MotanFrameworkException("Fail to init consul, cause: " + e.getMessage());
            }
        } else {
            throw new MotanFrameworkException(String.format("not supported registry type of %s.", ManagerConstants.REGISTRY));
        }
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public CommandService getCommandService() {
        return commandService;
    }
}
