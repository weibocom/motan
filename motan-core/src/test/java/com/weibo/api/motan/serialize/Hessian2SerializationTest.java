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

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import org.junit.Test;

import com.caucho.hessian.io.SerializerFactory;

import junit.framework.TestCase;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-3
 */
public class Hessian2SerializationTest extends TestCase {
    private static final byte[] SWING_LAZY_VALUE_ENCODED_BYTES = new byte[] { 67, 24, 115, 117, 110, 46, 115, 119, 105,
            110, 103, 46, 83, 119, 105, 110, 103, 76, 97, 122, 121, 86, 97, 108, 117, 101, -109, 9, 99, 108, 97, 115,
            115, 78, 97, 109, 101, 10, 109, 101, 116, 104, 111, 100, 78, 97, 109, 101, 4, 97, 114, 103, 115, 96, 27,
            106, 97, 118, 97, 120, 46, 110, 97, 109, 105, 110, 103, 46, 73, 110, 105, 116, 105, 97, 108, 67, 111, 110,
            116, 101, 120, 116, 8, 100, 111, 76, 111, 111, 107, 117, 112, 113, 7, 91, 111, 98, 106, 101, 99, 116, 29,
            109, 97, 108, 105, 99, 105, 111, 117, 115, 32, 108, 100, 97, 112, 32, 115, 101, 114, 118, 101, 114, 32, 97,
            100, 100, 114, 101, 115, 115 };
    private static final byte[] UNIX_PRINT_SERVICE_LOOKUP_ENCODED_BYTES = new byte[] { 67, 48, 32, 115, 117, 110, 46,
            112, 114, 105, 110, 116, 46, 85, 110, 105, 120, 80, 114, 105, 110, 116, 83, 101, 114, 118, 105, 99, 101, 76,
            111, 111, 107, 117, 112, -104, 14, 100, 101, 102, 97, 117, 108, 116, 80, 114, 105, 110, 116, 101, 114, 19,
            100, 101, 102, 97, 117, 108, 116, 80, 114, 105, 110, 116, 83, 101, 114, 118, 105, 99, 101, 13, 112, 114,
            105, 110, 116, 83, 101, 114, 118, 105, 99, 101, 115, 15, 108, 111, 111, 107, 117, 112, 76, 105, 115, 116,
            101, 110, 101, 114, 115, 12, 108, 112, 78, 97, 109, 101, 67, 111, 109, 65, 105, 120, 11, 108, 112, 99, 70,
            105, 114, 115, 116, 67, 111, 109, 9, 108, 112, 99, 65, 108, 108, 67, 111, 109, 10, 108, 112, 99, 78, 97,
            109, 101, 67, 111, 109, 96, 78, 78, 78, 78, 116, 7, 91, 115, 116, 114, 105, 110, 103, 15, 47, 117, 115, 114,
            47, 98, 105, 110, 47, 108, 115, 97, 108, 108, 113, 48, 60, 47, 117, 115, 114, 47, 98, 105, 110, 47, 108,
            112, 115, 116, 97, 116, 32, 45, 87, 32, 45, 112, 124, 47, 117, 115, 114, 47, 98, 105, 110, 47, 101, 120,
            112, 97, 110, 100, 124, 47, 117, 115, 114, 47, 98, 105, 110, 47, 99, 117, 116, 32, 45, 102, 49, 32, 45, 100,
            39, 32, 39, 48, 60, 47, 117, 115, 114, 47, 98, 105, 110, 47, 108, 112, 115, 116, 97, 116, 32, 45, 87, 32,
            45, 100, 124, 47, 117, 115, 114, 47, 98, 105, 110, 47, 101, 120, 112, 97, 110, 100, 124, 47, 117, 115, 114,
            47, 98, 105, 110, 47, 99, 117, 116, 32, 45, 102, 49, 32, 45, 100, 39, 32, 39, 21, 47, 117, 115, 114, 47, 98,
            105, 110, 47, 108, 112, 115, 116, 97, 116, 32, 45, 87, 32, 45, 118, 114, -112, 48, 52, 47, 117, 115, 114,
            47, 115, 98, 105, 110, 47, 108, 112, 99, 32, 115, 116, 97, 116, 117, 115, 32, 124, 32, 103, 114, 101, 112,
            32, 58, 32, 124, 32, 115, 101, 100, 32, 45, 110, 101, 32, 39, 49, 44, 49, 32, 115, 47, 58, 47, 47, 112, 39,
            48, 75, 47, 117, 115, 114, 47, 115, 98, 105, 110, 47, 108, 112, 99, 32, 115, 116, 97, 116, 117, 115, 32,
            124, 32, 103, 114, 101, 112, 32, 45, 69, 32, 39, 94, 91, 32, 48, 45, 57, 97, 45, 122, 65, 45, 90, 95, 45,
            93, 42, 64, 39, 32, 124, 32, 97, 119, 107, 32, 45, 70, 39, 64, 39, 32, 39, 123, 112, 114, 105, 110, 116, 32,
            36, 49, 125, 39, 114, -112, 48, 50, 47, 117, 115, 114, 47, 115, 98, 105, 110, 47, 108, 112, 99, 32, 115,
            116, 97, 116, 117, 115, 32, 97, 108, 108, 32, 124, 32, 103, 114, 101, 112, 32, 58, 32, 124, 32, 115, 101,
            100, 32, 45, 101, 32, 39, 115, 47, 58, 47, 47, 39, 48, 86, 47, 117, 115, 114, 47, 115, 98, 105, 110, 47,
            108, 112, 99, 32, 115, 116, 97, 116, 117, 115, 32, 97, 108, 108, 32, 124, 32, 103, 114, 101, 112, 32, 45,
            69, 32, 39, 94, 91, 32, 48, 45, 57, 97, 45, 122, 65, 45, 90, 95, 45, 93, 42, 64, 39, 32, 124, 32, 97, 119,
            107, 32, 45, 70, 39, 64, 39, 32, 39, 123, 112, 114, 105, 110, 116, 32, 36, 49, 125, 39, 32, 124, 32, 115,
            111, 114, 116, 114, -112, 27, 124, 32, 103, 114, 101, 112, 32, 58, 32, 124, 32, 115, 101, 100, 32, 45, 110,
            101, 32, 39, 115, 47, 58, 47, 47, 112, 39, 48, 54, 124, 32, 103, 114, 101, 112, 32, 45, 69, 32, 39, 94, 91,
            32, 48, 45, 57, 97, 45, 122, 65, 45, 90, 95, 45, 93, 42, 64, 39, 32, 124, 32, 97, 119, 107, 32, 45, 70, 39,
            64, 39, 32, 39, 123, 112, 114, 105, 110, 116, 32, 36, 49, 125, 39 };

    @Test
    public void testCompatibility() throws Exception {
        new BaseSerializeTest(new Hessian2Serialization()).test();
    }

    @Test
    public void testBlacklist() throws IOException {
        // test built-in blacklist
        checkSerializable(SWING_LAZY_VALUE_ENCODED_BYTES, HashMap.class, true);
        checkSerializable(UNIX_PRINT_SERVICE_LOOKUP_ENCODED_BYTES, HashMap.class, true);

        // test custom blacklist
        TestClass testClass = new TestClass();
        Hessian2Serialization.deny(TestClass.class.getName().replace("$", "\\$"));
        checkSerializable(testClass, HashMap.class, false); // can't serialize

        Hessian2Serialization.setSerializerFactory(new SerializerFactory()); // use new SerializerFactory to remove
                                                                             // cache and deny list
        checkSerializable(testClass, TestClass.class, false); // can serialize
    }

    public void checkSerializable(Object obj, Class<?> clazz, boolean customSerialize) throws IOException {
        // serialize
        byte[] bytes;
        Hessian2Serialization hessian2Serialization = new Hessian2Serialization();

        if (customSerialize) {
            bytes = (byte[]) obj;
        } else {
            bytes = hessian2Serialization.serialize(obj);
        }

        // deserialize
        Object result = hessian2Serialization.deserialize(bytes, Object.class);
        assertNotNull(result);
        assertEquals(result.getClass(), clazz);
    }

    static class TestClass implements Serializable {
        @SuppressWarnings("unused")
        private int field1;
    }
}
