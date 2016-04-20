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

import java.util.Map;

import com.weibo.api.motan.config.annotation.ConfigDesc;

/**
 * 
 * protocol
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
public class ProtocolConfig extends AbstractConfig {

    private static final long serialVersionUID = 7605496816982926360L;

    // 服务协议
    private String name;

    // 序列化方式
    private String serialization;

    // 协议编码
    private String codec;

    // IO线程池大小
    private Integer iothreads;
    // 请求超时
    protected Integer requestTimeout;
    // client最小连接数
    protected Integer minClientConnection;
    // client最大连接数
    protected Integer maxClientConnection;
    // 最小工作pool线程数
    protected Integer minWorkerThread;
    // 最大工作pool线程数
    protected Integer maxWorkerThread;
    // 请求响应包的最大长度限制
    protected Integer maxContentLength;
    // server支持的最大连接数
    protected Integer maxServerConnection;

    // 连接池管理方式，是否lifo
    protected Boolean poolLifo;
    // 是否延迟init
    protected Boolean lazyInit;

    // endpoint factory
    protected String endpointFactory;

    // 采用哪种cluster 的实现
    protected String cluster;
    // loadbalance 方式
    protected String loadbalance;
    // high available strategy
    protected String haStrategy;
    // server worker queue size
    protected Integer workerQueueSize;
    // server accept connections count
    protected Integer acceptConnections;

    // proxy type, like jdk or javassist
    protected String proxy;
    // filter, 多个filter用","分割，blank string 表示采用默认的filter配置
    protected String filter;
    // retry count if call failure
    protected Integer retries;
    // if the request is called async, a taskFuture result will be sent back.
    protected Boolean async;

    // 是否缺省配置
    private Boolean isDefault;

    // 扩展参数
    private Map<String, String> parameters;

    @ConfigDesc(key = "protocol", required = true)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public Integer getIothreads() {
        return iothreads;
    }

    public void setIothreads(Integer iothreads) {
        this.iothreads = iothreads;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Integer getMinClientConnection() {
        return minClientConnection;
    }

    public void setMinClientConnection(Integer minClientConnection) {
        this.minClientConnection = minClientConnection;
    }

    public Integer getMaxClientConnection() {
        return maxClientConnection;
    }

    public void setMaxClientConnection(Integer maxClientConnection) {
        this.maxClientConnection = maxClientConnection;
    }

    public Integer getMinWorkerThread() {
        return minWorkerThread;
    }

    public void setMinWorkerThread(Integer minWorkerThread) {
        this.minWorkerThread = minWorkerThread;
    }

    public Integer getMaxWorkerThread() {
        return maxWorkerThread;
    }

    public void setMaxWorkerThread(Integer maxWorkerThread) {
        this.maxWorkerThread = maxWorkerThread;
    }

    public Integer getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(Integer maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public Integer getMaxServerConnection() {
        return maxServerConnection;
    }

    public void setMaxServerConnection(Integer maxServerConnection) {
        this.maxServerConnection = maxServerConnection;
    }

    public Boolean getPoolLifo() {
        return poolLifo;
    }

    public void setPoolLifo(Boolean poolLifo) {
        this.poolLifo = poolLifo;
    }

    public Boolean getLazyInit() {
        return lazyInit;
    }

    public void setLazyInit(Boolean lazyInit) {
        this.lazyInit = lazyInit;
    }

    public String getEndpointFactory() {
        return endpointFactory;
    }

    public void setEndpointFactory(String endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public String getCluster() {
        return cluster;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getLoadbalance() {
        return loadbalance;
    }

    public void setLoadbalance(String loadbalance) {
        this.loadbalance = loadbalance;
    }

    public String getHaStrategy() {
        return haStrategy;
    }

    public void setHaStrategy(String haStrategy) {
        this.haStrategy = haStrategy;
    }

    public Integer getWorkerQueueSize() {
        return workerQueueSize;
    }

    public void setWorkerQueueSize(Integer workerQueueSize) {
        this.workerQueueSize = workerQueueSize;
    }

    public Integer getAcceptConnections() {
        return acceptConnections;
    }

    public void setAcceptConnections(Integer acceptConnections) {
        this.acceptConnections = acceptConnections;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean isDefault() {
        return isDefault;
    }
}
