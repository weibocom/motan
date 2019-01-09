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
import hprose.io.HproseReader;
import hprose.io.HproseWriter;

import java.io.IOException;

/**
 * hprose 序列化，不要求序列化的对象实现 java.io.Serializable 接口，
 * 但序列化的字段需要是 public 的，或者定义有 public 的 setter 和 getter 方法。
 *
 * @author mabingyao
 * @version 创建时间：2016-8-11
 *
 */
@SpiMeta(name = "hprose")
public class HproseSerialization implements Serialization {

    @Override
    public byte[] serialize(Object data) throws IOException {
        ByteBufferStream stream = null;
        try {
            stream = new ByteBufferStream();
            HproseWriter writer = new HproseWriter(stream.getOutputStream());
            writer.serialize(data);
            byte[] result = stream.toArray();
            return result;
        }
        finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        return new HproseReader(data).unserialize(clz);
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        ByteBufferStream stream = null;
        try {
            stream = new ByteBufferStream();
            HproseWriter writer = new HproseWriter(stream.getOutputStream());
            for (Object o : data) {
                writer.serialize(o);
            }
            byte[] result = stream.toArray();
            return result;
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        HproseReader reader = new HproseReader(data);
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++){
            objects[i] = reader.unserialize(classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializationNumber() {
        return 4;
    }
}
