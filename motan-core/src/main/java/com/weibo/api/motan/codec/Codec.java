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

package com.weibo.api.motan.codec;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.transport.Channel;

import java.io.IOException;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-21
 */
@Spi(scope = Scope.PROTOTYPE)
public interface Codec {

    byte[] encode(Channel channel, Object message) throws IOException;

    /**
     * @param channel
     * @param remoteIp 用来在server端decode request时能获取到client的ip。
     * @param buffer
     * @return
     * @throws IOException
     */
    Object decode(Channel channel, String remoteIp, byte[] buffer) throws IOException;

}
