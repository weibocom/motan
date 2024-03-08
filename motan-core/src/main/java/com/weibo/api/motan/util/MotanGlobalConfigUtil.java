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

package com.weibo.api.motan.util;

import com.weibo.api.motan.config.DefaultGlobalConfig;
import com.weibo.api.motan.config.GlobalConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhanglei28
 * @date 2022/11/1.
 * @since 1.2.1
 */
public class MotanGlobalConfigUtil {
    public static final String MOTAN_CONFIG_FILE = "motan.properties";
    private static final Map<String, String> DEFAULT_CONFIGS = new HashMap<>();
    private static volatile GlobalConfig innerGlobalConfig;

    static {
        init();
    }

    public static Map<String, String> getDefaultConfigCopy() {
        return new HashMap<>(DEFAULT_CONFIGS);
    }

    public static String getConfig(String key) {
        return innerGlobalConfig.getConfig(key);
    }

    public static String getConfig(String key, String defaultValue) {
        return innerGlobalConfig.getConfig(key, defaultValue);
    }

    public static void putConfig(String key, String value) {
        innerGlobalConfig.putConfig(key, value);
    }

    public static String remove(String key) {
        return innerGlobalConfig.remove(key);
    }

    public static void putConfigs(Map<String, String> configs, boolean override) {
        innerGlobalConfig.putConfigs(configs, override);
    }

    public static ConcurrentHashMap<String, String> getConfigs() {
        return innerGlobalConfig.getConfigs();
    }

    public static GlobalConfig setInnerGlobalConfig(GlobalConfig newConfig) {
        if (newConfig != null) {
            GlobalConfig oldConfig = innerGlobalConfig;
            innerGlobalConfig = newConfig;
            return oldConfig;
        }
        return null;
    }

    // load default motan configs from the file named "motan.properties" in resources
    private static void init() {
        URL url = Thread.currentThread().getContextClassLoader().getResource(MOTAN_CONFIG_FILE);
        if (url != null) {
            try (InputStream is = url.openStream()) {
                LoggerUtil.info("load default motan properties from " + url.getPath());
                Properties properties = new Properties();
                properties.load(is);
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
                        DEFAULT_CONFIGS.put(key.trim(), value.trim());
                    }
                }
            } catch (IOException e) {
                LoggerUtil.warn("load default motan properties fail. err:" + e.getMessage(), e);
            }
        }
        LoggerUtil.info("default motan properties:" + DEFAULT_CONFIGS);
        innerGlobalConfig = new DefaultGlobalConfig();
    }

}
