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

package com.weibo.api.motan.util;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-26
 * 
 */
public class ReflectUtilTest {
    
    
    @Test
    public void testReflect(){
        Method method;
        try {
            method = ReflectTest.class.getMethod("getByte", byte.class);
            assertEquals("getByte(byte)", ReflectUtil.getMethodDesc(method));
            method = ReflectTest.class.getMethod("get");
            assertEquals("get(void)", ReflectUtil.getMethodDesc(method));
            method = ReflectTest.class.getMethod("getList", List.class);
            assertEquals("getList(java.util.List)", ReflectUtil.getMethodDesc(method));
            method = ReflectTest.class.getMethod("getMap", Map.class);
            assertEquals("getMap(java.util.Map)", ReflectUtil.getMethodDesc(method));
            method = ReflectTest.class.getMethod("getStringArray", String[].class);
            assertEquals("getStringArray(java.lang.String[])", ReflectUtil.getMethodDesc(method));
            method = ReflectTest.class.getMethod("getIntArray", int[].class);
            assertEquals("getIntArray(int[])", ReflectUtil.getMethodDesc(method));
        } catch (Exception e) {
            assertTrue(false);
        } 
        
    }

}


class ReflectTest {
    public void get(){}
    public int getInt(int param) {return param;}
    public int[] getIntArray(int[] param) {return param;}
    public byte getByte(byte param) {return param;}
    public byte[] getByteArray(byte[] param) {return param;}
    public String getString(String param) {return param;}
    public String[] getStringArray(String[] param) {return param;}
    public List<Object> getList(List<Object> param) {return param;}
    public Map<Object, Object> getMap(Map<Object, Object> param) {return param;}
}
