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

@Service
@Lazy
public class ConsulRegistryService implements RegistryService {
    @Autowired
    private ConsulClientWrapper clientWrapper;
    private ConsulClient consulClient;

    public ConsulRegistryService() {
    }

    /**
     * Unit Test中使用
     *
     * @param consulClient
     */
    public ConsulRegistryService(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    @PostConstruct
    public void init() {
        consulClient = clientWrapper.getConsulClient();
    }

    public List<String> getDatacenters() {
        return consulClient.getCatalogDatacenters().getValue();
    }

    @Override
    public List<String> getGroups() {
        List<String> groups = new ArrayList<String>();
        for (String dc : getDatacenters()) {
            QueryParams queryParams = new QueryParams(dc);
            Map<String, List<String>> serviceMap = consulClient.getCatalogServices(queryParams).getValue();
            serviceMap.remove("consul");
            for (String service : serviceMap.keySet()) {
                groups.add(formatGroupName(dc, service));
            }
        }
        return groups;
    }

    @Override
    public List<String> getServicesByGroup(String dcGroup) {
        Set<String> services = new HashSet<String>();
        List<CatalogService> serviceList = getCatalogServicesByGroup(dcGroup);
        for (CatalogService service : serviceList) {
            services.add(formatServiceName(service));
        }
        return new ArrayList<String>(services);
    }

    private List<CatalogService> getCatalogServicesByGroup(String dcGroup) {
        QueryParams queryParams = new QueryParams(getDcName(dcGroup));
        return consulClient.getCatalogService(getGroupName(dcGroup), queryParams).getValue();
    }

    @Override
    public List<JSONObject> getNodes(String dcGroup, String service, String nodeType) {
        List<JSONObject> results = new ArrayList<JSONObject>();
        List<Check> checks = consulClient.getHealthChecksForService(getGroupName(dcGroup), new QueryParams(getDcName(dcGroup))).getValue();
        for (Check check : checks) {
            String serviceId = check.getServiceId();
            String[] strings = serviceId.split("-");
            if (strings[1].equals(service)) {
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
    public List<JSONObject> getAllNodes(String dcGroup) {
        List<JSONObject> results = new ArrayList<JSONObject>();
        List<String> serviceNameSet = getServicesByGroup(dcGroup);
        for (String dcServiceName : serviceNameSet) {
            JSONObject service = new JSONObject();
            service.put("service", dcServiceName);
            List<JSONObject> availableServer = getNodes(dcGroup, dcServiceName, "PASSING");
            service.put("server", availableServer);
            List<JSONObject> unavailableServer = getNodes(dcGroup, dcServiceName, "CRITICAL");
            service.put("unavailableServer", unavailableServer);
            service.put("client", null);
            results.add(service);
        }
        return results;
    }

    private String formatGroupName(String dc, String groupName) {
        return dc + ":" + groupName;
    }

    private String formatServiceName(CatalogService catalogService) {
        return catalogService.getServiceId().split("-")[1];
    }

    private String getDcName(String dcString) {
        return dcString.substring(0, dcString.indexOf(":"));
    }

    private String getGroupName(String dcGroup) {
        return dcGroup.substring(dcGroup.indexOf(":") + 1);
    }
}
