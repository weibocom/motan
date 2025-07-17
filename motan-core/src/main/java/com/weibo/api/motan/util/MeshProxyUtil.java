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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author zhanglei28
 * @date 2022/7/14.
 * @since 1.1.11
 */
public class MeshProxyUtil {
    // config keys
    private static final String MODE_KEY = "mode"; // proxy type key
    private static final String PORT_KEY = "port"; // mesh transport port for client end
    private static final String IP_KEY = "ip"; // mesh management port
    private static final String PROTOCOL_KEY = "protocol"; // proxy protocol

    // config values
    private static final String MODE_SERVER = "server"; // 代理server侧流量
    private static final String MODE_CLIENT = "client"; // 代理client侧流量
    private static final String MODE_ALL = "all"; // 代理双端流量
    private static final String DEFAULT_PORT = "0"; // 默认mesh正向代理端口.为0时，MeshRegistry会使用统一默认端口。

    private static final String MESH_REGISTRY_NAME = "weibomesh";
    private static final Set<String> NOT_PROCESS_REGISTRY_PROTOCOLS = new HashSet<>(Arrays.asList("local", "direct", MESH_REGISTRY_NAME));
    private static Boolean initChecked; // 是否可以进行mesh proxy
    private static Map<String, String> proxyConfig;

    static {
        initCheck();
    }

    /**
     * 如果通过环境变量配置了使用Mesh进行代理，则通过把registry转换为MeshRegistry的方式实现服务的代理
     *
     * @param originRegistryUrls 原始注册中心urls
     * @param serviceUrl         具体的rpc服务。
     * @param isServerEnd        是否是服务端使用的场景。环境变量可以控制是对client端流量代理，还是server端流量代理，或者全部代理。
     * @return 如果配置了环境变量则进行代理，否则原样返回
     */
    public static List<URL> processMeshProxy(List<URL> originRegistryUrls, URL serviceUrl, boolean isServerEnd) {
        if (needProcess(serviceUrl, isServerEnd)) {
            try {
                List<URL> newRegistryUrls = new ArrayList<>(originRegistryUrls.size());
                for (URL url : originRegistryUrls) {
                    if (NOT_PROCESS_REGISTRY_PROTOCOLS.contains(url.getProtocol())) {
                        newRegistryUrls.add(url); // 使用原始注册中心
                        LoggerUtil.info("mesh proxy ignore url:" + serviceUrl.toSimpleString()
                                + ", registry: " + url.toSimpleString());
                    } else {
                        URL meshRegistryUrl = buildMeshRegistryUrl(url);
                        newRegistryUrls.add(meshRegistryUrl);
                        LoggerUtil.info("build mesh proxy registry for url:" + serviceUrl.toSimpleString()
                                + ", origin registry:" + url.toSimpleString()
                                + ", mesh registry url:" + meshRegistryUrl.toFullStr());
                    }
                }
                return newRegistryUrls;
            } catch (Exception e) {
                LoggerUtil.error("proxy motan fail", e);
            }
        }
        return originRegistryUrls;
    }

    public static boolean needProcess(URL serviceUrl, boolean isServerEnd) {
        if (!initChecked) {
            return false;
        }
        // check proxy mode
        String mode = proxyConfig.get(MODE_KEY);
        if (StringUtils.isBlank(mode)) {// 必须显示指定，不考虑提供默认值
            return false;
        }
        if (!MODE_ALL.equals(mode) && !MODE_SERVER.equals(mode) && !MODE_CLIENT.equals(mode)) {
            return false; // 未识别模式不进行处理
        }
        if (MODE_CLIENT.equals(mode) && isServerEnd) {// client模式下，server端不进行处理
            return false;
        }
        if (MODE_SERVER.equals(mode) && !isServerEnd) {// server模式下，client端不进行处理
            return false;
        }

        // check protocol
        if (!"motan2".equals(serviceUrl.getProtocol()) && !"motan".equals(serviceUrl.getProtocol())) {// only support motan&motan2 protocol
            return false;
        }
        String protocol = proxyConfig.get(PROTOCOL_KEY);
        if (StringUtils.isNotBlank(protocol) && !protocol.equals(serviceUrl.getProtocol())) {
            return false;
        }
        return true;
    }

    /**
     * 解析mesh proxy 环境变量中的配置
     *
     * @param meshProxyString 配置字符串格式 "key:value,key:value", 其中value会进行url decode。 例如："type:server,mport:8002,port:9981"
     * @return 解析后的配置
     */
    private static Map<String, String> parseProxyConfig(String meshProxyString) {
        Map<String, String> proxyConfig = new HashMap<>();
        String[] items = meshProxyString.split(",");
        for (String item : items) {
            String[] values = item.split(":");
            if (StringUtils.isNotBlank(values[0])) {// key not empty
                String k = values[0].trim();
                String v = "";
                if (values.length > 1 && StringUtils.isNotBlank(values[1])) {
                    v = StringTools.urlDecode(values[1].trim());
                }
                proxyConfig.put(k, v);
                LoggerUtil.info("add mesh proxy param: " + k + ":" + v);
            }
        }
        return proxyConfig;
    }

    private static URL buildMeshRegistryUrl(URL proxyRegistry) {
        URL meshRegistryUrl = new URL(MESH_REGISTRY_NAME,
                getValue(proxyConfig, IP_KEY, MotanConstants.MESH_DEFAULT_HOST),
                Integer.parseInt(getValue(proxyConfig, PORT_KEY, DEFAULT_PORT)),
                RegistryService.class.getName()
        );
        Map<String, String> params = new HashMap<>(proxyConfig);
        // put necessary keys
        params.put(URLParamType.dynamic.getName(), "true");
        params.put(URLParamType.proxyRegistryUrlString.getName(), StringTools.urlEncode(proxyRegistry.toFullStr()));
        meshRegistryUrl.addParameters(params);
        return meshRegistryUrl;
    }

    private static String getValue(Map<String, String> configs, String key, String defaultValue) {
        String value = configs.get(key);
        return StringUtils.isNotBlank(value) ? value : defaultValue;
    }

    // 检查是否支持mesh proxy
    private static void initCheck() {
        // check env set
        String meshProxyString = System.getenv(MotanConstants.ENV_MESH_PROXY);
        if (StringUtils.isNotBlank(meshProxyString)) {
            LoggerUtil.info("find MOTAN_MESH_PROXY env, value:" + meshProxyString);
            proxyConfig = parseProxyConfig(meshProxyString);
            // check MeshRegistry extension
            RegistryFactory meshRegistryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(MESH_REGISTRY_NAME, false);
            if (meshRegistryFactory != null) {
                initChecked = true;
                LoggerUtil.info("mesh proxy init check passed");
                return;
            } else {
                LoggerUtil.error("can not proxy motan, because MeshRegistry extension not found, maybe the dependency of 'motan-registry-weibomesh' not set in pom");
            }
        }
        initChecked = false;
    }

    // ---- only for test ----
    protected static void reset() {
        proxyConfig = null;
        initCheck();
    }

    protected static Map<String, String> getProxyConfig() {
        return proxyConfig;
    }

    protected static boolean setInitChecked(boolean value) {
        boolean oldValue = initChecked;
        initChecked = value;
        return oldValue;
    }

}
