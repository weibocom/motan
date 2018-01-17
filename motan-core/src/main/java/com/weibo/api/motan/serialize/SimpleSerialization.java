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
import com.weibo.api.motan.protocol.v2motan.GrowableByteBuffer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Created by zhanglei28 on 2017/6/8.
 * olny support Null, String, Map<String,String>, byte[]
 */
@SpiMeta(name = "simple")
public class SimpleSerialization implements Serialization {

    @Override
    public byte[] serialize(Object obj) throws IOException {
        GrowableByteBuffer buffer = new GrowableByteBuffer(4096);
        serialize(obj, buffer);
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    private void serialize(Object obj, GrowableByteBuffer buffer) throws IOException {
        if (obj != null) {
            if (obj instanceof String) {
                buffer.put((byte) 1);
                byte[] b = ((String) obj).getBytes("UTF-8");
                buffer.putInt(b.length);
                buffer.put(b);
            } else if (obj instanceof Map) {
                buffer.put((byte) 2);
                int pos = buffer.position();
                int size = 0;
                buffer.position(pos + 4);
                for (Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null
                            && (entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
                        size += putString(buffer, (String) entry.getKey());
                        size += putString(buffer, (String) entry.getValue());
                    }
                }
                buffer.position(pos);
                buffer.putInt(size);
                buffer.position(pos + size + 4);
            } else if (obj instanceof byte[]) {
                buffer.put((byte) 3);
                byte[] b = (byte[]) obj;
                buffer.putInt(b.length);
                buffer.put(b);
            } else {
                throw new MotanServiceException("SimpleSerialization not support type:" + obj.getClass());
            }
        } else {
            buffer.put((byte) 0);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return deserialize(buffer, clz);
    }

    private <T> T deserialize(ByteBuffer buffer, Class<T> clz) throws IOException {
        byte type = buffer.get();
        switch (type) {
            case 0:
                return null;
            case 1:
                if (clz == String.class || clz == Object.class) {
                    String str = getString(buffer);
                    return (T) str;
                } else {
                    throw new MotanServiceException("SimpleSerialization not support type:" + clz);
                }
            case 2:
                if (clz == Map.class || clz == Object.class) {
                    Map<String, String> map = new HashMap<String, String>();
                    int size = buffer.getInt();
                    if (size > 0) {
                        if (size > buffer.remaining()) {
                            throw new MotanServiceException("SimpleSerialization deserialize fail! buffer not enough!need size:" + size);
                        }
                        buffer.limit(buffer.position() + size);
                        String key = getString(buffer);
                        while (key != null) {
                            String value = getString(buffer);
                            if (value == null) {
                                throw new MotanServiceException("SimpleSerialization deserialize map fail! key and value not match. key:" + key);
                            } else {
                                map.put(key, value);
                            }
                            key = getString(buffer);
                        }
                    }
                    buffer.limit(buffer.capacity());
                    return (T) map;
                } else {
                    throw new MotanServiceException("SimpleSerialization not support type:" + clz);
                }
            case 3:
                if (clz == byte[].class || clz == Object.class) {
                    return (T) getBytes(buffer);
                }
        }
        return null;
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        GrowableByteBuffer buffer = new GrowableByteBuffer(4096);
        for (Object o : data) {
            serialize(o, buffer);
        }
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Object[] result = new Object[classes.length];
        for (int i = 0; i < classes.length; i++) {
            result[i] = deserialize(buffer, classes[i]);
        }
        return result;
    }

    @Override
    public int getSerializationNumber() {
        return 6;
    }

    private int putString(GrowableByteBuffer buffer, String str) throws UnsupportedEncodingException {
        byte[] b = str.getBytes("UTF-8");
        buffer.putInt(b.length);
        buffer.put(b);
        return 4 + b.length;
    }

    private String getString(ByteBuffer buffer) throws UnsupportedEncodingException {
        byte[] bytes = getBytes(buffer);
        if (bytes == null) {
            return null;
        } else {
            return new String(bytes, "UTF-8");
        }
    }

    private byte[] getBytes(ByteBuffer buffer) throws UnsupportedEncodingException {
        if (buffer.remaining() >= 4) {
            int size = buffer.getInt();
            if (size > buffer.remaining()) {
                throw new MotanServiceException("SimpleSerialization deserialize fail! buffer not enough!need size:" + size);
            }
            if (size == 0) {
                return new byte[]{};
            } else {
                byte[] b = new byte[size];
                buffer.get(b);
                return b;
            }
        } else {
            return null;
        }
    }

}


