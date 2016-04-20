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

package com.weibo.api.motan.transport.netty;

import java.io.IOException;

import com.weibo.api.motan.codec.AbstractCodec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.rpc.DefaultRpcCodec;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;

@SpiMeta(name = "mockMotan")
public class MockDefaultRpcCodec extends AbstractCodec {
    private DefaultRpcCodec codec = new DefaultRpcCodec();

    private static final byte MASK = 0x07;

    @Override
    public byte[] encode(Channel channel, Object message) throws IOException {
        return codec.encode(channel, message);
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] buffer) throws IOException {
        Object result = codec.decode(channel, remoteIp, buffer);

        if (result instanceof Response) {
            DefaultResponse object = (DefaultResponse) result;

            byte flag = buffer[3];
            byte dataType = (byte) (flag & MASK);
            boolean isResponse = (dataType != MotanConstants.FLAG_REQUEST);

            if (object.getException() == null) {
                if (isResponse && object.getValue().equals("error")) {
                    DefaultResponse response = (DefaultResponse) object;
                    response.setException(new MotanFrameworkException("decode error: response dataType not support " + dataType,
                            MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR));
                    return response;
                } else {
                    throw new MotanFrameworkException(MotanErrorMsgConstant.FRAMEWORK_DECODE_ERROR);
                }
            }
            return object;
        }

        return result;
    }
}
