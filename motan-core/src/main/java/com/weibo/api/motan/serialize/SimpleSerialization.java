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
 * olny support Null, String, Map<String,String>
 */
@SpiMeta(name = "simple")
public class SimpleSerialization implements Serialization {

    @Override
    public byte[] serialize(Object obj) throws IOException {
        GrowableByteBuffer buffer = new GrowableByteBuffer(4096);
        if (obj != null) {
            if (obj instanceof String) {
                buffer.put((byte) 1);
                byte[] b = ((String) obj).getBytes("UTF-8");
                buffer.putInt(b.length);
                buffer.put(b);
            } else if (obj instanceof Map) {
                buffer.put((byte) 2);
                int size = 0;
                buffer.position(5);
                for (Entry<Object, Object> entry : ((Map<Object, Object>) obj).entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null
                            && (entry.getKey() instanceof String) && (entry.getValue() instanceof String)) {
                        size += putString(buffer, (String) entry.getKey());
                        size += putString(buffer, (String) entry.getValue());
                    }
                }
                buffer.position(1);
                buffer.putInt(size);
                buffer.position(5 + size);
            } else {
                throw new MotanServiceException("SimpleSerialization not support type:" + obj.getClass());
            }
        } else {
            buffer.put((byte) 0);
        }
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte type = buffer.get();
        switch (type) {
            case 0:
                return null;
            case 1:
                if (clz == String.class) {
                    String str = getString(buffer);
                    return (T) str;
                } else {
                    throw new MotanServiceException("SimpleSerialization not support type:" + clz);
                }
            case 2:
                if (clz == Map.class) {
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
                    return (T) map;
                } else {
                    throw new MotanServiceException("SimpleSerialization not support type:" + clz);
                }
        }
        return null;
    }

    @Override
    public byte[] serializeMulti(Object[] data) throws IOException {
        if (data.length == 1) {
            return serialize(data[0]);
        }
        //TODO mulit param support
        throw new MotanServiceException("SimpleSerialization not support serialize multi Object");
    }

    @Override
    public Object[] deserializeMulti(byte[] data, Class<?>[] classes) throws IOException {
        if (classes.length == 1) {
            return new Object[]{deserialize(data, classes[0])};
        } else {
            StringBuilder sb = new StringBuilder(128);
            sb.append("[");
            for (Class c : classes) {
                sb.append(c.getName()).append(",");
            }
            if (sb.length() > 1) {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("]");
            throw new MotanServiceException("SimpleSerialization not support deserialize multi Object of " + classes);
        }

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
        if (buffer.remaining() >= 4) {
            int size = buffer.getInt();
            if (size > buffer.remaining()) {
                throw new MotanServiceException("SimpleSerialization deserialize fail! buffer not enough!need size:" + size);
            }
            if (size == 0) {
                return "";
            } else {
                byte[] b = new byte[size];
                buffer.get(b);
                return new String(b, "UTF-8");
            }
        } else {
            return null;
        }
    }

}


