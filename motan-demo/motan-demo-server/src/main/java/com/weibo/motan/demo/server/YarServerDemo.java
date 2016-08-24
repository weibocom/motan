/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.motan.demo.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.motan.demo.service.YarService;

public class YarServerDemo implements YarService {

    public String hello(String name) {
        System.out.println(name + " invoked rpc service");
        return "hello " + name;
    }

    // local
    public static void main(String[] args) throws InterruptedException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:motan_demo_server_yar.xml");
        System.out.println("yar server start...");
        Thread.sleep(Long.MAX_VALUE);

    }

    public void testVoid() {
        System.out.println("in void");
    }

    public String testArgVoid() {
        System.out.println("in arg void");
        return "in arg void";
    }

    public String testString(String arg) {
        System.out.println("in String");
        return arg;

    }

    public int testInt(int i) {
        System.out.println("in int");
        return i;
    }

    public Integer testInteger(Integer integer) {
        System.out.println("in Integer");
        return integer;
    }

    public boolean testBoolean(boolean b) {
        System.out.println("in boolean");
        return b;
    }

    public long testLong(long l) {
        System.out.println("in long");
        return l;
    }

    public float testFloat(Float f) {
        return f;
    }

    public double testDouble(Double d) {
        return d;
    }

    public List<Object> testList(List<Object> list) {
        System.out.println("in testlist");
        List<Object> retlist = new ArrayList<Object>(list);
        Collections.reverse(retlist);
        return retlist;
    }

    public Map<String, Object> testMap(Map<String, Object> map) {
        System.out.println("in testmap");
        Map<String, Object> retmap = new HashMap<String, Object>(map);
        retmap.put("size", map.size());
        return retmap;
    }

}
