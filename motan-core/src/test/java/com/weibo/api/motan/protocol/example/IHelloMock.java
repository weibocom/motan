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

public class IHelloMock implements IHello {

    @Override
    public String hello() {
        // TODO Auto-generated method stub
        return "I'm a mock service";
    }

    @Override
    public String hello(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(Model model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(int age) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(byte b) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(String name, int age, Model model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(byte[] bs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(int[] is) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(String[] s) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(Model[] model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(byte[][] bs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(int[][] is) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(String[][] s) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String hello(Model[][] model) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model objResult(String name, int age) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Model[] objArrayResult(String name, int age) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void voidResult(String name, int age) {
        // TODO Auto-generated method stub

    }

    @Override
    public Model nullResult() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void helloException() {
        // TODO Auto-generated method stub

    }

    @Override
    public UnSerializableClass helloSerializable() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void helloSerializable(UnSerializableClass unSerializableClass) {
        // TODO Auto-generated method stub

    }

}
