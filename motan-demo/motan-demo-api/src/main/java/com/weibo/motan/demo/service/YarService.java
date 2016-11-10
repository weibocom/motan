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

    public String hello(String name);

    public void testVoid();

    public String testArgVoid();

    public String testString(String arg);

    public int testInt(int i);

    public Integer testInteger(Integer integer);

    public boolean testBoolean(boolean b);

    public long testLong(long l);

    public float testFloat(Float f);

    public double testDouble(Double d);

    public List<Object> testList(List<Object> list);

    public Map<String, Object> testMap(Map<String, Object> map);


}
