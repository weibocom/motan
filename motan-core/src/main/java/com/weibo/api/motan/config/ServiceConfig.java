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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.config.annotation.ConfigDesc;
import com.weibo.api.motan.config.handler.ConfigHandler;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MetaUtil;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StringTools;

/**
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
public class ServiceConfig<T> extends AbstractServiceConfig {

    private static final long serialVersionUID = -3342374271064293224L;
    private static final ConcurrentHashSet<String> existingServices = new ConcurrentHashSet<>();
    private static final AtomicBoolean initEnv = new AtomicBoolean(false);
    private static final Map<Pattern, String> serviceGroupMapFromEnv = new HashMap<>();

    // 具体到方法的配置
    protected List<MethodConfig> methods;

    // 接口实现类引用
    private T ref;

    // service 对应的exporters，用于管理service服务的生命周期
    private final List<Exporter<T>> exporters = new CopyOnWriteArrayList<>();
    private Class<T> interfaceClass;
    private BasicServiceInterfaceConfig basicService;
    private final AtomicBoolean exported = new AtomicBoolean(false);

    public static ConcurrentHashSet<String> getExistingServices() {
        return existingServices;
    }

    public Class<?> getInterface() {
        return interfaceClass;
    }

    public void setInterface(Class<T> interfaceClass) {
        if (interfaceClass != null && !interfaceClass.isInterface()) {
            throw new IllegalStateException("The interface class " + interfaceClass + " is not a interface!");
        }
        this.interfaceClass = interfaceClass;
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    public void setMethods(MethodConfig methods) {
        this.methods = Collections.singletonList(methods);
    }

    public void setMethods(List<MethodConfig> methods) {
        this.methods = methods;
    }

    public boolean hasMethods() {
        return this.methods != null && !this.methods.isEmpty();
    }

    public T getRef() {
        return ref;
    }

    public void setRef(T ref) {
        this.ref = ref;
    }

    public List<Exporter<T>> getExporters() {
        return Collections.unmodifiableList(exporters);
    }

    protected boolean serviceExists(URL url) {
        return existingServices.contains(url.getIdentity());
    }

    public synchronized void export() {
        if (exported.get()) {
            LoggerUtil.warn(String.format("%s has already been exported, so ignore the export request!", interfaceClass.getName()));
            return;
        }

        checkInterfaceAndMethods(interfaceClass, methods);

        loadRegistryUrls();
        if (registryUrls == null || registryUrls.isEmpty()) {
            throw new IllegalStateException("Should set registry config for service:" + interfaceClass.getName());
        }

        Map<String, Integer> protocolPorts = getProtocolAndPort();
        for (ProtocolConfig protocolConfig : protocols) {
            Integer port = protocolPorts.get(protocolConfig.getId());
            if (port == null) {
                throw new MotanServiceException(String.format("Unknown port in service:%s, protocol:%s", interfaceClass.getName(),
                        protocolConfig.getId()));
            }
            doExport(protocolConfig, port);
        }

        afterExport();
    }

    public synchronized void unexport() {
        if (!exported.get()) {
            return;
        }
        try {
            ConfigHandler configHandler =
                    ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(MotanConstants.DEFAULT_VALUE);
            configHandler.unexport(exporters, registryUrls);
        } finally {
            afterUnexport();
        }
    }

    private void doExport(ProtocolConfig protocolConfig, int port) {
        String protocolName = protocolConfig.getName();
        if (protocolName == null || protocolName.isEmpty()) {
            protocolName = URLParamType.protocol.getValue();
        }

        String hostAddress = host;
        if (StringUtils.isBlank(hostAddress) && basicService != null) {
            hostAddress = basicService.getHost();
        }
        if (NetUtils.isInvalidLocalHost(hostAddress)) {
            hostAddress = getLocalHostAddress();
        }

        Map<String, String> map = new HashMap<>();

        map.put(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
        map.put(URLParamType.refreshTimestamp.getName(), String.valueOf(System.currentTimeMillis()));

        collectConfigParams(map, protocolConfig, basicService, extConfig, this);
        collectMethodConfigParams(map, this.getMethods());

        URL serviceUrl = new URL(protocolName, hostAddress, port, interfaceClass.getName(), map);
        // add server side meta info to the url, so these meta info can be passed to the client side through the registration mechanism.
        if (serviceUrl.getBooleanParameter(URLParamType.registerMeta.getName(), URLParamType.registerMeta.getBooleanValue())) {
            MetaUtil.addStaticMeta(serviceUrl);
        }
        String groupString = serviceUrl.getParameter(URLParamType.group.getName(), ""); // do not with default group value
        String additionalGroup = System.getenv(MotanConstants.ENV_ADDITIONAL_GROUP);
        if (StringUtils.isNotBlank(additionalGroup)) { // check additional groups
            groupString = StringUtils.isBlank(groupString) ? additionalGroup : groupString + "," + additionalGroup;
            serviceUrl.addParameter(URLParamType.group.getName(), groupString);
        }

        String serverMode = System.getenv(MotanConstants.ENV_MOTAN_SERVER_MODE);
        // change service group in sandbox mode.
        if (MotanConstants.MOTAN_SERVER_MODE_SANDBOX.equals(serverMode)) {
            LoggerUtil.info("motan server start in sandbox mode.");
            // change groups by env map
            if (!initEnv.get()) {
                initChangeGroupEnv();
            }
            // First get the sandbox group name from the environment variable
            String changeGroups = getChangeGroupFromEnv(serviceUrl.getPath());
            if (changeGroups == null) {
                // if not found, get the sandbox group name from the url parameter
                changeGroups = serviceUrl.getParameter(URLParamType.sandboxGroups.getName(), "");
            }
            if (StringUtils.isBlank(changeGroups)) {
                // The sandbox group name must be specified in sandbox mode
                LoggerUtil.error("can not find sandbox group name in sandbox mode. service url:" + serviceUrl.toSimpleString());
                throw new MotanServiceException("can not find sandbox group name in sandbox mode. service url:" + serviceUrl.toSimpleString());
            }
            groupString = changeGroups.replace(MotanConstants.SUFFIX_STRING, serviceUrl.getGroup());
            serviceUrl.addParameter(URLParamType.group.getName(), groupString);
            LoggerUtil.info("change register group in sandbox mode, serviceUrl:" + serviceUrl.toSimpleString()
                        + ", change to groups:" + groupString);
        }

        // check multi group.
        if (groupString.contains(MotanConstants.COMMA_SEPARATOR)) {
            for (String group : StringTools.splitSet(groupString, MotanConstants.COMMA_SEPARATOR)) {
                URL newGroupServiceUrl = serviceUrl.createCopy();
                newGroupServiceUrl.addParameter(URLParamType.group.getName(), group);
                exportService(hostAddress, protocolName, newGroupServiceUrl);
            }
        } else {
            exportService(hostAddress, protocolName, serviceUrl);
        }
    }

    private synchronized void initChangeGroupEnv(){
       if (!initEnv.get()) {
        // init ENV_MOTAN_CHANGE_REG_GROUPS
        String changeGroups = System.getenv(MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS);
        if (StringUtils.isNotBlank(changeGroups)) {
            LoggerUtil.info("init env " + MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS + ", value:" + changeGroups);
            try {
                JSONArray configs = JSON.parseArray(changeGroups);
                for (Object config : configs) {
                    JSONObject configJson = (JSONObject) config;
                    String group = configJson.getString("group");
                    String service = configJson.getString("service");
                    if (StringUtils.isNotBlank(group) && StringUtils.isNotBlank(service)) {
                        serviceGroupMapFromEnv.put(Pattern.compile(service), group);
                        LoggerUtil.info(String.format("add change group env, service:%s, group:%s", service, group));
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("parse env " + MotanConstants.ENV_MOTAN_CHANGE_REG_GROUPS + " fail, value:" + changeGroups, e);
            }
        }
        initEnv.set(true);
       }
    }

    private String getChangeGroupFromEnv(String service) {
        for (Map.Entry<Pattern, String> entry : serviceGroupMapFromEnv.entrySet()) {
            if (entry.getKey().matcher(service).matches()) {
                return entry.getValue();
            }
        }
        return null;
    }

    // only for test
    protected static void clearChangeGroupFromEnv() {
        serviceGroupMapFromEnv.clear();
        initEnv.set(false);
    }

    private void exportService(String hostAddress, String protocol, URL serviceUrl) {
        // Check if there is a suffix that needs to be appended
        String appendSuffix = System.getenv(MotanConstants.ENV_RPC_REG_GROUP_SUFFIX);
        if (StringUtils.isNotBlank(appendSuffix) && !serviceUrl.getGroup().endsWith(appendSuffix)
                && !MotanConstants.PROTOCOL_INJVM.equals(protocol)) {
            // if origin group not end with appendSuffix, and not inJvm protocol, add it.
            serviceUrl.addParameter(URLParamType.group.getName(), serviceUrl.getGroup() + appendSuffix);
        }
        if (serviceExists(serviceUrl)) {
            LoggerUtil.warn(String.format("%s configService is malformed, for same service (%s) already exists ", interfaceClass.getName(),
                    serviceUrl.getIdentity()));
            return; // not treat as exception
        }
        LoggerUtil.info("export for service url :" + serviceUrl.toFullStr());
        List<URL> urls = new ArrayList<>();

        // inJvm 协议只支持注册到本地，其他协议可以注册到local、remote
        if (MotanConstants.PROTOCOL_INJVM.equals(protocol)) {
            URL localRegistryUrl = null;
            for (URL ru : registryUrls) {
                if (MotanConstants.REGISTRY_PROTOCOL_LOCAL.equals(ru.getProtocol())) {
                    localRegistryUrl = ru.createCopy();
                    break;
                }
            }
            if (localRegistryUrl == null) {
                localRegistryUrl =
                        new URL(MotanConstants.REGISTRY_PROTOCOL_LOCAL, hostAddress, MotanConstants.DEFAULT_INT_VALUE,
                                RegistryService.class.getName());
            }

            urls.add(localRegistryUrl);
        } else {
            for (URL ru : registryUrls) {
                urls.add(ru.createCopy());
            }
        }

        ConfigHandler configHandler = ExtensionLoader.getExtensionLoader(ConfigHandler.class).getExtension(MotanConstants.DEFAULT_VALUE);

        exporters.add(configHandler.export(interfaceClass, ref, urls, serviceUrl));
    }

    private void afterExport() {
        exported.set(true);
        for (Exporter<T> ep : exporters) {
            String id = ep.getProvider().getUrl().getIdentity();
            existingServices.add(id);
            GlobalRuntime.addExporter(id, ep);
        }
    }

    private void afterUnexport() {
        exported.set(false);
        for (Exporter<T> ep : exporters) {
            String id = ep.getProvider().getUrl().getIdentity();
            existingServices.remove(id);
            GlobalRuntime.removeExporter(id);
        }
        exporters.clear();
    }

    @ConfigDesc(excluded = true)
    public BasicServiceInterfaceConfig getBasicService() {
        return basicService;
    }

    public void setBasicService(BasicServiceInterfaceConfig basicService) {
        this.basicService = basicService;
    }

    public Map<String, Integer> getProtocolAndPort() {
        if (StringUtils.isBlank(export)) {
            throw new MotanServiceException("export should not empty in service config:" + interfaceClass.getName());
        }
        return ConfigUtil.parseExport(this.export);
    }

    @ConfigDesc(excluded = true)
    public String getHost() {
        return host;
    }

    public AtomicBoolean getExported() {
        return exported;
    }
}
