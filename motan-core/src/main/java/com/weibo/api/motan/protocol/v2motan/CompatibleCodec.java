/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.protocol.v2motan;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.rpc.CompressRpcCodec;
import com.weibo.api.motan.protocol.rpc.DefaultRpcCodec;
import com.weibo.api.motan.protocol.rpc.RpcProtocolVersion;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.util.ByteUtil;

import java.io.IOException;

/**
 * Created by zhanglei28 on 2019/4/2.
 */
@SpiMeta(name = "motan-compatible")
public class CompatibleCodec implements Codec {
    private Codec v1 = new DefaultRpcCodec();
    private Codec v1Compress = new CompressRpcCodec();
    private Codec v2 = new MotanV2Codec();

    @Override
    public byte[] encode(Channel channel, Object message) throws IOException {
        if (message instanceof Response) {// depends on the request protocol in server end.
            byte version = ((Response) message).getRpcProtocolVersion();
            if (version == RpcProtocolVersion.VERSION_1.getVersion()) {
                return v1.encode(channel, message);
            } else if (version == RpcProtocolVersion.VERSION_1_Compress.getVersion()) {
                return v1Compress.encode(channel, message);
            }
        }
        return v2.encode(channel, message);// v2 codec as default.
    }

    @Override
    public Object decode(Channel channel, String remoteIp, byte[] buffer) throws IOException {
        if (buffer.length < 2) {
            throw new MotanServiceException("not enough bytes for decode. length:" + buffer.length);
        }
        short type = ByteUtil.bytes2short(buffer, 0);
        if (type == DefaultRpcCodec.MAGIC) {
            return v1Compress.decode(channel, remoteIp, buffer);
        } else if (type == MotanV2Header.MAGIC) {
            return v2.decode(channel, remoteIp, buffer);
        }
        throw new MotanFrameworkException("decode error: magic error. magic:" + type);
    }
}
