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
package com.weibo.api.motan.protocol.grpc;

import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;

public class GrpcServer {
    //TODO 是否扩展motan的server？
    private int port;
    private MotanHandlerRegistry registry;
    private Server server;
    private Map<URL, ServerServiceDefinition> serviceDefinetions;
    private boolean init;
    private boolean shareChannel;
    private ExecutorService executor;
    
    public GrpcServer(int port) {
        super();
        this.port = port;
    }

    public GrpcServer(int port, boolean shareChannel) {
        super();
        this.port = port;
        this.shareChannel = shareChannel;
    }
    
    public GrpcServer(int port, boolean shareChannel, ExecutorService executor) {
        super();
        this.port = port;
        this.shareChannel = shareChannel;
        this.executor = executor;
    }


    public void init() throws Exception{
        if(!init){
            synchronized (this) {
                if(!init){
                    registry = new MotanHandlerRegistry();
                    serviceDefinetions = new HashMap<URL, ServerServiceDefinition>();
                    io.grpc.ServerBuilder builder = ServerBuilder.forPort(port);
                    builder.fallbackHandlerRegistry(registry);
                    if(executor != null){
                        builder.executor(executor);
                    }
                    server = builder.build();
                    server.start();
                    init = true;
                }
            }
        }
    }
    
    public void addExporter(Exporter<?> exporter) throws Exception{
        Provider provider = exporter.getProvider();        
        ServerServiceDefinition serviceDefine = GrpcUtil.getServiceDefByAnnotation(provider.getInterface());
        boolean urlShareChannel = exporter.getUrl().getBooleanParameter(URLParamType.shareChannel.getName(),
          URLParamType.shareChannel.getBooleanValue());
        synchronized (serviceDefinetions) {
            if(!(shareChannel && urlShareChannel) && !serviceDefinetions.isEmpty()){
                URL url = serviceDefinetions.keySet().iterator().next();
                throw new MotanFrameworkException("url:" + exporter.getUrl() + " cannot share channel with url:" + url);
            }
            registry.addService(serviceDefine, provider);
            serviceDefinetions.put(exporter.getUrl(), serviceDefine);
        }        
    }
    
    private ServerServiceDefinition genServiceDef(String clazzName) throws Exception{
        Class<?> clazz = Class.forName(clazzName);
        io.grpc.ServiceDescriptor desc = (ServiceDescriptor) clazz.getMethod("getServiceDescriptor", null).invoke(null, null);
        io.grpc.ServerServiceDefinition.Builder builder = io.grpc.ServerServiceDefinition.builder(desc);
        for(MethodDescriptor<?,?> methodDesc : desc.getMethods()){
            builder.addMethod(methodDesc, new MotanServerCallHandler());
        }
        return builder.build();
    }
    
    /**
     * remove service specified by url.
     * the server will be closed if all service is remove 
     * @param url
     */
    public void safeRelease(URL url){
        synchronized (serviceDefinetions) {
            registry.removeService(serviceDefinetions.remove(url));
            if(serviceDefinetions.isEmpty()){
                server.shutdown();
                if(executor != null){
                    executor.shutdownNow();
                }

            }
        }
    }

}
