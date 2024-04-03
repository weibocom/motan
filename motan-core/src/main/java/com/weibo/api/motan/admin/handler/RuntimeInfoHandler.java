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
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2024/2/28.
 */
public class RuntimeInfoHandler extends AbstractAdminCommandHandler {

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        result.put(RuntimeInfoKeys.INSTANCE_TYPE_KEY, "motan-java");
        result.put(RuntimeInfoKeys.GLOBAL_CONFIG_KEY, MotanGlobalConfigUtil.getConfigs());

        // add registry infos
        addInfos(GlobalRuntime.getRuntimeRegistries(), RuntimeInfoKeys.REGISTRIES_KEY, result);

        // add cluster infos
        addInfos(GlobalRuntime.getRuntimeClusters(), RuntimeInfoKeys.CLUSTERS_KEY, result);

        // add exporter infos
        addInfos(GlobalRuntime.getRuntimeExporters(), RuntimeInfoKeys.EXPORTERS_KEY, result);

        // add mesh client infos
        addInfos(GlobalRuntime.getRuntimeMeshClients(), RuntimeInfoKeys.MESH_CLIENTS_KEY, result);

        // add server infos
        addInfos(GlobalRuntime.getRuntimeServers(), RuntimeInfoKeys.SERVERS_KEY, result);
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
