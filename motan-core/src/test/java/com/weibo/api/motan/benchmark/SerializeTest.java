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

package com.weibo.api.motan.benchmark;

import com.weibo.api.motan.codec.Serialization;
import com.weibo.api.motan.serialize.FastJsonSerialization;
import com.weibo.api.motan.serialize.Hessian2Serialization;
import com.weibo.api.motan.serialize.ProtostuffSerialization;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-9
 * 
 */
public class SerializeTest {
    private static final int loop = 10000;

    public static void main(String[] args) throws Exception {
    	System.out.println(System.currentTimeMillis());
    	System.out.println(System.nanoTime());
        Hessian2Serialization hession = new Hessian2Serialization();
        FastJsonSerialization fastjson = new FastJsonSerialization();
        ProtostuffSerialization protostuff = new ProtostuffSerialization();
        SerializeObject object = new SerializeObject();
        object.setHello("hello world");
        object.setAge(1000000L);
        object.setArr(new int[20]);
        object.setArr2(new int[2][20]);

        for (int i = 0; i < loop; i++) {
            hession.serialize(object);
            fastjson.serialize(object);
            protostuff.serialize(object);
        }

        byte[] fastjsonBytes = fastjson.serialize(object);
        byte[] hessionBytes = hession.serialize(object);
        byte[] protostuffBytes = protostuff.serialize(object);

        for (int i = 0; i < loop; i++) {
            hession.deserialize(hessionBytes, object.getClass());
            fastjson.deserialize(fastjsonBytes, object.getClass());
            protostuff.deserialize(protostuffBytes, object.getClass());
        }

        costtime(hession, object);
        costtime(fastjson, object);
        costtime(protostuff, object);

        System.out.println("~~~~~~~~~~~~~~~~~~~~\n");

        System.out.println("Hessian2Serialization serialize size: " + hessionBytes.length);
        costtime(hession, object);

        System.out.println("FastJsonSerialization serialize size: " + fastjsonBytes.length);
        costtime(fastjson, object);

        System.out.println("ProtostuffSerialization serialize size: " + protostuffBytes.length);
        costtime(protostuff, object);

    }

    public static void costtime(Serialization serialization, Object object) throws Exception {
        long start = System.nanoTime();

        for (int i = 0; i < loop; i++) {
            serialization.serialize(object);
        }

        long cost = System.nanoTime() - start;
        System.out.println(serialization.getClass().getSimpleName() + " serialize costtime: " + cost / loop + "ns");

        byte[] bytes = serialization.serialize(object);

        start = System.nanoTime();

        for (int i = 0; i < loop; i++) {
            serialization.deserialize(bytes, object.getClass());
        }

        cost = System.nanoTime() - start;
        System.out.println(serialization.getClass().getSimpleName() + " deserialize costtime: " + cost / loop + "ns");
    }

}


