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

package com.weibo.api.motan.common;

import com.weibo.api.motan.config.RegistryConfig.Excise;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-21
 */
public enum URLParamType {
    /**
     * version
     **/
    version("version", MotanConstants.DEFAULT_VERSION),
    /**
     * request timeout
     **/
    requestTimeout("requestTimeout", 200),
    /**
     * request id from http interface
     **/
    requestIdFromClient("requestIdFromClient", 0),
    /**
     * connect timeout
     **/
    connectTimeout("connectTimeout", 1000),
    /**
     * service min worker threads
     **/
    minWorkerThread("minWorkerThread", 20),
    /**
     * service max worker threads
     **/
    maxWorkerThread("maxWorkerThread", 200),
    /**
     * pool min conn number
     **/
    minClientConnection("minClientConnection", 2),
    /**
     * pool max conn number
     **/
    maxClientConnection("maxClientConnection", 10),
    maxConnectionPerGroup("maxConnectionPerGroup", 0),
    /**
     * pool max conn number
     **/
    maxContentLength("maxContentLength", 10 * 1024 * 1024),
    /**
     * max server conn (all clients conn)
     **/
    maxServerConnection("maxServerConnection", 100000),
    /**
     * pool conn manger strategy
     **/
    poolLifo("poolLifo", true),

    lazyInit("lazyInit", false),
    /**
     * multi referer share the same channel
     **/
    shareChannel("shareChannel", false),
    asyncInitConnection("asyncInitConnection", true),
    fusingThreshold("fusingThreshold", 10),

    // === SPI start ===

    /**
     * serialize
     **/
    serialize("serialization", "hessian2"),
    /**
     * codec
     **/
    codec("codec", "motan"),
    /**
     * endpointFactory
     **/
    endpointFactory("endpointFactory", "motan"),
    /**
     * heartbeatFactory
     **/
    heartbeatFactory("heartbeatFactory", "motan"),
    /**
     * switcherService
     **/
    switcherService("switcherService", "localSwitcherService"),

    // === SPI end ===

    group("group", "default_rpc"),
    clientGroup("clientGroup", "default_rpc"),
    accessLog("accessLog", false),

    // 0为不做并发限制
    actives("actives", 0),

    refreshTimestamp("refreshTimestamp", 0),
    nodeType("nodeType", MotanConstants.NODE_TYPE_SERVICE),

    // 格式为protocol:port
    export("export", ""),
    embed("embed", ""),

    registryRetryPeriod("registryRetryPeriod", 30 * MotanConstants.SECOND_MILLS),
    /* 注册中心不可用节点剔除方式 */
    excise("excise", Excise.excise_dynamic.getName()),
    cluster("cluster", MotanConstants.DEFAULT_VALUE),
    loadbalance("loadbalance", "activeWeight"),
    haStrategy("haStrategy", "failover"),
    protocol("protocol", MotanConstants.PROTOCOL_MOTAN),
    path("path", ""),
    host("host", ""),
    port("port", 0),
    iothreads("iothreads", Runtime.getRuntime().availableProcessors() + 1),
    workerQueueSize("workerQueueSize", 0),
    acceptConnections("acceptConnections", 0),
    proxy("proxy", MotanConstants.PROXY_JDK),
    filter("filter", ""),

    usegz("usegz", false), // 是否开启gzip压缩
    mingzSize("mingzSize", 1000), // 进行gz压缩的最小数据大小。超过此阈值才进行gz压缩


    application("application", MotanConstants.FRAMEWORK_NAME),
    module("module", MotanConstants.FRAMEWORK_NAME),

    retries("retries", 0),
    async("async", false),
    mock("mock", "false"),
    mean("mean", "2"),
    p90("p90", "4"),
    p99("p99", "10"),
    p999("p999", "70"),
    errorRate("errorRate", "0.01"),
    check("check", "true"),
    directUrl("directUrl", ""),
    registrySessionTimeout("registrySessionTimeout", MotanConstants.MINUTE_MILLS),
    slowThreshold("slowThreshold", 200),

    register("register", true),
    subscribe("subscribe", true),
    throwException("throwException", "true"),
    transExceptionStack("transExceptionStack", true),

    localServiceAddress("localServiceAddress", ""),
    // backupRequest
    backupRequestDelayTime("backupRequestDelayTime", 0),
    backupRequestDelayRatio("backupRequestDelayRatio", "0.4"),
    backupRequestSwitcherName("backupRequestSwitcherName", ""),
    backupRequestMaxRetryRatio("backupRequestMaxRetryRatio", "0.15"),

    // 切换group时，各个group的权重比。默认无权重
    weights("weights", ""),
    mixGroups("mixGroups", ""),//配置要进行混打的分组

    // 消息处理分发策略
    providerProtectedStrategy("providerProtectedStrategy", "motan"),

    // mesh registry
    dynamic("dynamic", false), // mesh registry 是否开启动态订阅、注册能力。需要mesh同时支持动态功能
    meshRegistryName("meshRegistryName", ""), // mesh配置中对应注册中心的名称
    proxyRegistryId("proxyRegistryId", ""), // mesh registry要代理的registry的bean id
    meshMPort("mport", 8002), //mesh管理端口
    needHealthCheck("needHealthCheck", true), //是否启用健康检测。如果非动态注册场景，不一定能进行自动降级，因此可以针对registry关闭
    proxyRegistryUrlString("proxyRegistryUrlString", ""), //保存要代理的的registry具体信息

    // meta info
    registerMeta("registerMeta", false), // whether to register meta info
    dynamicMeta("dynamicMeta", true), // whether to enable dynamic meta
    clusterSelector("clusterSelector", "default"), // cluster selector for ClusterGroup
    sandboxGroups("sandboxGroups", ""),
    backupGroups("backupGroups", ""),
    clusterEmptyNodeNotify("clusterEmptyNodeNotify", false), // whether to enable empty node notify to cluster
    ;


    private final String name;
    private final String value;
    private long longValue;
    private int intValue;
    private boolean boolValue;

    URLParamType(String name, String value) {
        this.name = name;
        this.value = value;
    }

    URLParamType(String name, long longValue) {
        this.name = name;
        this.value = String.valueOf(longValue);
        this.longValue = longValue;
    }

    URLParamType(String name, int intValue) {
        this.name = name;
        this.value = String.valueOf(intValue);
        this.intValue = intValue;
    }

    URLParamType(String name, boolean boolValue) {
        this.name = name;
        this.value = String.valueOf(boolValue);
        this.boolValue = boolValue;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getIntValue() {
        return intValue;
    }

    public long getLongValue() {
        return longValue;
    }

    public boolean getBooleanValue() {
        return boolValue;
    }

}
