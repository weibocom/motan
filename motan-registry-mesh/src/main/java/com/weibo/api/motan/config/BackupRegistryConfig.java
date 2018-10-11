package com.weibo.api.motan.config;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunnights
 */
public class BackupRegistryConfig extends ExtConfig {
    private RegistryConfig backupRegistryConfig;

    public BackupRegistryConfig(RegistryConfig backupRegistryConfig) {
        this.backupRegistryConfig = backupRegistryConfig;
    }

    @Override
    protected void appendConfigParams(Map<String, String> parameters, String prefix) {
        StringBuilder builder = new StringBuilder(128);
        for (URL url : extractRegistryConfigToUrl(backupRegistryConfig)) {
            builder.append(url.toFullStr()).append(";");
        }
        parameters.put("backupRegistry", builder.toString());
    }

    private List<URL> extractRegistryConfigToUrl(RegistryConfig registryConfig) {
        String address = registryConfig.getAddress();
        if (StringUtils.isBlank(address)) {
            address = NetUtils.LOCALHOST + ":" + MotanConstants.DEFAULT_INT_VALUE;
        }
        Map<String, String> map = new HashMap<>();
        registryConfig.appendConfigParams(map);

        map.put(URLParamType.application.getName(), URLParamType.application.getValue());
        map.put(URLParamType.path.getName(), RegistryService.class.getName());
        map.put(URLParamType.refreshTimestamp.getName(), String.valueOf(System.currentTimeMillis()));

        // 设置默认的registry protocol，parse完protocol后，需要去掉该参数
        if (!map.containsKey(URLParamType.protocol.getName())) {
            if (address.contains("://")) {
                map.put(URLParamType.protocol.getName(), address.substring(0, address.indexOf("://")));
            } else {
                map.put(URLParamType.protocol.getName(), MotanConstants.REGISTRY_PROTOCOL_LOCAL);
            }
        }
        // address内部可能包含多个注册中心地址
        return UrlUtils.parseURLs(address, map);
    }
}
