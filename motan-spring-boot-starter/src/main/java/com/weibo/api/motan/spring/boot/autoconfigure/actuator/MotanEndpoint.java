package com.weibo.api.motan.spring.boot.autoconfigure.actuator;

import com.google.common.collect.Maps;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties(prefix = "endpoints.motan")
public class MotanEndpoint extends AbstractEndpoint<Map<String, Object>> {
    public MotanEndpoint() {
        super("motan", false);
    }

    @Override
    public Map<String, Object> invoke() {
        Map<String, Set<String>> statusMap = Maps.newHashMap();
        Map<String, Set<String>> servicesMap = Maps.newHashMap();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("status", statusMap);
        metrics.put("services", servicesMap);
        return metrics;
    }
}
