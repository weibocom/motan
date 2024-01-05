/*
 *
 *   Copyright 2009-2022 Weibo, Inc.
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

package com.weibo.api.motan.config;

import com.weibo.api.motan.util.MotanGlobalConfigUtil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DefaultGlobalConfig is a default implement of GlobalConfig,
 * it will load default motan config by MotanGlobalConfigUtil.getDefaultConfigCopy() in constructor
 *
 * @author zhanglei28
 * @date 2022/11/1.
 */
public class DefaultGlobalConfig implements GlobalConfig {
    private ConcurrentHashMap<String, String> configs = new ConcurrentHashMap<>();

    public DefaultGlobalConfig() {
        Map<String, String> defaultConfigs = MotanGlobalConfigUtil.getDefaultConfigCopy();
        if (!defaultConfigs.isEmpty()) {
            putConfigs(defaultConfigs, false);
        }
    }

    @Override
    public String getConfig(String key) {
        return configs.get(key);
    }

    @Override
    public String getConfig(String key, String defaultValue) {
        String value = configs.get(key);
        return value == null ? defaultValue : value;
    }

    @Override
    public void putConfig(String key, String value) {
        configs.put(key, value);
    }

    @Override
    public String remove(String key) {
        return configs.remove(key);
    }

    @Override
    public void putConfigs(Map<String, String> configs, boolean override) {
        if (configs != null && !configs.isEmpty()) {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                if (override) {
                    this.configs.put(entry.getKey(), entry.getValue());
                } else {
                    this.configs.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    @Override
    public Set<Map.Entry<String, String>> entrySet() {
        return configs.entrySet();
    }
}
