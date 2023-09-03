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

import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.SerializerFactory;
import junit.framework.TestCase;
import org.junit.Test;
import sun.print.UnixPrintServiceLookup;
import sun.swing.SwingLazyValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-3
 */
public class Hessian2SerializationTest extends TestCase {
    @Test
    public void testCompatibility() throws Exception {
        new BaseSerializeTest(new Hessian2Serialization()).test();
    }

    @Test
    public void testBlacklist() throws IOException {
        // test built-in blacklist
        SwingLazyValue swingLazyValue = new SwingLazyValue("javax.naming.InitialContext", "doLookup", new Object[]{"malicious ldap server address"});
        checkSerializable(swingLazyValue, HashMap.class, true);

        UnixPrintServiceLookup unsafe = new UnixPrintServiceLookup();
        checkSerializable(unsafe, HashMap.class, true);

        // test custom blacklist
        TestClass testClass = new TestClass();
        Hessian2Serialization.deny(TestClass.class.getName().replace("$", "\\$"));
        checkSerializable(testClass, HashMap.class, false); // can't serialize

        Hessian2Serialization.setSerializerFactory(new SerializerFactory()); // use new SerializerFactory to remove cache and deny list
        checkSerializable(testClass, TestClass.class, false); // can serialize
    }

    public void checkSerializable(Object obj, Class<?> clazz, boolean customSerialize) throws IOException {
        // serialize
        byte[] bytes;
        Hessian2Serialization hessian2Serialization = new Hessian2Serialization();

        if (customSerialize){
            bytes = customSerialize(obj);
        }else {
            bytes = hessian2Serialization.serialize(obj);
        }

        // deserialize
        Object result = hessian2Serialization.deserialize(bytes, Object.class);
        assertNotNull(result);
        assertEquals(result.getClass(), clazz);
    }

    // serialize allow non-serializable
    private byte[] customSerialize(Object data) throws IOException {
        SerializerFactory serializerFactory = new SerializerFactory();
        serializerFactory.setAllowNonSerializable(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output out = new Hessian2Output(bos);
        out.setSerializerFactory(serializerFactory);
        out.writeObject(data);
        out.flush();
        return bos.toByteArray();
    }

    static class TestClass implements Serializable {
        private int field1;
    }
}
