/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.util;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codahale.metrics.MetricRegistry;

/**
 * 生成 {@link MetricRegistry} 的工厂类。
 * 
 * @author Aiden S. Zouliu
 *
 */
public class InternalMetricsFactory {

    private static final ConcurrentMap<String, MetricRegistry> getRegistryCache;
    private static final MetricRegistry defaultMetricsRegistry;
    static {
        getRegistryCache = new ConcurrentHashMap<String, MetricRegistry>();
        getRegistryCache.put("default", defaultMetricsRegistry = new MetricRegistry());
    }

    /**
     * 指定名字获取所属的实例。
     * 
     * @param name {@link MetricRegistry} 实例的名字。
     * @return {@link MetricRegistry} 实例。
     */
    public static MetricRegistry getRegistryInstance(String name) {
        MetricRegistry instance = getRegistryCache.get(name);
        if (instance == null) {
            getRegistryCache.putIfAbsent(name, new MetricRegistry());
            instance = getRegistryCache.get(name);
        }
        return instance;
    }

    /**
     * 指定几个名字的关键词，依据 {@link MetricRegistry} 的名字生成规则获取所属的实例。
     * 
     * @param name 关键字。
     * @param names 剩余的关键字。
     * @return {@link MetricRegistry} 实例。
     */
    public static MetricRegistry getRegistryInstance(String name, String... names) {
        final String key = MetricRegistry.name(name, names);
        MetricRegistry instance = getRegistryCache.get(key);
        if (instance == null) {
            getRegistryCache.putIfAbsent(key, new MetricRegistry());
            instance = getRegistryCache.get(key);
        }
        return instance;
    }

    /**
     * 指定类类型和几个名字的关键词，依据 {@link MetricRegistry} 的名字生成规则获取所属的实例。
     * 
     * @param clazz 类的类型。
     * @param names 关键字。
     * @return {@link MetricRegistry} 实例。
     */
    public static MetricRegistry getRegistryInstance(Class<?> clazz, String... names) {
        final String key = MetricRegistry.name(clazz, names);
        MetricRegistry instance = getRegistryCache.get(key);
        if (instance == null) {
            getRegistryCache.putIfAbsent(key, new MetricRegistry());
            instance = getRegistryCache.get(key);
        }
        return instance;
    }

    /**
     * 返回默认的 {@link MetricRegistry}。
     */
    public static MetricRegistry getDefaultMetricsRegistry() {
        return defaultMetricsRegistry;
    }

    /**
     * 返回当前注册的全部 {@link MetricRegistry}s。
     */
    public static Map<String, MetricRegistry> allRegistries() {
        return Collections.unmodifiableMap(getRegistryCache);
    }

}
