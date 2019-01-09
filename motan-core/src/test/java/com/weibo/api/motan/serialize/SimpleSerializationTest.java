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

import com.google.common.collect.Sets;
import com.weibo.api.motan.codec.Serialization;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by zhanglei28 on 2017/8/29.
 */
public class SimpleSerializationTest {

    @Test
    public void serialize() throws Exception {
        String s = "hello";
        SimpleSerialization serialization = new SimpleSerialization();
        byte[] b = serialization.serialize(s);
        assertNotNull(b);
        assertTrue(b.length > 0);

        String result = serialization.deserialize(b, String.class);
        assertEquals(s, result);


        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "ray");
        map.put("code", "xxx");
        b = serialization.serialize(map);
        assertNotNull(b);
        assertTrue(b.length > 0);
        Map m2 = serialization.deserialize(b, Map.class);
        assertEquals(map.size(), m2.size());
        for (Map.Entry entry : map.entrySet()) {
            assertEquals(entry.getValue(), m2.get(entry.getKey()));
        }

        byte[] bytes = new byte[]{2, 34, 12, 24};
        b = serialization.serialize(bytes);
        assertNotNull(b);
        assertTrue(b.length > 0);
        assertTrue(b[0] == 3);
        byte[] nbytes = serialization.deserialize(b, byte[].class);
        assertEquals(bytes.length, nbytes.length);

        for (int i = 0; i < nbytes.length; i++) {
            assertEquals(nbytes[i], bytes[i]);
        }
    }

    @Test
    public void testSerializeMulti() throws Exception {
        SimpleSerialization serialization = new SimpleSerialization();
        Object[] objects = new Object[3];
        objects[0] = "teststring";
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", "ray");
        map.put("code", "xxx");
        objects[1] = map;
        byte[] bytes = new byte[]{2, 34, 12, 24};
        objects[2] = bytes;

        byte[] b = serialization.serializeMulti(objects);
        assertNotNull(b);
        assertTrue(b.length > 0);

        Object[] result = serialization.deserializeMulti(b, new Class[]{String.class, Map.class, byte[].class});
        assertEquals(3, result.length);
        assertTrue(result[0] instanceof String);
        assertEquals(result[0], objects[0]);
        assertTrue(result[1] instanceof Map);
        Map<String, String> map2 = (Map<String, String>) result[1];
        for (Map.Entry entry : map.entrySet()) {
            assertEquals(entry.getValue(), map2.get(entry.getKey()));
        }
        assertTrue(result[2] instanceof byte[]);

        byte[] nbytes = (byte[]) result[2];
        for (int i = 0; i < nbytes.length; i++) {
            assertEquals(nbytes[i], bytes[i]);
        }
    }

    @Test
    public void testBaseType() throws Exception {
        verify(true);
        verify(false);

        verify((byte) 16);
        verify((byte) 0);
        verify((byte) 255);

        verify((short) -16);
        verify((short) 0);
        verify((short) 16);
        verify((short) 127);
        verify((short) 128);
        verify((short) 300);
        verify(Short.MAX_VALUE);
        verify(Short.MIN_VALUE);

        verify(-16);
        verify(0);
        verify(16);
        verify(127);
        verify(128);
        verify(300);
        verify(Integer.MAX_VALUE);
        verify(Integer.MIN_VALUE);

        verify(-16L);
        verify(0L);
        verify(16L);
        verify(127L);
        verify(128L);
        verify(300L);
        verify(Long.MAX_VALUE);
        verify(Long.MIN_VALUE);

        verify(3.141592653f);
        verify(-3.141592653f);
        verify(0f);
        verify(Float.MAX_VALUE);
        verify(Float.MIN_VALUE);

        verify(3.141592653d);
        verify(-3.141592653d);
        verify(0d);
        verify(Double.MAX_VALUE);
        verify(Double.MIN_VALUE);
    }

    @Test
    public void testArray() throws Exception {
        assertTrue(SimpleSerialization.isStringCollection(Arrays.asList("1", "2", "3")));
        assertFalse(SimpleSerialization.isStringCollection(Arrays.asList("1", 1, "3")));
        assertTrue(SimpleSerialization.isStringCollection(Sets.newHashSet("1", "2", "3")));
        assertFalse(SimpleSerialization.isStringCollection(Sets.newHashSet("1", 1, "3")));

        List<String> sList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sList.add("testString" + i);
        }

        String[] sArray = new String[sList.size()];
        sArray = sList.toArray(sArray);
        verify(sList);
        verify(new HashSet<>(sList));
        verify(sArray);
    }

    @Test
    public void testMap() throws Exception {
        Serialization serialization = new SimpleSerialization();
        Map<String, String> v = new HashMap<>();
        v.put("1", "1");
        v.put("2", "2");
        assertTrue(SimpleSerialization.isStringMap(v));
        Map<Object, Object> dv = serialization.deserialize(serialization.serialize(v), Map.class);
        assertEquals(v.size(), dv.size());
        Map<Object, Object> ov = new HashMap<>();
        ov.put("a", 1);
        ov.put("b", true);
        ov.put("c", 1L);
        ov.put("d", 1f);
        ov.put("e", 1d);
        ov.put("f", (short) 1);
        ov.put("g", (byte) 1);
        ov.put("h", "1");
        ov.put("i", new String[]{"1", "2", "3", "4"});
        ov.put("j", Arrays.asList("1", "2", "3", "4"));
        ov.put("k", Sets.newHashSet("1", "2", "3", "4"));
        assertFalse(SimpleSerialization.isStringMap(ov));
        dv = serialization.deserialize(serialization.serialize(ov), Map.class);
        assertEquals(ov.size(), dv.size());
        for (Map.Entry<Object, Object> entry : ov.entrySet()) {
            if (entry.getValue().getClass().isArray()) {
                List<Object> values = Arrays.asList((Object[]) entry.getValue());
                assertEquals(values, dv.get(entry.getKey()));
            } else if (entry.getValue() instanceof Collection) {
                ArrayList excepted = new ArrayList((Collection) entry.getValue());
                ArrayList actual = new ArrayList((Collection) entry.getValue());
                Collections.sort(excepted);
                Collections.sort(actual);
                assertEquals(excepted, actual);
            } else {
                assertEquals(entry.getValue(), dv.get(entry.getKey()));
            }
        }
    }

    private void verify(Object v) throws Exception {
        Serialization serialization = new SimpleSerialization();
        byte[] bytes = serialization.serialize(v);
        Object dv = serialization.deserialize(bytes, v.getClass());
        if (v.getClass().isArray()) {
            assertArrayEquals((Object[]) v, (Object[]) dv);
        } else {
            assertEquals(v, dv);
        }
    }
}
