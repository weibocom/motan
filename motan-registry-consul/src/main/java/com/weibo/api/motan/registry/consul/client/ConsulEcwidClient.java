package com.weibo.api.motan.registry.consul.client;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.ecwid.consul.v1.health.model.HealthService.Service;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.weibo.api.motan.registry.consul.ConsulConstants;
import com.weibo.api.motan.registry.consul.ConsulResponse;
import com.weibo.api.motan.registry.consul.ConsulService;
import com.weibo.api.motan.registry.consul.ConsulUtils;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

public class ConsulEcwidClient extends MotanConsulClient {
    public static ConsulClient client;

    public ConsulEcwidClient(String host, int port) {
        super(host, port);
        client = new ConsulClient(host, port);
        LoggerUtil.info("ConsulEcwidClient init finish. client host:" + host
                + ", port:" + port);
    }

    @Override
    public void checkPass(String serviceid) {
        client.agentCheckPass("service:" + serviceid);
    }

    @Override
    public void registerService(ConsulService service) {
        NewService newService = convertService(service);
        client.agentServiceRegister(newService);
    }

    @Override
    public void unregisterService(String serviceid) {
        client.agentServiceDeregister(serviceid);
    }

    @Override
    public ConsulResponse<List<ConsulService>> lookupHealthService(
            String serviceName, long lastConsulIndex) {
        QueryParams queryParams = new QueryParams(
                ConsulConstants.CONSUL_BLOCK_TIME_SECONDS, lastConsulIndex);
        Response<List<HealthService>> orgResponse = client.getHealthServices(
                serviceName, true, queryParams);
        ConsulResponse<List<ConsulService>> newResponse = null;
        if (orgResponse != null && orgResponse.getValue() != null
                && !orgResponse.getValue().isEmpty()) {
            List<HealthService> HealthServices = orgResponse.getValue();
            List<ConsulService> ConsulServices = new ArrayList<ConsulService>(
                    HealthServices.size());

            for (HealthService orgService : HealthServices) {
                try {
                    ConsulService newService = convertToConsulService(orgService);
                    ConsulServices.add(newService);
                } catch (Exception e) {
                    String servcieid = "null";
                    if (orgService.getService() != null) {
                        servcieid = orgService.getService().getId();
                    }
                    LoggerUtil.error(
                            "convert consul service fail. org consulservice:"
                                    + servcieid, e);
                }
            }
            if (!ConsulServices.isEmpty()) {
                newResponse = new ConsulResponse<List<ConsulService>>();
                newResponse.setValue(ConsulServices);
                newResponse.setConsulIndex(orgResponse.getConsulIndex());
                newResponse.setConsulLastContact(orgResponse
                        .getConsulLastContact());
                newResponse.setConsulKnownLeader(orgResponse
                        .isConsulKnownLeader());
            }
        }

        return newResponse;
    }

    @Override
    public String lookupCommand(String group) {
        Response<GetValue> response = client.getKVValue(ConsulConstants.CONSUL_MOTAN_COMMAND + ConsulUtils.convertGroupToServiceName(group));
        GetValue value = response.getValue();
        String command = "";
        if (value == null) {
            LoggerUtil.info("no command in group: " + group);
        } else if (value.getValue() != null) {
            command = value.getDecodedValue();
        }
        return command;
    }

    private NewService convertService(ConsulService service) {
        NewService newService = new NewService();
        newService.setAddress(service.getAddress());
        newService.setId(service.getId());
        newService.setName(service.getName());
        newService.setPort(service.getPort());
        newService.setTags(service.getTags());
        NewService.Check check = new NewService.Check();
        check.setTtl(service.getTtl() + "s");
        newService.setCheck(check);
        return newService;
    }

    private ConsulService convertToConsulService(HealthService healthService) {
        ConsulService service = new ConsulService();
        Service org = healthService.getService();
        service.setAddress(org.getAddress());
        service.setId(org.getId());
        service.setName(org.getService());
        service.setPort(org.getPort());
        service.setTags(org.getTags());
        return service;
    }

    @Override
    public void checkFail(String serviceid) {
        client.agentCheckFail("service:" + serviceid);
    }

}
