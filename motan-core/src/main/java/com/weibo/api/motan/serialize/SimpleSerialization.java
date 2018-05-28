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
import java.nio.ByteBuffer;
import java.util.*;

import static com.weibo.api.motan.serialize.SimpleSerialization.SimpleType.*;

/**
 * Created by zhanglei28 on 2017/6/8. <br/>
 *
 * Supported types (list,set treated as array):
 * <pre>
 *    null
 *    String
 *    Map&lt;String, String&gt;
 *    byte[]
 *    String[]
 *    boolean
 *    byte
 *    short
 *    int
 *    long
 *    float
 *    double
 *    Map&lt;Object, Object&gt;
 *    Object[]
 * </pre>
 * @author luominggang
 */
@SpiMeta(name = "simple")
public class SimpleSerialization implements Serialization {

    public static final class SimpleType {
        public static final byte NULL = 0;
        public static final byte STRING = 1;
        public static final byte STRING_MAP = 2;
        public static final byte BYTE_ARRAY = 3;
        public static final byte STRING_ARRAY = 4;
        public static final byte BOOL = 5;
        public static final byte BYTE = 6;
        public static final byte INT16 = 7;
        public static final byte INT32 = 8;
        public static final byte INT64 = 9;
        public static final byte FLOAT32 = 10;
        public static final byte FLOAT64 = 11;

        public static final byte MAP = 20;
        public static final byte ARRAY = 21;
    }

    private static final int DEFAULT_MAP_SIZE = 16;
    private static final int DEFAULT_ARRAY_SIZE = 16;

    public static boolean isStringCollection(Collection<?> obj) {
        if (obj.isEmpty()) {
            return false;
        }
        for (Object v : obj) {
            if (v == null) {
                continue;
            }
            if (!(v instanceof String)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStringMap(Map<?, ?> obj) {
        if (obj.isEmpty()) {
            return false;
        }
        for (Map.Entry<?, ?> entry : obj.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) {
                return false;
            }
        }
        return true;
    }

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
        if (obj == null) {
            buffer.put(NULL);
            return;
        }

        Class<?> clz = obj.getClass();
        if (clz == String.class) {
            writeString(buffer, (String) obj);
            return;
        }

        if (clz == Byte.class || clz == byte.class) {
            writeByte(buffer, (Byte) obj);
            return;
        }

        if (clz == Boolean.class || clz == boolean.class) {
            writeBool(buffer, (Boolean) obj);
            return;
        }

        if (clz == Short.class || clz == short.class) {
            writeInt16(buffer, (Short) obj);
            return;
        }

        if (clz == Integer.class || clz == int.class) {
            writeInt32(buffer, (Integer) obj);
            return;
        }

        if (clz == Long.class || clz == long.class) {
            writeInt64(buffer, (Long) obj);
            return;
        }

        if (clz == Float.class || clz == float.class) {
            writeFloat32(buffer, (Float) obj);
            return;
        }

        if (clz == Double.class || clz == double.class) {
            writeFloat64(buffer, (Double) obj);
            return;
        }

        if (obj instanceof Map) {
            if (isStringMap((Map) obj)) {
                writeStringMap(buffer, (Map<String, String>) obj);
            } else {
                writeMap(buffer, (Map) obj);
            }
            return;
        }

        if (clz.isArray()) {
            if (clz.getComponentType() == String.class) {
                writeStringArray(buffer, (String[]) obj);
            } else if (clz.getComponentType() == byte.class) {
                writeBytes(buffer, (byte[]) obj);
            } else {
                writeArray(buffer, (Object[]) obj);
            }
            return;
        }

        if (obj instanceof List || obj instanceof Set) {
            if (isStringCollection((Collection) obj)) {
                writeStringArray(buffer, (Collection<String>) obj);
            } else {
                writeArray(buffer, (Collection) obj);
            }
            return;
        }

        throw new MotanServiceException("SimpleSerialization unsupported type: " + clz);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clz) throws IOException {
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.wrap(bytes));
        return deserialize(buffer, clz);
    }

    private <T> T deserialize(GrowableByteBuffer buffer, Class<T> clz) throws IOException {
        byte type = buffer.get();
        switch (type) {
            default:
                break;
            case NULL:
                return null;
            case STRING:
                if (clz == String.class || clz == Object.class) {
                    return (T) readString(buffer);
                }
                break;
            case STRING_MAP:
                if (clz.isAssignableFrom(HashMap.class)) {
                    // contain Object, Map
                    return (T) readStringMap(buffer);
                }
                break;
            case BYTE_ARRAY:
                if (clz == byte[].class || clz == Object.class) {
                    return (T) readBytes(buffer);
                }
                break;
            case STRING_ARRAY:
                if ((clz.isArray() && clz.getComponentType() == String.class)) {
                    return (T) readStringArray(buffer);
                } else if (clz.isAssignableFrom(ArrayList.class)) {
                    // contain Object, Collection, List
                    return (T) readStringList(buffer);
                } else if (clz.isAssignableFrom(HashSet.class)) {
                    return (T) readStringSet(buffer);
                }
                break;
            case BOOL:
                if (clz == boolean.class || clz == Boolean.class || clz == Object.class) {
                    return (T) readBool(buffer);
                }
                break;
            case BYTE:
                if (clz == byte.class || clz == Byte.class || clz == Object.class) {
                    return (T) readByte(buffer);
                }
            case INT16:
                if (clz == short.class || clz == Short.class || clz == Object.class) {
                    return (T) readInt16(buffer);
                }
                break;
            case INT32:
                if (clz == int.class || clz == Integer.class || clz == Object.class) {
                    return (T) readInt32(buffer);
                }
                break;
            case INT64:
                if (clz == long.class || clz == Long.class || clz == Object.class) {
                    return (T) readInt64(buffer);
                }
                break;
            case FLOAT32:
                if (clz == float.class || clz == Float.class || clz == Object.class) {
                    return (T) readFloat32(buffer);
                }
                break;
            case FLOAT64:
                if (clz == double.class || clz == Double.class || clz == Object.class) {
                    return (T) readFloat64(buffer);
                }
                break;
            case MAP:
                if (clz.isAssignableFrom(HashMap.class)) {
                    // contain Object, Map
                    return (T) readMap(buffer);
                }
                break;
            case ARRAY:
                if (clz.isArray()) {
                    return (T) readArray(buffer);
                } else if (clz.isAssignableFrom(ArrayList.class)) {
                    // contain Object, Collection, List
                    return (T) readList(buffer);
                } else if (clz.isAssignableFrom(HashSet.class)) {
                    return (T) readSet(buffer);
                }
                break;
        }
        throw new MotanServiceException("SimpleSerialization not support " + type + " with receiver type:" + clz);
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
        GrowableByteBuffer buffer = new GrowableByteBuffer(ByteBuffer.wrap(data));
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

    private void putString(GrowableByteBuffer buffer, String str) throws IOException {
        byte[] b = str.getBytes("UTF-8");
        buffer.putInt(b.length);
        buffer.put(b);
    }

    private void writeString(GrowableByteBuffer buffer, String str) throws IOException {
        buffer.put(STRING);
        putString(buffer, str);
    }

    private void writeStringMap(GrowableByteBuffer buffer, Map<String, String> value) throws IOException {
        buffer.put(STRING_MAP);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (Map.Entry<String, String> entry : value.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            putString(buffer, entry.getKey());
            putString(buffer, entry.getValue());
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private void writeBytes(GrowableByteBuffer buffer, byte[] value) {
        buffer.put(BYTE_ARRAY);
        buffer.putInt(value.length);
        buffer.put(value);
    }

    private void writeStringArray(GrowableByteBuffer buffer, String[] value) throws IOException {
        buffer.put(STRING_ARRAY);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) {
                continue;
            }
            putString(buffer, value[i]);
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private void writeStringArray(GrowableByteBuffer buffer, Collection<String> value) throws IOException {
        buffer.put(STRING_ARRAY);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (String s : value) {
            // TODO: if 's' is null, the size of array may be different with origin
            if (s == null) {
                continue;
            }
            putString(buffer, s);
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private void writeBool(GrowableByteBuffer buffer, boolean value) {
        buffer.put(BOOL);
        if (value) {
            buffer.put((byte) 1);
        } else {
            buffer.put((byte) 0);
        }
    }

    private void writeByte(GrowableByteBuffer buffer, byte value) {
        buffer.put(BYTE);
        buffer.put(value);
    }

    private void writeInt16(GrowableByteBuffer buffer, short value) {
        buffer.put(INT16);
        buffer.putShort(value);
    }

    private void writeInt32(GrowableByteBuffer buffer, int value) {
        buffer.put(INT32);
        buffer.putZigzag32(value);
    }

    private void writeInt64(GrowableByteBuffer buffer, long value) {
        buffer.put(INT64);
        buffer.putZigzag64(value);
    }

    private void writeFloat32(GrowableByteBuffer buffer, float value) {
        buffer.put(FLOAT32);
        buffer.putFloat(value);
    }

    private void writeFloat64(GrowableByteBuffer buffer, double value) {
        buffer.put(FLOAT64);
        buffer.putDouble(value);
    }

    private void writeArray(GrowableByteBuffer buffer, Object[] value) throws IOException {
        buffer.put(ARRAY);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (int i = 0; i < value.length; i++) {
            serialize(value[i], buffer);
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private void writeArray(GrowableByteBuffer buffer, Collection<?> value) throws IOException {
        buffer.put(ARRAY);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (Object v : value) {
            serialize(v, buffer);
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private void writeMap(GrowableByteBuffer buffer, Map<?, ?> value) throws IOException {
        buffer.put(MAP);
        int pos = buffer.position();
        buffer.position(pos + 4);
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            serialize(entry.getKey(), buffer);
            serialize(entry.getValue(), buffer);
        }
        int npos = buffer.position();
        buffer.position(pos);
        buffer.putInt(npos - pos - 4);
        buffer.position(npos);
    }

    private int getAndCheckSize(GrowableByteBuffer buffer) {
        int size = buffer.getInt();
        if (size > buffer.remaining()) {
            throw new MotanServiceException("SimpleSerialization deserialize fail! buffer not enough!need size:" + size);
        }
        return size;
    }

    private String readString(GrowableByteBuffer buffer) throws IOException {
        return new String(readBytes(buffer), "UTF-8");
    }

    private Map<String, String> readStringMap(GrowableByteBuffer buffer) throws IOException {
        Map<String, String> map = new HashMap<>(DEFAULT_MAP_SIZE);
        int size = getAndCheckSize(buffer);
        int startPos = buffer.position();
        int endPos = startPos + size;
        while (buffer.position() < endPos) {
            map.put(readString(buffer), readString(buffer));
        }
        if (buffer.position() != endPos) {
            throw new MotanServiceException("SimpleSerialization deserialize wrong map size, except: " + size + " actual: " + (buffer.position() - startPos));
        }
        return map;
    }

    private byte[] readBytes(GrowableByteBuffer buffer) {
        int size = getAndCheckSize(buffer);
        if (size == 0) {
            return new byte[]{};
        } else {
            byte[] b = new byte[size];
            buffer.get(b);
            return b;
        }
    }

    private String[] readStringArray(GrowableByteBuffer buffer) throws IOException {
        List<String> values = readStringList(buffer);
        String[] result = new String[values.size()];
        return values.toArray(result);
    }

    private List<String> readStringList(GrowableByteBuffer buffer) throws IOException {
        List<String> result = new ArrayList<>(DEFAULT_ARRAY_SIZE);
        return readStringCollection(buffer, result);
    }

    private Set<String> readStringSet(GrowableByteBuffer buffer) throws IOException {
        Set<String> result = new HashSet<>(DEFAULT_ARRAY_SIZE);
        return readStringCollection(buffer, result);
    }

    private <T extends Collection> T readStringCollection(GrowableByteBuffer buffer, T collection) throws IOException {
        int size = getAndCheckSize(buffer);
        if (size == 0) {
            return collection;
        }
        int startPos = buffer.position();
        int endPos = startPos + size;
        while (buffer.position() < endPos) {
            collection.add(readString(buffer));
        }
        if (buffer.position() != endPos) {
            throw new MotanServiceException("SimpleSerialization deserialize wrong array size, except: " + size + " actual: " + (buffer.position() - startPos));
        }
        return collection;
    }

    private Boolean readBool(GrowableByteBuffer buffer) {
        return buffer.get() == 1;
    }

    private Byte readByte(GrowableByteBuffer buffer) {
        return buffer.get();
    }

    private Short readInt16(GrowableByteBuffer buffer) {
        return buffer.getShort();
    }

    private Integer readInt32(GrowableByteBuffer buffer) {
        return buffer.getZigZag32();
    }

    private Long readInt64(GrowableByteBuffer buffer) {
        return buffer.getZigZag64();
    }

    private Float readFloat32(GrowableByteBuffer buffer) {
        return buffer.getFloat();
    }

    private Double readFloat64(GrowableByteBuffer buffer) {
        return buffer.getDouble();
    }

    private Map readMap(GrowableByteBuffer buffer) throws IOException {
        Map<Object, Object> map = new HashMap<>(DEFAULT_MAP_SIZE);
        int size = getAndCheckSize(buffer);
        int startPos = buffer.position();
        int endPos = startPos + size;
        while (buffer.position() < endPos) {
            map.put(deserialize(buffer, Object.class), deserialize(buffer, Object.class));
        }
        if (buffer.position() != endPos) {
            throw new MotanServiceException("SimpleSerialization deserialize wrong map size, except: " + size + " actual: " + (buffer.position() - startPos));
        }
        return map;
    }

    private Object[] readArray(GrowableByteBuffer buffer) throws IOException {
        List<Object> values = readList(buffer);
        Object[] result = new Object[values.size()];
        return values.toArray(result);
    }

    private List<Object> readList(GrowableByteBuffer buffer) throws IOException {
        List<Object> result = new ArrayList<>(DEFAULT_ARRAY_SIZE);
        return readCollection(buffer, result);
    }

    private Set<Object> readSet(GrowableByteBuffer buffer) throws IOException {
        Set<Object> result = new HashSet<>(DEFAULT_ARRAY_SIZE);
        return readCollection(buffer, result);
    }

    private <T extends Collection> T readCollection(GrowableByteBuffer buffer, T collection) throws IOException {
        int size = getAndCheckSize(buffer);
        if (size == 0) {
            return collection;
        }
        int startPos = buffer.position();
        int endPos = startPos + size;
        while (buffer.position() < endPos) {
            collection.add(deserialize(buffer, Object.class));
        }
        if (buffer.position() != endPos) {
            throw new MotanServiceException("SimpleSerialization deserialize wrong array size, except: " + size + " actual: " + (buffer.position() - startPos));
        }
        return collection;
    }
}
