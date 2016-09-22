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
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;

import java.lang.reflect.Method;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.grpc.annotation.GrpcConfig;

public class GrpcUtil {
    private static HashMap<String, HashMap<String, MethodDescriptor>> serviceMap = new HashMap<String, HashMap<String, MethodDescriptor>>();
    
    public static ServerServiceDefinition getServiceDefByAnnotation(Class<?> clazz) throws Exception{
        ServiceDescriptor serviceDesc = getServiceDesc(getGrpcClassName(clazz));
        io.grpc.ServerServiceDefinition.Builder builder = io.grpc.ServerServiceDefinition.builder(serviceDesc);
        for(MethodDescriptor<?,?> methodDesc : serviceDesc.getMethods()){
            builder.addMethod(methodDesc, new MotanServerCallHandler());
        }
        return builder.build();
    }
    
    private static String getGrpcClassName(Class<?> clazz){
        GrpcConfig config = clazz.getAnnotation(GrpcConfig.class);
        if(config == null || StringUtils.isBlank(config.grpc())){
            throw new MotanFrameworkException("can not find grpc config in class " + clazz.getName());
        }
        return config.grpc();
    }
    
    public static ServiceDescriptor getServiceDesc(String clazzName) throws Exception{
        Class<?> clazz = Class.forName(clazzName);
        return (ServiceDescriptor) clazz.getMethod("getServiceDescriptor", null).invoke(null, null);
    }
    
    public static HashMap<String, MethodDescriptor> getMethodDescriptorByAnnotation(Class<?> clazz) throws Exception{
        String clazzName = getGrpcClassName(clazz);
        HashMap<String, MethodDescriptor> result = serviceMap.get(clazzName);
        if(result == null){
            synchronized (serviceMap) {
                if(!serviceMap.containsKey(clazzName)){
                    ServiceDescriptor serviceDesc = getServiceDesc(getGrpcClassName(clazz));
                    HashMap<String, MethodDescriptor> methodMap = new HashMap<String, MethodDescriptor>();
                    for(MethodDescriptor<?,?> methodDesc : serviceDesc.getMethods()){
                        Method interfaceMethod = getMethod(methodDesc.getFullMethodName(), clazz);
                        methodMap.put(interfaceMethod.getName(), methodDesc);
                    }
                    serviceMap.put(clazzName, methodMap);
                }
                result = serviceMap.get(clazzName);
            }
        }
        return result;
    }
    
    public static Method getMethod(String name, Class<?> interfaceClazz){
        int index = name.lastIndexOf("/");
        if(index > -1){
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        
        Method[] methods = interfaceClazz.getMethods();
        for(Method m : methods){
            if(m.getName().equalsIgnoreCase(name)){
                return m;
            }
        }
        throw new MotanFrameworkException("not find grpc method");
    }

}
