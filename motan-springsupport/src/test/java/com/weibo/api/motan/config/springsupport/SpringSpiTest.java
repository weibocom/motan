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

package com.weibo.api.motan.config.springsupport;

import junit.framework.TestCase;

import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;

public class SpringSpiTest extends TestCase {

    @Test
    public void testProtocoConfigTest() {
        new ClassPathXmlApplicationContext("classpath:spi_test_context.xml");

        ISpi spi = ExtensionLoader.getExtensionLoader(ISpi.class).getExtension(SpiImpl.class.getAnnotation(SpiMeta.class).name());
        assertNotNull(spi);
        assertEquals("com.weibo.api.motan.config.springsupport.SpiImpl", spi.getClass().getName());
    }
}
