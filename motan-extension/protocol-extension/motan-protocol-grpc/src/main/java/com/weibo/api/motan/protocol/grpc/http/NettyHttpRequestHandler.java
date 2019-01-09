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
package com.weibo.api.motan.protocol.grpc.http;

import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.protobuf.ProtoUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.Message;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.ReflectUtil;

/**
 * 
 * @Description http request handler for netty4
 * @author zhanglei
 * @date 2016-5-31
 *
 */

@Sharable
public class NettyHttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    public static final String BAD_REQUEST = "/bad-request";
    public static final String ROOT_PATH = "/";
    public static final String STATUS_PATH = "/rpcstatus";
    private ExecutorService executor;
    protected String swictherName = MotanConstants.REGISTRY_HEARTBEAT_SWITCHER;
    @SuppressWarnings("rawtypes")
    protected ConcurrentHashMap<String, Provider> providerMap = new ConcurrentHashMap<String, Provider>();
    protected ConcurrentHashMap<String, MethodInfo> methodDescMap = new ConcurrentHashMap<String, MethodInfo>();


    public NettyHttpRequestHandler(ExecutorService executor) {
        this.executor = executor;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest httpRequest) throws Exception {
        // check badRequest
        if (BAD_REQUEST.equals(httpRequest.uri())) {
            sendResponse(ctx, buildDefaultResponse("bad request!", HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // service status
        if (ROOT_PATH.equals(httpRequest.uri()) || STATUS_PATH.equals(httpRequest.uri())) {
            if (isSwitchOpen()) {// 200
                sendResponse(ctx, buildDefaultResponse("ok!", HttpResponseStatus.OK));
            } else {// 503
                sendResponse(ctx, buildErrorResponse("service not available!"));
            }
            return;
        }

        httpRequest.content().retain();

        if (executor == null) {
            processHttpRequest(ctx, httpRequest);
        } else {
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        processHttpRequest(ctx, httpRequest);
                    }
                });
            } catch (Exception e) {
                LoggerUtil.error("request is rejected by threadpool!", e);
                httpRequest.content().release();
                sendResponse(ctx, buildErrorResponse("request is rejected by threadpool!"));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void addProvider(Provider provider) {
        providerMap.put(provider.getUrl().getPath(), provider);
        Method[] methods = provider.getInterface().getMethods();
        for (Method m : methods) {
            MethodInfo newMethodInfo = new MethodInfo(m.getName(), ReflectUtil.getMethodParamDesc(m), m);
            if (methodDescMap.get(newMethodInfo.getMethodName()) == null) {
                methodDescMap.put(newMethodInfo.getMethodName(), newMethodInfo);
            } else {
                MethodInfo old = methodDescMap.get(newMethodInfo.getMethodName());
                if (!old.isDuplicate()) {
                    methodDescMap.put(old.getMethodName() + old.getMethodDesc(), old);
                    methodDescMap.put(newMethodInfo.getMethodName(), MethodInfo.DUP_METHOD);
                }
                methodDescMap.put(newMethodInfo.getMethodName() + newMethodInfo.getMethodDesc(), newMethodInfo);
            }
        }
    }

    public void removeProvider(URL url) {
        providerMap.remove(url.getPath());
    }

    @SuppressWarnings("rawtypes")
    protected void processHttpRequest(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        FullHttpResponse httpResponse = null;
        try {
            DefaultRequest rpcRequest = buildRpcRequest(httpRequest);

            String ip = NetUtils.getHostName(ctx.channel().remoteAddress());
            if(ip != null){
                rpcRequest.setAttachment(URLParamType.host.getName(), ip);
            }
            
            Provider provider = providerMap.get(rpcRequest.getInterfaceName());
            if (provider == null) {
                httpResponse = buildErrorResponse("request service not exist. service:" + rpcRequest.getInterfaceName());
            } else {
                Response response = provider.call(rpcRequest);
                httpResponse = buildHttpResponse(response, HttpUtil.isKeepAlive(httpRequest));
            }
        } catch (Exception e) {

            LoggerUtil.error("NettyHttpHandler process http request fail.", e);
            httpResponse = buildErrorResponse(e.getMessage());
        } finally {
            httpRequest.content().release();
        }
        sendResponse(ctx, httpResponse);
    }

    protected DefaultRequest buildRpcRequest(FullHttpRequest httpRequest) throws UnsupportedEncodingException {
        String uri = httpRequest.uri();
        String[] uriInfo = uri.split("\\?");
        String[] serviceInfo = uriInfo[0].split("/");
        if (serviceInfo.length != 4) {
            throw new MotanServiceException("invalid request uri! uri like '/${group}/${service}/${method}'");
        }
        DefaultRequest rpcRequest = new DefaultRequest();
        rpcRequest.setAttachment(URLParamType.group.getName(), serviceInfo[1]);
        rpcRequest.setInterfaceName(serviceInfo[2]);
        rpcRequest.setMethodName(serviceInfo[3]);

        HashMap<String, String> params = new HashMap<String, String>();
        if (uriInfo.length == 2) {
            addParams(params, uriInfo[1]);
        }
        ByteBuf buf = httpRequest.content();
        final byte[] contentBytes = new byte[buf.readableBytes()];
        buf.getBytes(0, contentBytes);
        String body = new String(contentBytes, "UTF-8");
        addParams(params, body);

        MethodInfo mi = methodDescMap.get(rpcRequest.getMethodName());
        if (mi != null && mi.isDuplicate()) {
            mi = null;
            String paramDesc = params.get("paramDesc");
            if (StringUtils.isBlank(paramDesc)) {
                throw new MotanServiceException("request method name conflict! paramDesc is required!" + rpcRequest.getMethodName());
            }
            mi = methodDescMap.get(rpcRequest.getMethodName() + paramDesc);

        }
        if (mi == null) {
            throw new MotanServiceException("request method name not found" + rpcRequest.getMethodName());
        }
        rpcRequest.setParamtersDesc(mi.getMethodDesc());
        // TODO other info
        addAttachment(rpcRequest, httpRequest.headers());
        rpcRequest.setArguments(parseArguments(params.get("params"), mi));
        return rpcRequest;
    }

    private void addAttachment(DefaultRequest rpcRequest, HttpHeaders headers) {
        for (Entry<String, String> h : headers) {
            // TODO remove unuse header
            rpcRequest.setAttachment(h.getKey(), h.getValue());
        }
    }

    // donot support param with same name
    private void addParams(Map<String, String> params, String paramStr) throws UnsupportedEncodingException {
        String[] tempArray = paramStr.split("&");
        for (String str : tempArray) {
            String[] param = str.split("=");
            if (param.length == 2) {
                params.put(param[0], URLDecoder.decode(param[1], "UTF-8"));
            }
        }
    }

    protected Object[] parseArguments(String params, MethodInfo methodInfo) {
        if (params == null) {
            return null;
        }

        Class<?>[] paramsType = methodInfo.getMethod().getParameterTypes();
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = (JsonArray) parser.parse(params);        
        try {
            Object[] result = new Object[jsonArray.size()];
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonElement element = jsonArray.get(i);
                Message pbMessage = null;
                try {
                    Method method = paramsType[i].getMethod("getDefaultInstance", null);
                    if (method != null) {
                        pbMessage = (Message) method.invoke(null, null);
                    }
                } catch (Exception e) {
                    LoggerUtil.warn("parse pb message fail. param type:" + paramsType[i]);
                }

                if (pbMessage != null) {
                    result[i] = parsePB(element.toString(), pbMessage);
                } else {
                    // TODO not pb
                }

            }
            return result;
        } catch (Exception e) {
            throw new MotanServiceException("parse arguments fail!" + e.getMessage());
        }
    }

    @SuppressWarnings("rawtypes")
    private Object parsePB(String json, Message pbMessage) throws Exception {
        Marshaller marshaller = ProtoUtils.jsonMarshaller(pbMessage);
        InputStream is = new ByteArrayInputStream(json.getBytes("UTF-8"));
        return marshaller.parse(is);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected FullHttpResponse buildHttpResponse(Response response, boolean keepAlive) throws Exception {
        Object value = response.getValue();
        byte[] responseBytes = null;
        if (value instanceof Message) {
            Marshaller marshaller =
                    ProtoUtils.jsonMarshaller((Message) value.getClass().getMethod("getDefaultInstance", null).invoke(null, null));
            InputStream is = marshaller.stream(value);
            responseBytes = new byte[is.available()];
            is.read(responseBytes);
        } else {
            // TODO not pb
        }

        FullHttpResponse httpResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseBytes));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        if (keepAlive) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        return httpResponse;
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpResponse httpResponse) {
        boolean close = false;
        try {
            ctx.write(httpResponse);
            ctx.flush();
        } catch (Exception e) {
            LoggerUtil.error("NettyHttpHandler write response fail.", e);
            close = true;
        } finally {
            // close connection
            if (close || httpResponse == null
                    || !HttpHeaderValues.KEEP_ALIVE.equals(httpResponse.headers().get(HttpHeaderNames.CONNECTION))) {
                ctx.close();
            }
        }
    }

    protected FullHttpResponse buildErrorResponse(String errMsg) {
        return buildDefaultResponse(errMsg, HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    protected FullHttpResponse buildDefaultResponse(String msg, HttpResponseStatus status) {
        FullHttpResponse errorResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(msg.getBytes()));
        return errorResponse;
    }

    /**
     * is service switcher close. http status will be 503 when switcher is close
     * 
     * @return
     */
    protected boolean isSwitchOpen() {
        return MotanSwitcherUtil.isOpen(swictherName);
    }

    static class MethodInfo {
        static final String DUPLICATION = "DUP_METHOD";
        public static MethodInfo DUP_METHOD = new MethodInfo(DUPLICATION, null, null);

        private String methodName;
        private String methodDesc;
        private Method method;

        public MethodInfo(String methodName, String methodDesc, Method method) {
            super();
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.method = method;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodDesc() {
            return methodDesc;
        }

        public Method getMethod() {
            return method;
        }

        public boolean isDuplicate() {
            return DUPLICATION.equals(methodName);
        }
    }

}
