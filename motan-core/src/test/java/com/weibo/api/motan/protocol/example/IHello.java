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

package com.weibo.api.motan.protocol.example;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-23
 * 
 */
public interface IHello {
    String hello();

    String hello(String name);

    String hello(Model model);

    String hello(int age);

    String hello(byte b);

    String hello(String name, int age, Model model);

    String hello(byte[] bs);

    String hello(int[] is);

    String hello(String[] s);

    String hello(Model[] model);

    String hello(byte[][] bs);

    String hello(int[][] is);

    String hello(String[][] s);

    String hello(Model[][] model);

    Model objResult(String name, int age);

    Model[] objArrayResult(String name, int age);

    void voidResult(String name, int age);

    Model nullResult();

    void helloException();

    UnSerializableClass helloSerializable();

    void helloSerializable(UnSerializableClass unSerializableClass);
}
