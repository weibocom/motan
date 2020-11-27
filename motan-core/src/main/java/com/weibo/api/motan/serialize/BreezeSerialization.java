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

package com.weibo.api.motan.serialize;

import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.breeze.BreezeBuffer;
import com.weibo.breeze.BreezeReader;
import com.weibo.breeze.BreezeWriter;

import java.io.IOException;

/**
 * Created by zhanglei28 on 2019/4/3.
 */
@SpiMeta(name = "breeze")
public class BreezeSerialization implements Serialization {
    public static int DEFAULT_BUFFER_SIZE = 1024;

    @Override
    public byte[] serialize(Object o) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(DEFAULT_BUFFER_SIZE);
        BreezeWriter.writeObject(buffer, o);
        buffer.flip();
        return buffer.getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(bytes);
        return BreezeReader.readObject(buffer, clz);
    }

    @Override
    public byte[] serializeMulti(Object[] objects) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(DEFAULT_BUFFER_SIZE);
        for (Object o: objects){
            BreezeWriter.writeObject(buffer, o);
        }
        buffer.flip();
        return buffer.getBytes();
    }

    @Override
    public Object[] deserializeMulti(byte[] bytes, Class<?>[] classes) throws IOException {
        Object[] objects = new Object[classes.length];
        BreezeBuffer buffer = new BreezeBuffer(bytes);
        for (int i = 0; i < classes.length; i++) {
            objects[i] = BreezeReader.readObject(buffer, classes[i]);
        }
        return objects;
    }

    @Override
    public int getSerializationNumber() {
        return 8;
    }
}
