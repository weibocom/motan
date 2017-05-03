package com.weibo.api.motan.serialize;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.MessageLite;
import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Created by zhanglei28 on 2017/4/27.
 */
@SpiMeta(name = "protobuf")
public class ProtobufSerialization implements Serialization {
    @Override
    public byte[] serialize(Object obj) throws IOException {
        //TODO 支持v1版本序列化异常。
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CodedOutputStream output = CodedOutputStream.newInstance(baos);
        serialize(output, obj);
        output.flush();
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        CodedInputStream in = CodedInputStream.newInstance(bytes);
        return (T) deserialize(in, clazz);
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
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

        if(obj == null){
            output.writeBoolNoTag(true);;
        }else{
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
            } else if (clazz == double.class || clazz == Double.class) {
                output.writeDoubleNoTag((Double) obj);
            } else if (clazz == float.class || clazz == Float.class) {
                output.writeFloatNoTag((Float) obj);
            } else if (clazz == String.class) {
                output.writeStringNoTag(obj.toString());
            } else if (MessageLite.class.isAssignableFrom(clazz)) {
                output.writeMessageNoTag((MessageLite)obj);
            } else {
                throw new IllegalArgumentException("can't serialize " + clazz);
            }
        }

    }

    protected Object deserialize(CodedInputStream in, Class<?> clazz) throws IOException {
        if(in.readBool()){
            return  null;
        }else{
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
