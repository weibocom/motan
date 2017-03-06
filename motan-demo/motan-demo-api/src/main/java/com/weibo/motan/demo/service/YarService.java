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
package com.weibo.motan.demo.service;

import java.util.List;
import java.util.Map;

import com.weibo.api.motan.protocol.yar.annotation.YarConfig;

@YarConfig(path = "/openapi/yarserver/test")
public interface YarService {

    String hello(String name);

    void testVoid();

    String testArgVoid();

    String testString(String arg);

    int testInt(int i);

    Integer testInteger(Integer integer);

    boolean testBoolean(boolean b);

    long testLong(long l);

    float testFloat(Float f);

    double testDouble(Double d);

    List<Object> testList(List<Object> list);

    Map<String, Object> testMap(Map<String, Object> map);


}
