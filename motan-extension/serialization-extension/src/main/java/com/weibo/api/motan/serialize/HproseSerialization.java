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

package com.weibo.api.motan.serialize;

import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import hprose.io.ByteBufferStream;
import hprose.io.HproseFormatter;
import java.io.IOException;

/**
 * hprose 序列化，要求序列化的对象实现 java.io.Serializable 接口
 *
 * @author mabingyao
 * @version 创建时间：2016-7-30
 *
 */
@SpiMeta(name = "hprose")
public class HproseSerialization implements Serialization {

    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteBufferStream stream = HproseFormatter.serialize(data);
        byte[] result = stream.toArray();
        stream.close();
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        return HproseFormatter.unserialize(data, clz);
    }
}
