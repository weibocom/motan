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

import io.grpc.BindableService;
import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import com.weibo.api.motan.rpc.Provider;
/**
 * 
 * @Description MotanHandlerRegistry
 * @author zhanglei
 * @date Oct 13, 2016
 *
 */
public class MotanHandlerRegistry extends HandlerRegistry {
    private ConcurrentHashMap<String, ServerMethodDefinition<?, ?>> methods = new ConcurrentHashMap<String, ServerMethodDefinition<?, ?>>();

    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, String authority) {
        return methods.get(methodName);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void addService(ServerServiceDefinition service, Provider provider) {

        for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
            Method providerMethod = GrpcUtil.getMethod(method.getMethodDescriptor().getFullMethodName(), provider.getInterface());
            MotanServerCallHandler handler;
            if (method.getServerCallHandler() instanceof MotanServerCallHandler) {
                handler = (MotanServerCallHandler) method.getServerCallHandler();
            } else {
                handler = new MotanServerCallHandler();
                method = method.withServerCallHandler(handler);
            }
            handler.init(provider, providerMethod);
            methods.put(method.getMethodDescriptor().getFullMethodName(), method);
        }
    }

    @SuppressWarnings("rawtypes")
    public void addService(BindableService bindableService, Provider provider) {
        addService(bindableService.bindService(), provider);
    }

    public void removeService(ServerServiceDefinition service) {
        if (service != null) {
            for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
                methods.remove(method.getMethodDescriptor().getFullMethodName());
            }
        }
    }



}
