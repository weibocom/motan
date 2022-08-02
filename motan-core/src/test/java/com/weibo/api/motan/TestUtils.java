/*
 *
 *   Copyright 2009-2022 Weibo, Inc.
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

package com.weibo.api.motan;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2022/8/2.
 */
public class TestUtils {
    public static Map<String, String> getModifiableEnvironment() throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class<?> map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        return (Map<String, String>) m.get(unmodifiableEnvironment);
    }
}
