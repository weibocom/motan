/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.admin.handler;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.admin.AbstractAdminCommandHandler;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.runtime.RuntimeInfo;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.weibo.api.motan.runtime.RuntimeInfoKeys.*;

/**
 * @author zhanglei28
 * @date 2024/2/28.
 */
public class RuntimeInfoHandler extends AbstractAdminCommandHandler {
    private static final Map<String, Supplier<Map<String, ? extends RuntimeInfo>>> RUNTIME_INFO_FUNCTIONS = new HashMap<>();
    private static final Set<String> DEFAULT_KEYS = new HashSet<>();

    static {
        RUNTIME_INFO_FUNCTIONS.put(REGISTRIES_KEY, GlobalRuntime::getRuntimeRegistries);
        RUNTIME_INFO_FUNCTIONS.put(CLUSTERS_KEY, GlobalRuntime::getRuntimeClusters);
        RUNTIME_INFO_FUNCTIONS.put(EXPORTERS_KEY, GlobalRuntime::getRuntimeExporters);
        RUNTIME_INFO_FUNCTIONS.put(SERVERS_KEY, GlobalRuntime::getRuntimeServers);
        RUNTIME_INFO_FUNCTIONS.put(MESH_CLIENTS_KEY, GlobalRuntime::getRuntimeMeshClients);

        // default keys
        DEFAULT_KEYS.add(REGISTRIES_KEY);
        DEFAULT_KEYS.add(CLUSTERS_KEY);
        DEFAULT_KEYS.add(EXPORTERS_KEY);
        DEFAULT_KEYS.add(SERVERS_KEY);
        DEFAULT_KEYS.add(MESH_CLIENTS_KEY);
    }

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        result.put(INSTANCE_TYPE_KEY, "motan-java");
        result.put(GLOBAL_CONFIG_KEY, MotanGlobalConfigUtil.getConfigs());

        // add runtime info by keys
        Set<String> keys = DEFAULT_KEYS;
        if (params.containsKey("keys")) { // custom keys
            String keyString = params.get("keys");
            if (StringUtils.isNotBlank(keyString)) {
                keys = Arrays.stream(keyString.split(","))
                        .map(String::trim).collect(Collectors.toSet());
            }
        }
        for (String key : keys) {
            Supplier<Map<String, ? extends RuntimeInfo>> supplier = RUNTIME_INFO_FUNCTIONS.get(key);
            if (supplier != null) {
                addInfos(supplier.get(), key, result);
            }
        }
    }

    private void addInfos(Map<String, ? extends RuntimeInfo> infos, String key, JSONObject result) {
        if (!infos.isEmpty()) {
            JSONObject infoObject = new JSONObject();
            infos.forEach((k, v) -> infoObject.put(k, v.getRuntimeInfo()));
            result.put(key, infoObject);
        }
    }

    @Override
    public String[] getCommandName() {
        return new String[]{"/runtime/info"};
    }
}
