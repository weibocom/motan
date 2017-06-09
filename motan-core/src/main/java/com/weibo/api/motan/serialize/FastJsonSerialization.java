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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;

import java.io.IOException;
import java.util.List;

/**
 * fastjson 序列化
 * 
 * <pre>
 * 对于嵌套场景无法支持
 * </pre>
 * 
 * @author maijunsheng
 * 
 */
@SpiMeta(name = "fastjson")
public class FastJsonSerialization implements Serialization {

    @Override
    public byte[] serialize(Object data) throws IOException {
        SerializeWriter out = new SerializeWriter();
        JSONSerializer serializer = new JSONSerializer(out);
        serializer.config(SerializerFeature.WriteEnumUsingToString, true);
        serializer.config(SerializerFeature.WriteClassName, true);
        serializer.write(data);
        return out.toBytes(null);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        return JSON.parseObject(new String(data), clz);
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        return serialize(data);
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
         List<Object> list = JSON.parseArray(new String(data), classes);
         if (list != null) {
             return list.toArray();
         }
         return null;
    }

    @Override
    public int getSerializationNumber() {
        return 2;
    }
}
