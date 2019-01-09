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

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;

import java.io.*;
import java.lang.reflect.Method;

/**
 * protobuf序列化器,支持基本数据类型及其包装类、String、Throwable、Protobuf2/3对象
 *
 * @author zhouhaocheng
 */
@SpiMeta(name = "protobuf")
public class ProtobufSerialization implements Serialization {
    @Override
    public byte[] serialize(Object obj) throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        // 兼容motan1 协议，对throwable使用 java ObjectOutputStream进行序列化
        if (Throwable.class.isAssignableFrom(obj.getClass())) {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.flush();
        } else {
            CodedOutputStream output = CodedOutputStream.newInstance(baos);
            serialize(output, obj);
            output.flush();
        }

        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        // 兼容motan1 协议，对throwable使用 java ObjectInputStream进行反序列化
        if (Throwable.class.isAssignableFrom(clazz)) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                return (T) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new MotanFrameworkException(e);
            }
        } else {
            CodedInputStream in = CodedInputStream.newInstance(bytes);
            return (T) deserialize(in, clazz);
        }

    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        CodedOutputStream output = CodedOutputStream.newInstance(baos);
        for (Object obj : data) {
            serialize(output, obj);
        }
        output.flush();
        return baos.toByteArray();
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(data);
        Object[] objects = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            objects[i] = deserialize(in, classes[i]);
        }
        return objects;
    }

    protected void serialize(CodedOutputStream output, Object obj) throws IOException {

        if (obj == null) {
            output.writeBoolNoTag(true);
            ;
        } else {
            output.writeBoolNoTag(false);
            Class<?> clazz = obj.getClass();
            if (clazz == int.class || clazz == Integer.class) {
                output.writeSInt32NoTag((Integer) obj);
            } else if (clazz == long.class || clazz == Long.class) {
                output.writeSInt64NoTag((Long) obj);
            } else if (clazz == boolean.class || clazz == Boolean.class) {
                output.writeBoolNoTag((Boolean) obj);
            } else if (clazz == byte.class || clazz == Byte.class) {
                output.writeRawByte((Byte) obj);
            } else if (clazz == char.class || clazz == Character.class) {
                output.writeSInt32NoTag((Character) obj);
            } else if (clazz == short.class || clazz == Short.class) {
                output.writeSInt32NoTag((Short) obj);
            } else if (clazz == double.class || clazz == Double.class) {
                output.writeDoubleNoTag((Double) obj);
            } else if (clazz == float.class || clazz == Float.class) {
                output.writeFloatNoTag((Float) obj);
            } else if (clazz == String.class) {
                output.writeStringNoTag(obj.toString());
            } else if (MessageLite.class.isAssignableFrom(clazz)) {
                output.writeMessageNoTag((MessageLite) obj);
            } else {
                throw new IllegalArgumentException("can't serialize " + clazz);
            }
        }

    }

    protected Object deserialize(CodedInputStream in, Class<?> clazz) throws IOException {
        if (in.readBool()) {
            return null;
        } else {
            Object value;
            if (clazz == int.class || clazz == Integer.class) {
                value = in.readSInt32();
            } else if (clazz == long.class || clazz == Long.class) {
                value = in.readSInt64();
            } else if (clazz == boolean.class || clazz == Boolean.class) {
                value = in.readBool();
            } else if (clazz == byte.class || clazz == Byte.class) {
                value = in.readRawByte();
            } else if (clazz == char.class || clazz == Character.class) {
                value = (char) in.readSInt32();
            } else if (clazz == short.class || clazz == Short.class) {
                value = (short) in.readSInt32();
            } else if (clazz == double.class || clazz == Double.class) {
                value = in.readDouble();
            } else if (clazz == float.class || clazz == Float.class) {
                value = in.readFloat();
            } else if (clazz == String.class) {
                value = in.readString();
            } else if (MessageLite.class.isAssignableFrom(clazz)) {

                try {
                    Method method = clazz.getDeclaredMethod("newBuilder", null);
                    MessageLite.Builder builder = (MessageLite.Builder) method.invoke(null, null);
                    in.readMessage(builder, null);
                    value = builder.build();
                } catch (Exception e) {
                    throw new MotanFrameworkException(e);
                }
            } else {
                throw new IllegalArgumentException("can't serialize " + clazz);
            }

            return value;
        }
    }

    @Override
    public int getSerializationNumber() {
        return 5;
    }
}
