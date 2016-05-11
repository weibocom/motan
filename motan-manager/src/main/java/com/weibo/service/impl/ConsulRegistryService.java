package com.weibo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.Check;
import com.weibo.service.RegistryService;
import com.weibo.utils.ConsulClientWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

import static com.weibo.api.motan.registry.consul.ConsulConstants.CONSUL_SERVICE_MOTAN_PRE;

@Service
@Lazy
public class ConsulRegistryService implements RegistryService {
    @Autowired
    private ConsulClientWrapper clientWrapper;
    private ConsulClient consulClient;
    private String dc = "dc1";

    @PostConstruct
    public void init() {
        consulClient = clientWrapper.getConsulClient();
    }

    @Override
    public List<String> getGroups() {
        List<String> groups = new ArrayList<String>();
        QueryParams queryParams = new QueryParams(dc);
        Map<String, List<String>> serviceMap = consulClient.getCatalogServices(queryParams).getValue();
        serviceMap.remove("consul");
        for (String service : serviceMap.keySet()) {
            groups.add(formatGroupName(service));
        }
        return groups;
    }

    private String formatGroupName(String str) {
        return str.substring(CONSUL_SERVICE_MOTAN_PRE.length());
    }

    @Override
    public List<String> getServicesByGroup(String group) {
        Set<String> services = new HashSet<String>();
        List<CatalogService> serviceList = getCatalogServicesByGroup(group);
        for (CatalogService service : serviceList) {
            services.add(formatServiceName(service));
        }
        return new ArrayList<String>(services);
    }

    private List<CatalogService> getCatalogServicesByGroup(String group) {
        QueryParams queryParams = new QueryParams(dc);
        return consulClient.getCatalogService(CONSUL_SERVICE_MOTAN_PRE + group, queryParams).getValue();
    }

    private String formatServiceName(CatalogService catalogService) {
        return catalogService.getServiceId().split("-")[1];
    }

    @Override
    public List<JSONObject> getNodes(String group, String service, String nodeType) {
        List<JSONObject> results = new ArrayList<JSONObject>();

        List<Check> checks = consulClient.getHealthChecksForService(CONSUL_SERVICE_MOTAN_PRE + group, new QueryParams(dc)).getValue();
        for (Check check : checks) {
            String serviceId = check.getServiceId();
            String[] strings = serviceId.split("-");
            if (service.equals(strings[1])) {
                Check.CheckStatus status = check.getStatus();
                JSONObject node = new JSONObject();
                if (nodeType.equals(status.toString())) {
                    node.put("host", strings[0]);
                    node.put("info", null);
                    results.add(node);
                }
            }
        }
        return results;
    }

    @Override
    public List<JSONObject> getAllNodes(String group) {
        List<JSONObject> results = new ArrayList<JSONObject>();
        List<String> serviceNameSet = getServicesByGroup(group);
        for (String serviceName : serviceNameSet) {
            JSONObject service = new JSONObject();
            service.put("service", serviceName);
            List<JSONObject> availableServer = getNodes(group, serviceName, "PASSING");
            service.put("server", availableServer);
            List<JSONObject> unavailableServer = getNodes(group, serviceName, "CRITICAL");
            service.put("unavailableServer", unavailableServer);
            service.put("client", null);
            results.add(service);
        }
        return results;
    }
}
