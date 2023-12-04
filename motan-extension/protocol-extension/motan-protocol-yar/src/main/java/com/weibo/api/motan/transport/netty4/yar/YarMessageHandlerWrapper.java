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
package com.weibo.api.motan.transport.netty4.yar;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.yar.AttachmentRequest;
import com.weibo.api.motan.protocol.yar.YarMessageRouter;
import com.weibo.api.motan.protocol.yar.YarProtocolUtil;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.netty4.http.HttpMessageHandler;
import com.weibo.api.motan.transport.netty4.http.NettyHttpUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.yar.YarProtocol;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhanglei
 * wrapper to process yar message
 * @date 2016-5-31
 */
public class YarMessageHandlerWrapper implements HttpMessageHandler {
    public static final String BAD_REQUEST = "/bad-request";
    public static final String ROOT_PATH = "/";
    public static final String STATUS_PATH = "/rpcstatus";
    protected String switcherName = MotanConstants.REGISTRY_HEARTBEAT_SWITCHER;
    private final YarMessageRouter orgHandler;

    public YarMessageHandlerWrapper(MessageHandler orgHandler) {
        if (orgHandler == null) {
            throw new MotanFrameworkException("messageHandler is null!");
        }
        if (orgHandler instanceof YarMessageRouter) {
            this.orgHandler = (YarMessageRouter) orgHandler;
        } else {
            throw new MotanFrameworkException("YarMessageHandlerWrapper can not wrapper " + orgHandler.getClass().getSimpleName());
        }
    }

    @Override
    public FullHttpResponse handle(Channel channel, FullHttpRequest httpRequest) {
        QueryStringDecoder decoder = new QueryStringDecoder(httpRequest.uri());
        String path = decoder.path();

        // check badRequest
        if (BAD_REQUEST.equals(path)) {
            return NettyHttpUtil.buildDefaultResponse("bad request!", HttpResponseStatus.BAD_REQUEST);
        }

        // service status
        if (ROOT_PATH.equals(path) || STATUS_PATH.equals(path)) {
            if (isSwitchOpen()) {// 200
                return NettyHttpUtil.buildDefaultResponse("ok!", HttpResponseStatus.OK);
            }
            //503
            return NettyHttpUtil.buildErrorResponse("service not available!");
        }

        Map<String, String> attachments = null;
        if (!decoder.parameters().isEmpty()) {
            attachments = new HashMap<>(decoder.parameters().size());
            for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
                attachments.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        YarResponse yarResponse;
        String packagerName = "JSON";
        try {
            ByteBuf buf = httpRequest.content();
            final byte[] contentBytes = new byte[buf.readableBytes()];
            buf.getBytes(0, contentBytes);
            YarRequest yarRequest = new AttachmentRequest(YarProtocol.buildRequest(contentBytes), attachments);
            yarRequest.setRequestPath(path);
            yarResponse = (YarResponse) orgHandler.handle(channel, yarRequest);
        } catch (Exception e) {
            LoggerUtil.error("YarMessageHandlerWrapper handle yar request fail.", e);
            yarResponse = YarProtocolUtil.buildDefaultErrorResponse(e.getMessage(), packagerName);
        }
        byte[] responseBytes;
        try {
            responseBytes = YarProtocol.toProtocolBytes(yarResponse);
        } catch (IOException e) {
            throw new MotanFrameworkException("convert yar response to bytes fail.", e);
        }
        FullHttpResponse httpResponse =
                new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.wrappedBuffer(responseBytes));
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded");
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());

        if (HttpUtil.isKeepAlive(httpRequest)) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        } else {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }
        return httpResponse;
    }

    /**
     * is service switcher close. http status will be 503 when switcher is close
     */
    private boolean isSwitchOpen() {
        return MotanSwitcherUtil.isOpen(switcherName);
    }
}
