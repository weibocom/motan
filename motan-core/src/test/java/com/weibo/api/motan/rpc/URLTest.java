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

package com.weibo.api.motan.rpc;

import java.lang.reflect.Method;

import junit.framework.TestCase;

/**
 * 
 * URL test
 *
 * @author fishermen
 * @version V1.0 created at: 2013-7-19
 */

public class URLTest extends TestCase {

    public void testCheckGetMethod() {
        Method[] methods = URL.class.getDeclaredMethods();
        for (Method m : methods) {
            // 对于带参数的get方法，必须返回对象，不能返回原始类型，防止某些不经意的修改，引发错误
            if (m.getName().startsWith("get") && m.getParameterTypes().length > 0) {
                if (m.getReturnType().isPrimitive()) {
                    fail(String.format("URL.%s should not return primitive type", m.getName()));
                }
            }
        }
    }
}
