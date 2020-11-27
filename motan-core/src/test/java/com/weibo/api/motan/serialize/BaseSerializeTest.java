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

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by zhanglei28 on 2019/4/3.
 */
public class BaseSerializeTest {
    Serialization serialization;

    public BaseSerializeTest(Serialization serialization) {
        this.serialization = serialization;
    }

    public void test() throws Exception {
        testCompatibility(serialization);
        testHessianBug(serialization);
        testHessianDeserializeException(serialization);
        testHessianTimeStampBug(serialization);
        testSerialization(serialization);
        testSerializeMulti(serialization);
    }

    public static void testCompatibility(Serialization serialization) throws Exception {
        SubModel.SerializationObject1 test1 = new SubModel.SerializationObject1();
        SubModel.SerializationObject1 result = serialization.deserialize(serialization.serialize(test1), SubModel.SerializationObject1.class);
        assertEquals(test1.a, result.a);
        assertEquals(test1.b, result.b);
    }

    public static void testSerialization(Serialization serialization) throws IOException {
        Model model = new Model();
        model.add("world1", new SubModel("hello1", 1));
        model.add("world2", new SubModel("hello2", 2));
        model.add("world3", new SubModel("hello3", 2));
        Model result = serialization.deserialize(serialization.serialize(model), Model.class);
        assertNotNull(result);
        assertEquals(model.toString(), result.toString());
        UserAttentions user = new UserAttentions();
        user.setAddTimes(null);
        UserAttentions userResult = serialization.deserialize(serialization.serialize(user), UserAttentions.class);
        assertNull(userResult.getAddTimes());
    }

    public static void testHessianBug(Serialization serialization) throws Exception {
        UserAttentions ua1 = new UserAttentions(1234L, new long[]{}, new long[]{}, 0, 0L);
        UserAttentions ua2 = new UserAttentions(1234L, new long[]{}, new long[]{}, 0);
        byte[] bytes1 = serialization.serialize(ua1);
        byte[] bytes2 = serialization.serialize(ua2);
        assertEquals(bytes1.length, bytes2.length);
        UserAttentions result = serialization.deserialize(bytes1, UserAttentions.class);
        assertEquals(ua2.getUid(), result.getUid());
    }

    public static void testHessianTimeStampBug(Serialization serialization) throws Exception {
        UserAttentions ua = new UserAttentions(1234L, new long[]{}, new long[]{}, 0, 0L);
        Timestamp timestamp = ua.getTimeStamp();
        byte[] b = serialization.serialize(ua);
        UserAttentions result = serialization.deserialize(b, UserAttentions.class);
        assertEquals(result.getTimeStamp().getTime(), timestamp.getTime());
        ua.setTimeStamp(null);
        b = serialization.serialize(ua);
        result = serialization.deserialize(b, UserAttentions.class);
        assertNull(result.getTimeStamp());
    }

    public static void testHessianDeserializeException(Serialization serialization) throws Exception {
        UserAttentions user = new UserAttentions();
        user.setAddTimes(null);
        try {
            serialization.deserialize(serialization.serialize(user), UnModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void testSerializeMulti(Serialization serialization) throws Exception {
        Object[] objects = new Object[]{"123", 456, true, 45.67f};
        byte[] bytes = serialization.serializeMulti(objects);
        Class[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            classes[i] = objects[i].getClass();
        }
        Object[] newObjs = serialization.deserializeMulti(bytes, classes);
        for (int i = 0; i < objects.length; i++) {
            assertEquals(objects[i], newObjs[i]);
        }
    }




    public static class UnModel {

    }


    public static class Model implements Serializable {
        private static final long serialVersionUID = -6654784635984161860L;

        private Map<String, SubModel> map = new HashMap<String, SubModel>();
        private Integer index = 10;

        public void add(String key, SubModel value) {
            map.put(key, value);
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(index).append("\n");
            for (Map.Entry<String, SubModel> entry : map.entrySet()) {
                builder.append(entry.getKey()).append("     ").append(entry.getValue().toString()).append("\n");
            }
            return builder.toString();
        }

        public Map<String, SubModel> getMap() {
            return map;
        }

        public Integer getIndex() {
            return index;
        }
    }


    public static class SubModel implements java.io.Serializable {
        private static final long serialVersionUID = 4797257086799637170L;

        private String name;
        private int age;
        private long[] addTimes = null; // add attention/fan/filter times

        public SubModel(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public SubModel() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String toString() {
            return name + ":" + age;
        }

        public long[] getAddTimes() {
            return addTimes;
        }

        public void setAddTimes(long[] addTimes) {
            this.addTimes = addTimes;
        }

        public static class SerializationObject1 implements Serializable {
            private static final long serialVersionUID = -4917299054507959821L;
            public int a = 10;
            public long b = 20;
        }

        public static class SerializationObject2 implements Serializable {
            private static final long serialVersionUID = 302091199018752193L;

            public int a = 10;
            public int c = 30;
        }
    }

}
