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

package com.weibo.api.motan.config;

import java.lang.reflect.Field;

import junit.framework.TestCase;

/**
 * 
 * 对配置class来说，所有的要进入url的properties都需要是Object，而不能是原始类型。 因为这样可以通过判断property是否为null，来避免使用原始类型的默认值。
 * 为了防止误用，此处监测严格一点：检查所有的字段，而非进入url的字段.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-26
 */

public class ConfigClassTest extends TestCase {

    public void testCheckConfig() {
        checkClassAndSuperClass(BasicRefererInterfaceConfig.class);
        checkClassAndSuperClass(BasicServiceInterfaceConfig.class);
        checkClassAndSuperClass(ProtocolConfig.class);
        checkClassAndSuperClass(RefererConfig.class);
        checkClassAndSuperClass(RegistryConfig.class);
        checkClassAndSuperClass(ServiceConfig.class);
        checkClassAndSuperClass(SpiConfig.class);
    }

    private void checkClassAndSuperClass(Class<?> clazz) {
        do {
            if (clazz != Object.class && clazz != null) {
                checkConfigProperties(clazz);
            }
        } while ((clazz = clazz.getSuperclass()) != null);
    }

    private void checkConfigProperties(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getType().isPrimitive() && !"serialVersionUID".equals(field.getName())) {
                fail(String.format("Config:%s.%s should not be a primtive type!", clazz.getSimpleName(), field.getName()));
            }
        }
    }
}
