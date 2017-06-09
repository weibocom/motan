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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;


/**
 * 
 * @Description GrpcClient
 * @author zhanglei
 * @date Oct 13, 2016
 *
 */
public class GrpcClient {

    private URL url;
    private Class<?> interfaceClazz;
    private ManagedChannel channel;
    private CallOptions callOption = CallOptions.DEFAULT; // TODO 需要配置线程池时使用
    @SuppressWarnings("rawtypes")
    private HashMap<String, MethodDescriptor> methodDescMap;



    public GrpcClient(URL url, Class<?> interfaceClazz) {
        this.url = url;
        this.interfaceClazz = interfaceClazz;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <ReqT, RespT> Response request(final Request request) {
        MethodDescriptor<ReqT, RespT> methodDesc = methodDescMap.get(request.getMethodName());
        if (methodDesc == null) {
            throw new MotanServiceException("request method grpc descriptornot found.method:" + request.getMethodName());
        }

        int timeout =
                url.getMethodParameter(request.getMethodName(), request.getParamtersDesc(), URLParamType.requestTimeout.getName(),
                        URLParamType.requestTimeout.getIntValue());
        if (timeout < 0) {
            throw new MotanServiceException("request timeout invalid.method timeout:" + timeout);
        }
        ClientCall<ReqT, RespT> call =
                new SimpleForwardingClientCall(channel.newCall(methodDesc, callOption.withDeadlineAfter(timeout, TimeUnit.MILLISECONDS))) {

                    public void start(Listener responseListener, Metadata headers) {
                        Map<String, String> attachments = request.getAttachments();
                        if (attachments != null && !attachments.isEmpty()) {
                            for (Entry<String, String> entry : attachments.entrySet()) {
                                headers.put(Metadata.Key.of(entry.getKey(), Metadata.ASCII_STRING_MARSHALLER), entry.getValue());
                            }
                        }
                        super.start(responseListener, headers);
                    }
                };


        GrpcResponseFuture<RespT> responseFuture = new GrpcResponseFuture<RespT>(request, timeout, url, call);
        MethodType methodType = methodDesc.getType();
        switch (methodType) {
            case UNARY:
                ClientCalls.asyncUnaryCall(call, (ReqT) request.getArguments()[0], responseFuture);
                break;
            case SERVER_STREAMING:
                ClientCalls.asyncServerStreamingCall(call, (ReqT) request.getArguments()[0],
                        (io.grpc.stub.StreamObserver<RespT>) request.getArguments()[1]);
                responseFuture.onCompleted();
                break;
            case CLIENT_STREAMING:
                StreamObserver<ReqT> clientObserver =
                        ClientCalls.asyncClientStreamingCall(call, (io.grpc.stub.StreamObserver<RespT>) request.getArguments()[0]);
                responseFuture.onNext(clientObserver);
                responseFuture.onCompleted();
                break;
            case BIDI_STREAMING:
                StreamObserver<ReqT> biObserver =
                        ClientCalls.asyncBidiStreamingCall(call, (io.grpc.stub.StreamObserver<RespT>) request.getArguments()[0]);
                responseFuture.onNext(biObserver);
                responseFuture.onCompleted();
                break;
            default:
                throw new MotanServiceException("unknown grpc method type:" + methodType);
        }

        return responseFuture;
    }

    public boolean init() throws Exception {
        methodDescMap = GrpcUtil.getMethodDescriptorByAnnotation(interfaceClazz, url.getParameter(URLParamType.serialize.getName()));
        channel = ManagedChannelBuilder.forAddress(url.getHost(), url.getPort()).usePlaintext(true).build();
        return true;
    }

    public void destroy() {
        channel.shutdownNow();
    }

}
