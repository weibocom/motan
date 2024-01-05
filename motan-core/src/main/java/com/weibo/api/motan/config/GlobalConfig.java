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

import java.util.Map;
import java.util.Set;

/**
 * @author zhanglei28
 * @date 2022/11/1.
 */
public interface GlobalConfig {

    String getConfig(String key);

    String getConfig(String key, String defaultValue);

    void putConfig(String key, String value);

    String remove(String key);

    void putConfigs(Map<String, String> configs, boolean override);

    Set<Map.Entry<String, String>> entrySet();

}
