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

package com.weibo.api.motan.config;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.config.annotation.ConfigDesc;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * registry config
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-27
 */

public class RegistryConfig extends AbstractConfig {

    private static final long serialVersionUID = 3236055928361714933L;

    // 注册配置名称
    private String name;

    // 注册协议
    private String regProtocol;

    // 注册中心地址，支持多个ip+port，格式：ip1:port1,ip2:port2,ip3，如果没有port，则使用默认的port
    private String address;

    // 注册中心缺省端口
    private Integer port;

    // 注册中心请求超时时间(毫秒)
    private Integer requestTimeout;

    // 注册中心连接超时时间(毫秒)
    private Integer connectTimeout;

    // 注册中心会话超时时间(毫秒)
    private Integer registrySessionTimeout;

    // 失败后重试的时间间隔
    private Integer registryRetryPeriod;

    // 启动时检查注册中心是否存在
    private String check;

    // 在该注册中心上服务是否暴露
    private Boolean register;

    // 在该注册中心上服务是否引用
    private Boolean subscribe;

    private Boolean isDefault;

    // vintage的配置移除策略，@see #RegistryConfig#Excise
    private String excise;

    // 被代理的registry。
    private RegistryConfig proxyRegistry;

    // 在mesh中要使用的registry。用来和mesh 配置中的registry进行关联
    private String meshRegistryName;

    private Boolean dynamic; //是否支持动态注册、订阅能力。例如通过mesh代理的注册中心，是否支持动态注册、订阅服务。

    @ConfigDesc(key = "protocol")
    public String getRegProtocol() {
        return regProtocol;
    }

    public void setRegProtocol(String regProtocol) {
        this.regProtocol = regProtocol;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    @Deprecated
    public void setCheck(Boolean check) {
        this.check = String.valueOf(check);
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    public Boolean getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Boolean subscribe) {
        this.subscribe = subscribe;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Integer getRegistrySessionTimeout() {
        return registrySessionTimeout;
    }

    public void setRegistrySessionTimeout(Integer registrySessionTimeout) {
        this.registrySessionTimeout = registrySessionTimeout;
    }

    public Integer getRegistryRetryPeriod() {
        return registryRetryPeriod;
    }

    public void setRegistryRetryPeriod(Integer registryRetryPeriod) {
        this.registryRetryPeriod = registryRetryPeriod;
    }

    public String getExcise() {
        return excise;
    }

    public void setExcise(String excise) {
        this.excise = excise;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean isDefault() {
        return isDefault;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public RegistryConfig getProxyRegistry() {
        return proxyRegistry;
    }

    public void setProxyRegistry(RegistryConfig proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
    }

    public String getMeshRegistryName() {
        return meshRegistryName;
    }

    public void setMeshRegistryName(String meshRegistryName) {
        this.meshRegistryName = meshRegistryName;
    }

    public Boolean getDynamic() {
        return dynamic;
    }

    public void setDynamic(Boolean dynamic) {
        this.dynamic = dynamic;
    }

    /**
     * <pre>
     * vintage 的 excise 方式，static、dynamic、ratio；
     * static表示使用静态列表，不剔除unreachable的node；dynamic完全剔除；ratio按比例提出。
     * 配置方式，ratio直接使用数字，其他使用数字0-100.
     * </pre>
     *
     * @author fishermen
     */
    public enum Excise {
        excise_static("static"), excise_dynamic("dynamic"), excise_ratio("ratio");

        private String name;

        Excise(String n) {
            this.name = n;
        }

        public String getName() {
            return name;
        }
    }

    public List<URL> toURLs() {
        String address = getAddress();
        if (StringUtils.isBlank(address)) {
            address = NetUtils.LOCALHOST + ":" + MotanConstants.DEFAULT_INT_VALUE;
        }
        Map<String, String> map = new HashMap<>();
        map.putAll(getAddressParams());
        appendConfigParams(map);
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
        if (proxyRegistry != null) {
            String proxyRegistryString = UrlUtils.urlsToString(proxyRegistry.toURLs());
            if (StringUtils.isNotBlank(proxyRegistryString)) {
                map.put(URLParamType.proxyRegistryUrlString.getName(), proxyRegistryString);
            } else {
                LoggerUtil.warn("parse proxyRegistryString is empty, proxy registry:" + proxyRegistry.getName());
            }
        }
        // address内部可能包含多个注册中心地址
        return UrlUtils.parseURLs(address, map);
    }

    public Map<String, String> getAddressParams() {
        if (StringUtils.isNotBlank(address)) {
            int index = address.indexOf("?");
            if (index > -1) {
                int end = address.length();
                if (address.contains(MotanConstants.COMMA_SEPARATOR)) {
                    end = address.indexOf(MotanConstants.COMMA_SEPARATOR);
                }
                return UrlUtils.parseQueryParams(address.substring(index + 1, end));
            }
        }
        return Collections.emptyMap();
    }
}
