/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.api.motan.registry.weibomesh;

import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryFactory;
import com.weibo.api.motan.registry.support.AbstractRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Client;
import com.weibo.api.motan.transport.EndpointFactory;
import com.weibo.api.motan.transport.HeartbeatFactory;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanglei28
 * @date 2021/2/26.
 */
public class MeshRegistry extends AbstractRegistry {
    public static final int DEFAULT_MESH_PORT = 9981;
    public static final int DEFAULT_MESH_MANAGE_PORT = 8002;
    public static final int DEFAULT_HEALTH_CHECK_RETRY = 2;
    public static final String MESH_REGISTRY_SWITCHER_NAME = "motan.weibomesh.registry.enable";
    public static final String MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME = "motan.weibomesh.registry.healthcheck.enable";
    public static final String MESH_PARAM_COPY = "copy";
    public static final String MESH_REGISTER_URL = "/registry/register";
    public static final String MESH_UNREGISTER_URL = "/registry/unregister";
    public static final String MESH_SUBSCRIBE_URL = "/registry/subscribe";
    public static final String MESH_PROXY_REGISTRY_KEY = "proxyRegistry";

    private static final String DEFAULT_HEALTH_CHECK_TIMEOUT = "3000";
    protected static final long DEFAULT_CHECK_PERIOD_MILLISECONDS = 5000;
    protected static StandardThreadExecutor executor = new StandardThreadExecutor(5, 30, 500,
            new DefaultThreadFactory("MeshRegistry", true));

    private int meshMPort; // port receive manage request
    private boolean canBackup = false; // 是否支持注册中心backup，如果不支持则所有动态能力失效。
    private boolean dynamic = false; // 是否启动动态注册、订阅能力

    private MeshTransport meshTransport; // for mesh manage
    private Client heartbeatClient; // for health check
    private HeartbeatFactory heartbeatFactory;
    private transient boolean meshAvailable = true;// mesh 是否可用。会根据mesh探活情况改变。
    private transient boolean useMesh = true; //当前是否在使用mesh。受开关状态和available状态影响。
    // mesh registry的数量应该不会多，探测线程池先按实例维度。
    private ScheduledExecutorService healthCheckExecutor = Executors.newScheduledThreadPool(1);

    public void setProxyRegistry(Registry proxyRegistry) {
        this.proxyRegistry = proxyRegistry;
        if (this.proxyRegistry != null) {
            canBackup = true;
        }
    }

    public Registry getProxyRegistry() {
        return proxyRegistry;
    }

    private Registry proxyRegistry;
    private ConcurrentHashMap<URL, MeshRegistryListener> subscribeUrlMap = new ConcurrentHashMap<>();

    static {
        MotanSwitcherUtil.switcherIsOpenWithDefault(MESH_REGISTRY_SWITCHER_NAME, getDefaultSwitcherValue(MESH_REGISTRY_SWITCHER_NAME, true));
        MotanSwitcherUtil.switcherIsOpenWithDefault(MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, getDefaultSwitcherValue(MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, true));
    }

    public MeshRegistry(URL url, MeshTransport meshTransport) {
        super(url);
        this.meshTransport = meshTransport;
        initMeshInfo();
        initProxyRegistry();
        initSwitcher();
        initHealthCheck(); // 依赖canBackup状态，最后进行
    }

    @Override
    protected void doRegister(URL url) {
        if (!MotanSwitcherUtil.isOpen(MESH_REGISTRY_SWITCHER_NAME) && canBackup) {
            proxyRegistry.register(url);
        }
        executor.execute(() -> {
            doManageRequestToMesh("http://" + getUrl().getHost() + ":" + meshMPort + MESH_REGISTER_URL, url, 2);
        });
    }

    @Override
    protected void doUnregister(URL url) {
        if (!MotanSwitcherUtil.isOpen(MESH_REGISTRY_SWITCHER_NAME) && canBackup) {
            proxyRegistry.unregister(url);
        }
        executor.execute(() -> {
            doManageRequestToMesh("http://" + getUrl().getHost() + ":" + meshMPort + MESH_UNREGISTER_URL, url, 2);
        });
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        MeshRegistryListener meshListener = subscribeUrlMap.get(url);
        if (meshListener == null) {
            meshListener = subscribeUrlMap.putIfAbsent(url, new MeshRegistryListener(this, url));
            if (meshListener == null) {
                meshListener = subscribeUrlMap.get(url);
                MeshRegistryListener finalMeshListener = meshListener;
                executor.execute(() -> {
                    doManageRequestToMesh("http://" + getUrl().getHost() + ":" + meshMPort + MESH_SUBSCRIBE_URL, url, 2);
                    if (canBackup) {
                        proxyRegistry.subscribe(url, finalMeshListener);
                    }
                });

            }
        }
        meshListener.addListener(listener);
        List<URL> urls = doDiscover(url);
        if (urls != null && urls.size() > 0) {
            listener.notify(getUrl(), urls); // 不使用AbstractRegistry的notify方法（不使用订阅缓存）。
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        MeshRegistryListener meshListener = subscribeUrlMap.get(url);
        if (meshListener != null) {
            meshListener.removeListener(listener);
        }
    }

    @Override
    protected List<URL> doDiscover(URL url) {
        MeshRegistryListener listener = subscribeUrlMap.get(url);
        if (listener == null) {// 仅在订阅时更新subscribeUrlMap。未订阅时执行discover返回MeshRegistryListener默认节点
            listener = new MeshRegistryListener(this, url);
        }
        return listener.getUrls();
    }

    @Override
    protected void doAvailable(URL url) {
        // server对外部服务状态是否可用由mesh管理
    }

    @Override
    protected void doUnavailable(URL url) {
    }

    protected void doManageRequestToMesh(String manageUrl, URL url, int retry) {
        if (meshTransport == null) {
            LoggerUtil.info("mesh transport is null in mesh registry: " + getUrl().toSimpleString());
            return;
        }
        if (!dynamic) {
            LoggerUtil.info("mesh registry dynamic is close. registry:" + getUrl().toSimpleString());
            return;
        }
        int count = 0;
        while (count <= retry) {
            try {
                URL toMeshUrl = url.createCopy();
                addRegistryParams(toMeshUrl.getParameters(), true, "filter"); // 强制添加参数
                addRegistryParams(toMeshUrl.getParameters(), false, URLParamType.proxyRegistryUrlString.getName()); //补充参数
                String meshRegistryName = toMeshUrl.getParameter(URLParamType.meshRegistryName.getName());
                if (StringUtils.isNotBlank(meshRegistryName)) { // 如果指定了mesh配置中的registry id，转换成mesh可以识别的proxyRegistry
                    toMeshUrl.addParameter(MESH_PROXY_REGISTRY_KEY, meshRegistryName);
                }
                MeshTransport.ManageResponse response = meshTransport.postManageRequest(manageUrl, Util.UrlToJson(toMeshUrl));
                if (response.getStatusCode() == 200) {
                    return;
                }
                Thread.sleep(300l);
            } catch (Exception e) {
                LoggerUtil.warn("doManageRequestToMesh fail. manageUrl:" + manageUrl + ", url:" + url.toSimpleString(), e);
            }
            count++;
        }
    }

    /**
     * @param params
     * @param force  是否强制覆盖同名参数。true：强制覆盖同名参数。false：参数不存在时补充
     * @param keys   参数名
     */
    private void addRegistryParams(Map<String, String> params, boolean force, String... keys) {
        for (String key : keys) {
            String value = getUrl().getParameter(key);
            if (StringUtils.isNotBlank(value)) {
                if (force || StringUtils.isEmpty(params.get(key))) { // 没有同名参数时补充registry默认值
                    params.put(key, value);
                }
            }
        }
    }

    protected void initMeshInfo() {
        if (getUrl().getPort() == null || getUrl().getPort() == 0) {
            getUrl().setPort(DEFAULT_MESH_PORT);
        }
        meshMPort = getUrl().getIntParameter(URLParamType.meshMPort.getName(), DEFAULT_MESH_MANAGE_PORT);
        dynamic = getUrl().getBooleanParameter(URLParamType.dynamic.getName(), false);// 默认不进行动态注册、订阅，如果要使用，需配置相关参数
    }

    protected void initProxyRegistry() {
        String proxyRegistryString = getUrl().getParameter(URLParamType.proxyRegistryUrlString.getName());
        if (StringUtils.isNotBlank(proxyRegistryString)) {
            List<URL> urls = UrlUtils.stringToURLs(proxyRegistryString);
            if (!CollectionUtil.isEmpty(urls)) {
                URL proxyUrl = urls.get(0); // 仅支持对单注册中心的代理。如果有多注册中心的强需求在考虑扩展
                RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getExtension(proxyUrl.getProtocol(), false);
                if (registryFactory == null) {
                    LoggerUtil.warn("mesh registry can not find proxy registry. proxy registry url:" + proxyUrl.toSimpleString());
                    return;
                }
                Registry registry = registryFactory.getRegistry(proxyUrl);
                if (registry != null) {
                    this.proxyRegistry = registry;
                    canBackup = true;
                    LoggerUtil.info("mesh registry add proxy registry. url:" + getUrl().toFullStr());
                }
            }
        }
    }


    // client侧
    protected synchronized void enableMesh(boolean enable) {
        if (useMesh != enable) {// 状态不同时进行变更
            try {
                boolean needChange = false;
                if (enable && MotanSwitcherUtil.isOpen(MESH_REGISTRY_SWITCHER_NAME) && meshAvailable) {
                    // 如果是启用mesh，需要满足开关打开 且 mesh为可用状态
                    needChange = true;
                } else if (!enable && (!MotanSwitcherUtil.isOpen(MESH_REGISTRY_SWITCHER_NAME) || !meshAvailable)) {
                    // 如果是停用mesh，需要满足开关关闭 或 mesh为不可用状态
                    needChange = true;
                }
                if (needChange) {
                    notifyAll(enable);
                    useMesh = enable; // 通知后改变状态
                    LoggerUtil.info("mesh registry change useMesh to " + enable + ", registry:" + getUrl().toSimpleString());
                }
            } catch (Exception e) {
                LoggerUtil.error("enable mesh fail. enable:" + enable, e);
            }
        }
    }

    // server侧
    protected synchronized void enableMeshService(boolean enable) {
        // notice：
        // 只处理服务注册与注销，不控制service的available状态，available由业务方通过开关控制
        // 本地服务是否启用不与mesh进行联动（降级有可能就是因为mesh挂掉），mesh的服务注册、注销由mesh单独管理。
        try {
            if (enable) { // mesh作为server时，本地不在对远程直接提供服务，注销本地service。
                for (URL url : getRegisteredServiceUrls()) {
                    proxyRegistry.unregister(url);
                }
            } else { // 降级mesh时，转为由本地service直接提供服务
                for (URL url : getRegisteredServiceUrls()) {
                    proxyRegistry.register(url);
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("enable mesh service fail. enable:" + enable, e);
        }
    }

    public boolean isUseMesh() {
        return useMesh;
    }

    // for test
    public void setUseMesh(boolean value) {
        useMesh = value;
    }

    private void notifyAll(boolean useMesh) {
        LoggerUtil.info("MeshRegistry will notify，useMesh:" + useMesh
                + ", mesh switcher:" + MotanSwitcherUtil.isOpen(MESH_REGISTRY_SWITCHER_NAME)
                + ", mesh available:" + meshAvailable);
        for (MeshRegistryListener listener : subscribeUrlMap.values()) {
            listener.doNotify(useMesh);
        }
    }

    /**
     * 初始化MeshRegistry相关开关。
     * 默认值支持通过环境变量进行设置。
     * 如果需要自定义设置，需要在执行此方法前自行注册开关。
     * <p>
     * notice : 开关为全局开关，每个registry实例注册一个SwitcherListener.
     */
    protected void initSwitcher() {
        MotanSwitcherUtil.registerSwitcherListener(MESH_REGISTRY_SWITCHER_NAME, (key, value) -> {
            if (!canBackup) { // 不能降级的情况下仅响应开关，不执行降级动作，方便确认canbackup状态。
                LoggerUtil.warn("mesh registry can not backup, ignore switcher value：" + value + ", url:" + getUrl().toSimpleString());
                return;
            }
            if (value != null) {
                enableMesh(value);
                // notice： serice的状态只受降级开关控制，不与mesh健康状态绑定，mesh如果挂掉可以依靠注册中心的健康检测机制或者client侧的熔断机制来解决。
                enableMeshService(value);
            }
        });

        MotanSwitcherUtil.registerSwitcherListener(MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME, (key, value) -> {
            if (value != null && !value) { // 开关关闭时，mesh恢复可用状态
                meshAvailable = true;
                enableMesh(meshAvailable);
            }
        });
    }

    protected void initHealthCheck() {
        if (canBackup) {
            EndpointFactory endpointFactory =
                    ExtensionLoader.getExtensionLoader(EndpointFactory.class).getExtension(
                            getUrl().getParameter(URLParamType.endpointFactory.getName(), URLParamType.endpointFactory.getValue()));
            Map<String, String> meshParams = new HashMap();
            meshParams.put(URLParamType.codec.getName(), "motan2");
            meshParams.put(URLParamType.fusingThreshold.getName(), String.valueOf(Integer.MAX_VALUE));
            meshParams.put(URLParamType.requestTimeout.getName(), getUrl().getParameter(URLParamType.requestTimeout.getName(), DEFAULT_HEALTH_CHECK_TIMEOUT));
            URL meshUrl = new URL(MotanConstants.PROTOCOL_MOTAN2, getUrl().getHost(), getUrl().getPort(), "healthCheck", meshParams);
            heartbeatClient = endpointFactory.createClient(meshUrl);
            heartbeatClient.open();
            heartbeatFactory = ExtensionLoader.getExtensionLoader(HeartbeatFactory.class).getExtension(URLParamType.heartbeatFactory.getValue());

            long period = getUrl().getLongParameter(URLParamType.registrySessionTimeout.name(), DEFAULT_CHECK_PERIOD_MILLISECONDS);
            healthCheckExecutor.scheduleWithFixedDelay(this::healthCheck, period, period, TimeUnit.MILLISECONDS);

            ShutDownHook.registerShutdownHook(() -> {
                if (!healthCheckExecutor.isShutdown()) {
                    healthCheckExecutor.shutdown();
                }
            });
        }
    }

    /**
     * 只做链路层面探活，因此只针对mesh传输端口，不考虑mesh自身的200、503状态。服务状态由mesh自行控制
     * 使用motan2的心跳机制进行探活
     */
    protected void healthCheck() {
        if (MotanSwitcherUtil.isOpen(MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME)) {
            boolean health = false;
            int retry = 0; // 快速重试次数
            if (!meshAvailable) { // mesh已经是不可用状态时不重试
                retry = DEFAULT_HEALTH_CHECK_RETRY;
            }
            while (!health && retry <= DEFAULT_HEALTH_CHECK_RETRY) {
                try {
                    heartbeatClient.request(heartbeatFactory.createRequest());
                    health = true; // 只处理超时情况，有返回值就表示agent存活
                } catch (Exception e) {
                    LoggerUtil.warn("mesh health check fail. e:" + e.getMessage());
                    retry++;
                }
            }
            meshAvailable = health;
            if (!MotanSwitcherUtil.isOpen(MESH_REGISTRY_HEALTH_CHECK_SWITCHER_NAME)) {
                // 防止探测中开关变更。
                meshAvailable = true;
            }
            enableMesh(meshAvailable);
        }
    }

    private static boolean getDefaultSwitcherValue(String name, boolean defaultValue) {
        boolean value = defaultValue;
        String envValue = System.getenv(name);
        if (StringUtils.isNotBlank(envValue)) {
            value = Boolean.parseBoolean(envValue);
            LoggerUtil.info("mesh registry switcher:" + name + " default value will be " + value + ", env value:" + envValue);
        }
        return value;
    }

}
