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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;

import java.io.IOException;

/**
 * kryo 序列化
 * 只支持java, 性能比hessian2更好
 *
 * @author liaojia1
 */
@SpiMeta(name = "kryo")
public class KryoSerialization implements Serialization {

    private final Kryo kryo;

    public KryoSerialization() {
        this.kryo = new Kryo();
        kryo.setReferences(false);
    }

    @Override
    public byte[] serialize(Object data) throws IOException {
        Output output = new Output(16 * 1024, Integer.MAX_VALUE);
        kryo.writeObject(output, data);
        return output.toBytes();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clz) throws IOException {
        return kryo.readObject(new Input(data), clz);
    }
}
