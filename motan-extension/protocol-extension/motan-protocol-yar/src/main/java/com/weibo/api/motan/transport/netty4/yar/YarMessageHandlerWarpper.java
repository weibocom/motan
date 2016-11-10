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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Values;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.yar.AttachmentRequest;
import com.weibo.api.motan.protocol.yar.YarMessageRouter;
import com.weibo.api.motan.protocol.yar.YarProtocolUtil;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.yar.YarProtocol;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

/**
 * 
 * @Description wrapper to process yar message
 * @author zhanglei
 * @date 2016-5-31
 *
 */
public class YarMessageHandlerWarpper implements MessageHandler {
    private YarMessageRouter orgHandler;

    public YarMessageHandlerWarpper(MessageHandler orgHandler) {
        if (orgHandler == null) {
            throw new MotanFrameworkException("messageHandler is null!");
        }
        if (orgHandler instanceof YarMessageRouter) {
            this.orgHandler = (YarMessageRouter) orgHandler;
        } else {
            throw new MotanFrameworkException("YarMessageHandlerWarper can not wrapper " + orgHandler.getClass().getSimpleName());
        }

    }



    @Override
    public Object handle(Channel channel, Object message) {
        FullHttpRequest httpRequest = (FullHttpRequest) message;
        String uri = httpRequest.getUri();
        int index = uri.indexOf("?");// should not be null
        String requestPath = uri;
        Map<String, String> attachments = null;
        if (index > -1) {
            requestPath = uri.substring(0, index);
            if (index != uri.length() - 1) {
                attachments = getAttachMents(uri.substring(index + 1, uri.length()));
            }
        }
        YarResponse yarResponse = null;
        String packagerName = "JSON";
        try {
            ByteBuf buf = httpRequest.content();
            final byte[] contentBytes = new byte[buf.readableBytes()];
            buf.getBytes(0, contentBytes);
            YarRequest yarRequest = new AttachmentRequest(YarProtocol.buildRequest(contentBytes), attachments);
            yarRequest.setRequestPath(requestPath);
            yarResponse = (YarResponse) orgHandler.handle(channel, yarRequest);

        } catch (Exception e) {
            LoggerUtil.error("YarMessageHandlerWarpper handle yar request fail.", e);
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
        httpResponse.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");
        httpResponse.headers().set(HttpHeaders.Names.CONTENT_LENGTH, httpResponse.content().readableBytes());

        if (HttpHeaders.isKeepAlive(httpRequest)) {
            httpResponse.headers().set(HttpHeaders.Names.CONNECTION, Values.KEEP_ALIVE);
        } else {
            httpResponse.headers().set(HttpHeaders.Names.CONNECTION, Values.CLOSE);
        }

        return httpResponse;
    }

    private Map<String, String> getAttachMents(String params) {
        Map<String, String> map = new HashMap<String, String>();
        String[] paramArray = params.split("&");
        for (String param : paramArray) {
            String[] kv = param.split("=");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            } else {
                LoggerUtil.warn("yar attachment parse fail. uri param:" + param);
            }
        }
        return map;
    }

}
