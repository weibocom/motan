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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.util.JsonFormat;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.grpc.annotation.GrpcConfig;
import io.grpc.MethodDescriptor;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * 
 * @Description TODO
 * @author zhanglei
 * @date Oct 13, 2016
 *
 */
public class GrpcUtil {
    @SuppressWarnings("rawtypes")
    private static HashMap<String, HashMap<String, MethodDescriptor>> serviceMap = new HashMap<String, HashMap<String, MethodDescriptor>>();
    public static final String JSON_CODEC = "grpc-pb-json";
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ServerServiceDefinition getServiceDefByAnnotation(Class<?> clazz) throws Exception {
        ServiceDescriptor serviceDesc = getServiceDesc(getGrpcClassName(clazz));
        io.grpc.ServerServiceDefinition.Builder builder = io.grpc.ServerServiceDefinition.builder(serviceDesc);
        for (MethodDescriptor<?, ?> methodDesc : serviceDesc.getMethods()) {
            builder.addMethod(methodDesc, new MotanServerCallHandler());
        }
        return builder.build();
    }

    private static String getGrpcClassName(Class<?> clazz) {
        GrpcConfig config = clazz.getAnnotation(GrpcConfig.class);
        if (config == null || StringUtils.isBlank(config.grpc())) {
            throw new MotanFrameworkException("can not find grpc config in class " + clazz.getName());
        }
        return config.grpc();
    }

    public static ServiceDescriptor getServiceDesc(String clazzName) throws Exception {
        Class<?> clazz = Class.forName(clazzName);
        return (ServiceDescriptor) clazz.getMethod("getServiceDescriptor", null).invoke(null, null);
    }

    @SuppressWarnings("rawtypes")
    public static HashMap<String, MethodDescriptor> getMethodDescriptorByAnnotation(Class<?> clazz, String serialization) throws Exception {
        String clazzName = getGrpcClassName(clazz);
        HashMap<String, MethodDescriptor> result = serviceMap.get(clazzName);
        if (result == null) {
            synchronized (serviceMap) {
                if (!serviceMap.containsKey(clazzName)) {
                    ServiceDescriptor serviceDesc = getServiceDesc(getGrpcClassName(clazz));
                    HashMap<String, MethodDescriptor> methodMap = new HashMap<String, MethodDescriptor>();
                    for (MethodDescriptor<?, ?> methodDesc : serviceDesc.getMethods()) {
                        Method interfaceMethod = getMethod(methodDesc.getFullMethodName(), clazz);
                        if(JSON_CODEC.equals(serialization)){
                            methodDesc = convertJsonDescriptor(methodDesc, interfaceMethod.getParameterTypes()[0], interfaceMethod.getReturnType());
                        }
                        methodMap.put(interfaceMethod.getName(), methodDesc);
                    }
                    serviceMap.put(clazzName, methodMap);
                }
                result = serviceMap.get(clazzName);
            }
        }
        return result;
    }

    public static Method getMethod(String name, Class<?> interfaceClazz) {
        int index = name.lastIndexOf("/");
        if (index > -1) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }

        Method[] methods = interfaceClazz.getMethods();
        for (Method m : methods) {
            if (m.getName().equalsIgnoreCase(name)) {
                return m;
            }
        }
        throw new MotanFrameworkException("not find grpc method");
    }

    public static MethodDescriptor convertJsonDescriptor(MethodDescriptor oldDesc, Class req, Class res){
        MethodDescriptor.Marshaller reqMarshaller = getJsonMarshaller(req);
        MethodDescriptor.Marshaller resMarshaller = getJsonMarshaller(res);
        if(reqMarshaller != null && resMarshaller != null){
            return MethodDescriptor.create(oldDesc.getType(), oldDesc.getFullMethodName(), reqMarshaller, resMarshaller);
        }
        return null;
    }

    public static MethodDescriptor.Marshaller getJsonMarshaller(Class clazz) {
        try {
            if (MessageLite.class.isAssignableFrom(clazz)) {
                Method method = clazz.getDeclaredMethod("getDefaultInstance", null);
                Message message = (Message) method.invoke(null, null);
                return jsonMarshaller(message);
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public static <T extends Message> MethodDescriptor.Marshaller<T> jsonMarshaller(final T defaultInstance) {
        final JsonFormat.Printer printer = JsonFormat.printer().preservingProtoFieldNames();
        final JsonFormat.Parser parser = JsonFormat.parser();
        final Charset charset = Charset.forName("UTF-8");

        return new MethodDescriptor.Marshaller<T>() {
            @Override
            public InputStream stream(T value) {
                try {
                    return new ByteArrayInputStream(printer.print(value).getBytes(charset));
                } catch (InvalidProtocolBufferException e) {
                    throw Status.INTERNAL
                            .withCause(e)
                            .withDescription("Unable to print json proto")
                            .asRuntimeException();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public T parse(InputStream stream) {
                Message.Builder builder = defaultInstance.newBuilderForType();
                Reader reader = new InputStreamReader(stream, charset);
                T proto;
                try {
                    parser.merge(reader, builder);
                    proto = (T) builder.build();
                    reader.close();
                } catch (InvalidProtocolBufferException e) {
                    throw Status.INTERNAL.withDescription("Invalid protobuf byte sequence")
                            .withCause(e).asRuntimeException();
                } catch (IOException e) {
                    // Same for now, might be unavailable
                    throw Status.INTERNAL.withDescription("Invalid protobuf byte sequence")
                            .withCause(e).asRuntimeException();
                }
                return proto;
            }
        };
    }


}
