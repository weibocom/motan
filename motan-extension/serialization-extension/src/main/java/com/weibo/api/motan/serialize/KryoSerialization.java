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
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.core.extension.SpiMeta;

import java.io.IOException;

/**
 * kryo 序列化
 * 只支持java, 性能比hessian2更好
 *
 * @author liaojia1
 */
@SpiMeta(name = "kryo")
@Spi(scope = Scope.SINGLETON)
public class KryoSerialization implements Serialization {

    private final KryoPool kryoPool;

    public KryoSerialization() {
        this.kryoPool = new KryoPool.Builder(new KryoFactory() {
            @Override
            public Kryo create() {
                Kryo kryo = new Kryo();
                kryo.setReferences(false);
                kryo.setCopyReferences(false);
                return kryo;
            }
        }).softReferences().build();
    }

    @Override
    public byte[] serialize(final Object data) throws IOException {
        return kryoPool.run(new KryoCallback<byte[]>() {
            @Override
            public byte[] execute(Kryo kryo) {
                Output output = new Output(8 * 1024, Integer.MAX_VALUE);
                kryo.writeObject(output, data);
                return output.toBytes();
            }
        });

    }

    @Override
    public <T> T deserialize(final byte[] data, final Class<T> clz) throws IOException {
        return kryoPool.run(new KryoCallback<T>() {
            @Override
            public T execute(Kryo kryo) {
                return kryo.readObject(new Input(data), clz);
            }
        });
    }
}
