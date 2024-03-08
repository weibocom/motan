/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.transport.DefaultMeshClient;
import com.weibo.api.motan.transport.MeshClient;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhanglei28
 * @date 2022/12/23.
 */
public class MeshClientConfig extends AbstractConfig {
    // ---------- configuration items ------------
    // mesh proxy port
    protected Integer port;
    // mesh management port
    protected Integer mport;
    // mesh host ip
    protected String host;
    // default application for all services
    protected String application;
    protected Integer minClientConnection;
    protected Integer maxClientConnection;
    protected String serialization;
    protected Integer connectTimeout;
    protected Integer requestTimeout;
    // the maximum length limit of the request or response packet
    protected Integer maxContentLength;
    // the filters decorated on the mesh client
    protected String filter;
    protected String accessLog;
    // min gzip size
    protected Integer mingzSize;
    // specify the endpoint used, such as using unix socket
    protected String endpointFactory;
    // specify motan protocol codec (such as 'motan' using the motan1 protocol)
    protected String codec;
    protected String check;
    protected Boolean asyncInitConnection;
    protected Integer fusingThreshold;

    // ---------- internal variable ------------
    protected MeshClient meshClient;
    protected URL url;
    protected AtomicBoolean initialized = new AtomicBoolean(false);

    public MeshClient getMeshClient() {
        if (meshClient == null) {
            initMeshClient();
        }
        return meshClient;
    }

    protected synchronized void initMeshClient() {
        if (initialized.get()) {
            return;
        }
        buildMeshClientUrl();
        DefaultMeshClient defaultMeshClient = new DefaultMeshClient(url);
        try {
            defaultMeshClient.init();
        } catch (Exception e) {
            LoggerUtil.error("mesh client init fail. url:" + url.toFullStr(), e);
            boolean check = Boolean.parseBoolean(url.getParameter(URLParamType.check.getName(), URLParamType.check.getValue()));
            if (check) {
                throw e;
            }
        }
        meshClient = defaultMeshClient;
        GlobalRuntime.addMeshClient(meshClient.getUrl().getIdentity(), meshClient);
        initialized.set(true);
    }

    protected synchronized void destroy() throws Exception {
        if (meshClient != null) {
            GlobalRuntime.removeMeshClient(meshClient.getUrl().getIdentity());
            meshClient.destroy();
            meshClient = null;
        }
        initialized.set(false);
    }

    private void buildMeshClientUrl() {
        Map<String, String> params = DefaultMeshClient.getDefaultParams();
        appendConfigParams(params);
        url = new URL(params.get(URLParamType.protocol.getName()),
                host == null ? MotanConstants.MESH_DEFAULT_HOST : host,
                port == null ? MotanConstants.MESH_DEFAULT_PORT : port,
                MeshClient.class.getName(), params);
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMport() {
        return mport;
    }

    public void setMport(Integer mport) {
        this.mport = mport;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
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

    public String getSerialization() {
        return serialization;
    }

    public void setSerialization(String serialization) {
        this.serialization = serialization;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Integer getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(Integer maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(String accessLog) {
        this.accessLog = accessLog;
    }

    public Integer getMingzSize() {
        return mingzSize;
    }

    public void setMingzSize(Integer mingzSize) {
        this.mingzSize = mingzSize;
    }

    public String getEndpointFactory() {
        return endpointFactory;
    }

    public void setEndpointFactory(String endpointFactory) {
        this.endpointFactory = endpointFactory;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public String getCheck() {
        return check;
    }

    public void setCheck(String check) {
        this.check = check;
    }

    public Boolean getAsyncInitConnection() {
        return asyncInitConnection;
    }

    public void setAsyncInitConnection(Boolean asyncInitConnection) {
        this.asyncInitConnection = asyncInitConnection;
    }

    public Integer getFusingThreshold() {
        return fusingThreshold;
    }

    public void setFusingThreshold(Integer fusingThreshold) {
        this.fusingThreshold = fusingThreshold;
    }
}
