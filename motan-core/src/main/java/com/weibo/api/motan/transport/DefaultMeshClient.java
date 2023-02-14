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

package com.weibo.api.motan.transport;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.DefaultRpcReferer;
import com.weibo.api.motan.protocol.support.ProtocolFilterDecorator;
import com.weibo.api.motan.protocol.v2motan.MotanV2Protocol;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2022/12/27.
 */
public class DefaultMeshClient implements MeshClient {
    static final DefaultMeshClient DEFAULT_MESH_CLIENT; // package-private for test
    Referer<MeshClient> innerReferer;
    private URL meshUrl;

    static {
        URL defaultUrl = new URL(MotanConstants.PROTOCOL_MOTAN2,
                MotanConstants.MESH_DEFAULT_HOST,
                MotanConstants.MESH_DEFAULT_PORT,
                MeshClient.class.getName(), getDefaultParams());
        DEFAULT_MESH_CLIENT = new DefaultMeshClient(defaultUrl); // only create instance, should be initialized before using
    }

    // global default client
    public static DefaultMeshClient getDefault() {
        if (!DEFAULT_MESH_CLIENT.isAvailable()) { // lazy init
            DEFAULT_MESH_CLIENT.init();
        }
        return DEFAULT_MESH_CLIENT;
    }

    /**
     * get default mesh client params map
     *
     * @return a new map contains default mesh client params
     */
    public static Map<String, String> getDefaultParams() {
        Map<String, String> params = new HashMap<>();
        // default value
        params.put(URLParamType.meshMPort.getName(), String.valueOf(MotanConstants.MESH_DEFAULT_MPORT));
        params.put(URLParamType.application.getName(), MotanConstants.MESH_CLIENT);
        params.put(URLParamType.group.getName(), MotanConstants.MESH_CLIENT);
        params.put(URLParamType.module.getName(), MotanConstants.MESH_CLIENT);
        params.put(URLParamType.codec.getName(), MotanConstants.PROTOCOL_MOTAN2); // motan2 as default
        params.put(URLParamType.protocol.getName(), MotanConstants.PROTOCOL_MOTAN2);
        params.put(URLParamType.fusingThreshold.getName(), String.valueOf(Integer.MAX_VALUE)); // no fusing
        return params;
    }

    // build without initializing
    public DefaultMeshClient(URL url) {
        this.meshUrl = url;
    }

    @Override
    public Class getInterface() {
        return MeshClient.class;
    }

    @Override
    public Response call(Request request) {
        return innerReferer.call(request);
    }

    @Override
    public <T> T call(Request request, Class<T> returnType) throws Exception {
        Response response = innerReferer.call(request);
        T result = null;
        if (response != null) {
            if (response.getValue() instanceof DeserializableObject) {
                try {
                    result = ((DeserializableObject) response.getValue()).deserialize(returnType);
                } catch (IOException e) {
                    LoggerUtil.error("deserialize response value fail! deserialize type:" + returnType, e);
                    throw new MotanFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
                }
            } else {
                result = (T) response.getValue();
            }
        }
        return result;
    }

    @Override
    public ResponseFuture asyncCall(Request request, Class<?> returnType) throws Exception {
        Response response = innerReferer.call(request);
        ResponseFuture result;
        if (response instanceof ResponseFuture) {
            result = (ResponseFuture) response;
            result.setReturnType(returnType);
        } else {
            result = new DefaultResponseFuture(request, 0, innerReferer.getUrl());
            if (response.getException() != null) {
                result.onFailure(response);
            } else {
                result.onSuccess(response);
            }
            result.setReturnType(returnType);
        }
        return result;
    }

    @Override
    public synchronized void init() {
        ProtocolFilterDecorator decorator = new ProtocolFilterDecorator(new MotanV2Protocol());
        innerReferer = decorator.decorateRefererFilter(new InnerMeshReferer<>(MeshClient.class, this.meshUrl), this.meshUrl);
        innerReferer.init();
    }

    @Override
    public boolean isAvailable() {
        return innerReferer != null && innerReferer.isAvailable();
    }

    @Override
    public String desc() {
        return "DefaultMeshClient - url:" + innerReferer.getUrl().toFullStr();
    }

    @Override
    public synchronized void destroy() {
        if (innerReferer != null) {
            innerReferer.destroy();
        }
    }

    @Override
    public URL getUrl() {
        return meshUrl;
    }

    static class InnerMeshReferer<MeshClient> extends DefaultRpcReferer<MeshClient> {

        public InnerMeshReferer(Class<MeshClient> clz, URL url) {
            super(clz, url, url);
        }

        @Override
        public Response call(Request request) {
            if (!isAvailable()) {
                throw new MotanFrameworkException("DefaultMeshClient call Error: mesh client is not available, url=" + url.getUri()
                        + " " + MotanFrameworkUtil.toString(request));
            }
            return doCall(request);
        }

        @Override
        protected Response doCall(Request request) {
            try {
                // group will set by MeshClientRefererInvocationHandler
                return client.request(request);
            } catch (TransportException exception) {
                throw new MotanServiceException("DefaultMeshClient call Error: url=" + url.getUri(), exception);
            }
        }
    }
}
