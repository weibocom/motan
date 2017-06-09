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
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.ReflectUtil;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * 
 * @Description MotanServerCallHandler
 * @author zhanglei
 * @date Oct 13, 2016
 *
 * @param <Req>
 * @param <Resp>
 */
public class MotanServerCallHandler<Req, Resp> implements ServerCallHandler<Req, Resp> {

    private boolean inited = false;
    private Provider<?> provider;
    private String methodName;
    private String paramsDesc;
    private boolean requestStream = false;
    private boolean responseStream = false;
    private static Metadata.Key<String> REQUEST_ID = Metadata.Key.of("rid", Metadata.ASCII_STRING_MARSHALLER);

    public void init(Provider<?> provider, Method method) {
        checkMethod(method);
        this.provider = provider;
        this.methodName = method.getName();
        this.paramsDesc = ReflectUtil.getMethodParamDesc(method);
        inited = true;
    }

    private void checkMethod(Method method) {
        Class<?>[] paramsClazz = method.getParameterTypes();
        if (StreamObserver.class == method.getReturnType()) {
            if (paramsClazz.length != 1 || paramsClazz[0] != StreamObserver.class) {
                throw new MotanFrameworkException("invalid grpc method:" + method.getName());
            }
            this.requestStream = true;
        } else {

            if (paramsClazz.length == 2 && paramsClazz[1] == StreamObserver.class) {
                this.responseStream = true;
            }
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public Listener startCall(ServerCall call, Metadata headers) {
        if (!inited) {
            throw new MotanFrameworkException("grpc ServerCallHandler not inited!");
        }
        // TODO check header
        if (provider.getUrl().getBooleanParameter(URLParamType.usegz.getName(), URLParamType.usegz.getBooleanValue())){
            call.setCompression("gzip");
        }
        return requestStream ? streamCall(call, headers) : unaryCall(call, headers);
    }

    private <ReqT, RespT> Listener<ReqT> unaryCall(final ServerCall<ReqT, RespT> call, final Metadata headers) {
        final ServerCallStreamObserverImpl<ReqT, RespT> responseObserver = new ServerCallStreamObserverImpl<ReqT, RespT>(call);
        // see ServerCalls
        call.request(2);
        return new ServerCall.Listener<ReqT>() {
            ReqT request;

            @Override
            public void onMessage(ReqT request) {
                this.request = request;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onHalfClose() {
                if (request != null) {
                    DefaultRequest motanRequest = getBaseMotanRequest(headers);

                    String ip = NetUtils.getHostName(call.attributes().get(ServerCall.REMOTE_ADDR_KEY));
                    if (ip != null) {
                        motanRequest.setAttachment(URLParamType.host.getName(), ip);
                    }
                    if (responseStream) {
                        motanRequest.setArguments(new Object[] {request, responseObserver});
                    } else {
                        motanRequest.setArguments(new Object[] {request});
                    }
                    Response response = null;
                    try {
                        response = provider.call(motanRequest);
                        if (response.getValue() != null) {
                            responseObserver.onNext((RespT) response.getValue());
                            responseObserver.onCompleted();
                        }
                    } catch (Exception e) {
                        responseObserver.onError(e);
                        return;
                    }

                    responseObserver.freeze();
                    if (call.isReady()) {
                        onReady();
                    }
                } else {
                    call.close(Status.INTERNAL.withDescription("Half-closed without a request"), new Metadata());
                }
            }

            @Override
            public void onCancel() {
                responseObserver.cancelled = true;
                if (responseObserver.onCancelHandler != null) {
                    responseObserver.onCancelHandler.run();
                }
            }

            @Override
            public void onReady() {
                if (responseObserver.onReadyHandler != null) {
                    responseObserver.onReadyHandler.run();
                }
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <ReqT, RespT> Listener<ReqT> streamCall(final ServerCall<ReqT, RespT> call, Metadata headers) {
        final ServerCallStreamObserverImpl<ReqT, RespT> responseObserver = new ServerCallStreamObserverImpl<ReqT, RespT>(call);
        DefaultRequest request = getBaseMotanRequest(headers);
        request.setArguments(new Object[] {responseObserver});
        Response response = provider.call(request);
        final StreamObserver<ReqT> requestObserver = (StreamObserver<ReqT>) response.getValue();
        responseObserver.freeze();
        if (responseObserver.autoFlowControlEnabled) {
            call.request(1);
        }
        return new ServerCall.Listener<ReqT>() {
            boolean halfClosed = false;

            @Override
            public void onMessage(ReqT request) {
                requestObserver.onNext(request);

                if (responseObserver.autoFlowControlEnabled) {
                    call.request(1);
                }
            }

            @Override
            public void onHalfClose() {
                halfClosed = true;
                requestObserver.onCompleted();
            }

            @Override
            public void onCancel() {
                responseObserver.cancelled = true;
                if (responseObserver.onCancelHandler != null) {
                    responseObserver.onCancelHandler.run();
                }
                if (!halfClosed) {
                    requestObserver.onError(Status.CANCELLED.asException());
                }
            }

            @Override
            public void onReady() {
                if (responseObserver.onReadyHandler != null) {
                    responseObserver.onReadyHandler.run();
                }
            }
        };
    }

    private DefaultRequest getBaseMotanRequest(Metadata headers) {
        DefaultRequest request = new DefaultRequest();
        request.setMethodName(methodName);
        request.setParamtersDesc(paramsDesc);
        request.setInterfaceName(provider.getInterface().getName());
        String rid = headers.get(REQUEST_ID);
        if (rid == null) {
            rid = headers.get(Metadata.Key.of(URLParamType.requestIdFromClient.getName().toLowerCase(), Metadata.ASCII_STRING_MARSHALLER));
        }
        if (rid != null) {
            request.setAttachment(URLParamType.requestIdFromClient.getName(), rid);
        }
        // fill attachment info from headers
        Set<String> keys = headers.keys();
        for (String key : keys) {
            String value = headers.get(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER));
            if (value != null) {
                request.setAttachment(key, value);
            }
        }
        return request;
    }

    private static final class ServerCallStreamObserverImpl<ReqT, RespT> extends ServerCallStreamObserver<RespT> {
        final ServerCall<ReqT, RespT> call;
        volatile boolean cancelled;
        private boolean frozen;
        private boolean autoFlowControlEnabled = true;
        private boolean sentHeaders;
        private Runnable onReadyHandler;
        private Runnable onCancelHandler;

        ServerCallStreamObserverImpl(ServerCall<ReqT, RespT> call) {
            this.call = call;
        }

        private void freeze() {
            this.frozen = true;
        }

        @Override
        public void setMessageCompression(boolean enable) {
            call.setMessageCompression(enable);
        }

        @Override
        public void setCompression(String compression) {
            call.setCompression(compression);
        }

        @Override
        public void onNext(RespT response) {
            if (cancelled) {
                throw Status.CANCELLED.asRuntimeException();
            }
            if (!sentHeaders) {
                call.sendHeaders(new Metadata());
                sentHeaders = true;
            }
            //TODO send header from here..
            call.sendMessage(response);
        }

        @Override
        public void onError(Throwable t) {
            Metadata metadata = Status.trailersFromThrowable(t);
            if (metadata == null) {
                metadata = new Metadata();
            }
            if (t instanceof MotanBizException) {
                call.close(Status.INTERNAL.withDescription(t.getMessage()).withCause(t), metadata);
            } else {
                call.close(Status.UNAVAILABLE.withDescription(t.getMessage()).withCause(t), metadata);
            }
        }

        @Override
        public void onCompleted() {
            if (cancelled) {
                throw Status.CANCELLED.asRuntimeException();
            } else {
                call.close(Status.OK, new Metadata());
            }
        }

        @Override
        public boolean isReady() {
            return call.isReady();
        }

        @Override
        public void setOnReadyHandler(Runnable r) {
            if (frozen) {
                throw new IllegalStateException("Cannot alter onReadyHandler after initialization");
            }
            this.onReadyHandler = r;
        }

        @Override
        public boolean isCancelled() {
            return call.isCancelled();
        }

        @Override
        public void setOnCancelHandler(Runnable onCancelHandler) {
            if (frozen) {
                throw new IllegalStateException("Cannot alter onCancelHandler after initialization");
            }
            this.onCancelHandler = onCancelHandler;
        }

        @Override
        public void disableAutoInboundFlowControl() {
            if (frozen) {
                throw new IllegalStateException("Cannot disable auto flow control after initialization");
            } else {
                autoFlowControlEnabled = false;
            }
        }

        @Override
        public void request(int count) {
            call.request(count);
        }
    }

}
