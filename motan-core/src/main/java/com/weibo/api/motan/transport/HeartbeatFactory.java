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

package com.weibo.api.motan.transport;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Request;

/**
 * 
 * heartbeat的消息保持和正常请求的Request一致，这样以便更能反应service端的可用情况
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
@Spi(scope = Scope.SINGLETON)
public interface HeartbeatFactory {

    /**
     * 创建心跳包
     * 
     * @return
     */
    Request createRequest();

    /**
     * 包装 handler，支持心跳包的处理
     * 
     * @param handler
     * @return
     */
    MessageHandler wrapMessageHandler(MessageHandler handler);
}
