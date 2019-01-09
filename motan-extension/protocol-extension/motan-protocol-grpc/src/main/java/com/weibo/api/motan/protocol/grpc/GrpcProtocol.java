/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.grpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.protocol.AbstractProtocol;
import com.weibo.api.motan.rpc.AbstractExporter;
import com.weibo.api.motan.rpc.AbstractReferer;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
/**
 * 
 * @Description GrpcProtocol
 * @author zhanglei
 * @date Oct 13, 2016
 *
 */
@SpiMeta(name = "grpc")
public class GrpcProtocol extends AbstractProtocol {
    protected ConcurrentHashMap<String, GrpcServer> serverMap = new ConcurrentHashMap<String, GrpcServer>();

    @Override
    protected <T> Exporter<T> createExporter(Provider<T> provider, URL url) {
        String ipPort = url.getServerPortStr();
        GrpcServer server = serverMap.get(ipPort);
        if (server == null) {
            boolean shareChannel =
                    url.getBooleanParameter(URLParamType.shareChannel.getName(), URLParamType.shareChannel.getBooleanValue());

            int workerQueueSize = url.getIntParameter(URLParamType.workerQueueSize.getName(), URLParamType.workerQueueSize.getIntValue());

            int minWorkerThread = 0, maxWorkerThread = 0;

            if (shareChannel) {
                minWorkerThread =
                        url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MIN_WORKDER);
                maxWorkerThread =
                        url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_SHARECHANNEL_MAX_WORKDER);
            } else {
                minWorkerThread =
                        url.getIntParameter(URLParamType.minWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MIN_WORKDER);
                maxWorkerThread =
                        url.getIntParameter(URLParamType.maxWorkerThread.getName(), MotanConstants.NETTY_NOT_SHARECHANNEL_MAX_WORKDER);
            }

            ExecutorService executor =
                    new StandardThreadExecutor(minWorkerThread, maxWorkerThread, workerQueueSize, new DefaultThreadFactory("GrpcServer-"
                            + url.getServerPortStr(), true));
            server = new GrpcServer(url.getPort(), shareChannel, executor);
            serverMap.putIfAbsent(ipPort, server);
            server = serverMap.get(ipPort);

        }
        return new GrpcExporter<T>(provider, url, server);
    }

    @Override
    protected <T> Referer<T> createReferer(Class<T> clz, URL url, URL serviceUrl) {
        return new GrpcReferer<T>(clz, url, serviceUrl);
    }

    class GrpcExporter<T> extends AbstractExporter<T> {
        private GrpcServer server;

        public GrpcExporter(Provider<T> provider, URL url, GrpcServer server) {
            super(provider, url);
            this.server = server;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void unexport() {
            String protocolKey = MotanFrameworkUtil.getProtocolKey(url);
            Exporter<T> exporter = (Exporter<T>) exporterMap.remove(protocolKey);

            if (exporter != null) {
                exporter.destroy();
            }
            LoggerUtil.info("GrpcExporter unexport Success: url={}", url);
        }

        @Override
        public void destroy() {
            server.safeRelease(url);
            LoggerUtil.info("GrpcExporter destory Success: url={}", url);
        }

        @Override
        protected boolean doInit() {
            try {
                server.init();
                server.addExporter(this);
                return true;
            } catch (Exception e) {
                LoggerUtil.error("grpc server init fail!", e);
            }
            return false;
        }

    }


    class GrpcReferer<T> extends AbstractReferer<T> {
        private GrpcClient client;

        public GrpcReferer(Class<T> clz, URL url, URL serviceUrl) {
            super(clz, url, serviceUrl);
            client = new GrpcClient(url, clz);
        }

        @Override
        public void destroy() {
            client.destroy();
            LoggerUtil.info("GrpcReferer destory Success: url={}", url);
        }

        @Override
        protected Response doCall(Request request) {
            return client.request(request);
        }

        @Override
        protected boolean doInit() {
            try {
                client.init();
                return true;
            } catch (Exception e) {
                LoggerUtil.error("grpc client init fail!", e);
            }
            return false;
        }

    }

}
