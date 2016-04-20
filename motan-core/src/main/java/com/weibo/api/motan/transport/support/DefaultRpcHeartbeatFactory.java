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

package com.weibo.api.motan.transport.support;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.HeartbeatFactory;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.RequestIdGenerator;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
@SpiMeta(name = "motan")
public class DefaultRpcHeartbeatFactory implements HeartbeatFactory {

    @Override
    public Request createRequest() {
        DefaultRequest request = new DefaultRequest();

        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(MotanConstants.HEARTBEAT_INTERFACE_NAME);
        request.setMethodName(MotanConstants.HEARTBEAT_METHOD_NAME);
        request.setParamtersDesc(MotanConstants.HHEARTBEAT_PARAM);

        return request;
    }

    @Override
    public MessageHandler wrapMessageHandler(MessageHandler handler) {
        return new HeartMessageHandleWrapper(handler);
    }

    public static boolean isHeartbeatRequest(Object message) {
        if (!(message instanceof Request)) {
            return false;
        }

        Request request = (Request) message;

        return MotanConstants.HEARTBEAT_INTERFACE_NAME.equals(request.getInterfaceName())
                && MotanConstants.HEARTBEAT_METHOD_NAME.equals(request.getMethodName())
                && MotanConstants.HHEARTBEAT_PARAM.endsWith(request.getParamtersDesc());
    }


    private class HeartMessageHandleWrapper implements MessageHandler {
        private MessageHandler messageHandler;

        public HeartMessageHandleWrapper(MessageHandler messageHandler) {
            this.messageHandler = messageHandler;
        }

        @Override
        public Object handle(Channel channel, Object message) {
            if (isHeartbeatRequest(message)) {
                DefaultResponse response = new DefaultResponse();
                response.setValue("heartbeat");
                return response;
            }

            return messageHandler.handle(channel, message);
        }


    }
}
