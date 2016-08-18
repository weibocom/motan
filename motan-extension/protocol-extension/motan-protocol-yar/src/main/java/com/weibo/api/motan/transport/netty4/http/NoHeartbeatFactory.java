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
package com.weibo.api.motan.transport.netty4.http;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.transport.HeartbeatFactory;
import com.weibo.api.motan.transport.MessageHandler;
/**
 * 
 * @Description no heartbeatFactory
 * @author zhanglei
 * @date 2016-6-8
 *
 */
@SpiMeta(name = "noHeartbeat")
public class NoHeartbeatFactory implements HeartbeatFactory {

    @Override
    public Request createRequest() {
        throw new MotanFrameworkException("cann't create request in NoHeartbeatFactory");
    }

    @Override
    public MessageHandler wrapMessageHandler(MessageHandler handler) {
        return handler;
    }

}
