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
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.breeze.*;
import com.weibo.breeze.serializer.CommonSerializer;
import com.weibo.breeze.serializer.Serializer;

import java.io.IOException;

import static com.weibo.breeze.type.Types.*;

/**
 * Created by zhanglei28 on 2019/4/3.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@SpiMeta(name = "breeze")
public class BreezeSerialization implements Serialization {
    public static int DEFAULT_BUFFER_SIZE = 1024;

    @Override
    public byte[] serialize(Object o) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(DEFAULT_BUFFER_SIZE);
        if (o instanceof Throwable) { // compatible with motan1 protocol exception encoding mechanism， handle exception classes individually
            Serializer serializer = Breeze.getSerializer(o.getClass());
            if (serializer != null) {
                if (serializer instanceof CommonSerializer) { // non-customized serializer uses adaptive method
                    writeException((Throwable) o, buffer);
                } else {
                    BreezeWriter.putMessageType(buffer, serializer.getName());
                    serializer.writeToBuf(o, buffer);
                }
            } else {
                throw new BreezeException("Breeze unsupported type: " + o.getClass());
            }
        } else {
            BreezeWriter.writeObject(buffer, o);
        }
        buffer.flip();
        return buffer.getBytes();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(bytes);
        if (Throwable.class.isAssignableFrom(clz)) { // compatible with motan1 protocol exception encoding mechanism， handle exception classes individually
            Serializer serializer = Breeze.getSerializer(clz);
            if (serializer instanceof CommonSerializer) {
                // non-customized serializer uses adaptive method
                throw readToMotanException(buffer, clz);
            }
        }
        return BreezeReader.readObject(buffer, clz);
    }

    @Override
    public byte[] serializeMulti(Object[] objects) throws IOException {
        BreezeBuffer buffer = new BreezeBuffer(DEFAULT_BUFFER_SIZE);
        for (Object o : objects) {
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

    // adapt motan1 exception
    private void writeException(Throwable obj, BreezeBuffer buffer) throws BreezeException {
        BreezeWriter.putMessageType(buffer, obj.getClass().getName());
        BreezeWriter.writeMessage(buffer, () -> TYPE_STRING.writeMessageField(buffer, 1, obj.getMessage()));
    }

    private MotanServiceException readToMotanException(BreezeBuffer buffer, Class<?> clz) throws BreezeException {
        byte bType = buffer.get();
        if (bType >= MESSAGE && bType <= DIRECT_REF_MESSAGE_MAX_TYPE) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remote exception class : ")
                    .append(BreezeReader.readMessageName(buffer, bType))
                    .append(", error message : ");
            BreezeReader.readMessage(buffer, (int index) -> {
                switch (index) {
                    case 1:
                        stringBuilder.append(TYPE_STRING.read(buffer));
                        break;
                    default: //skip unknown field
                        BreezeReader.readObject(buffer, Object.class);
                }
            });
            return new MotanServiceException(stringBuilder.toString());
        }
        throw new BreezeException("Breeze not support " + bType + " with receiver type:" + clz);
    }
}
