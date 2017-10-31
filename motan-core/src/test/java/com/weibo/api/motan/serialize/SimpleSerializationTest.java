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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

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

        byte[] bytes = new byte[]{2,34,12,24};
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


}