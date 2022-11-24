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

package com.weibo.api.motan.util;

import com.weibo.api.motan.config.DefaultGlobalConfig;
import com.weibo.api.motan.config.GlobalConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2022/11/4.
 */
public class MotanGlobalConfigUtilTest {
    public static final String testKey1 = "testGlobalKey";
    public static final String testKey2 = "test_key";
    public static final String testValue1 = "testValue";
    public static final String testValue2 = "test_value";
    public static final String testNewValue1 = "testNewValue";
    public static final String testAppendKey = "xxx";
    public static final String testAppendValue = "ddd";

    @Before
    public void setUp() throws Exception {
        MotanGlobalConfigUtil.setInnerGlobalConfig(new DefaultGlobalConfig());
    }

    @Test
    public void testGetDefaultConfigCopy() {
        Map<String, String> defaultConfig = MotanGlobalConfigUtil.getDefaultConfigCopy();
        assertEquals(2, defaultConfig.size());
        defaultConfig.put(testAppendKey, testAppendValue);
        defaultConfig.remove(testKey1);
        defaultConfig.remove(testKey2);

        Map<String, String> defaultConfig2 = MotanGlobalConfigUtil.getDefaultConfigCopy();
        assertNotSame(defaultConfig, defaultConfig2);
        assertEquals(2, defaultConfig2.size());
        String value = defaultConfig2.get(testKey1);
        assertEquals(testValue1, value);
        value = defaultConfig2.get(testKey2);
        assertEquals(testValue2, value);
    }

    @Test
    public void testGetConfig() {
        // contains default global configs
        String value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testValue1, value);
        value = MotanGlobalConfigUtil.getConfig(testKey2);
        assertEquals(testValue2, value);
        // test put
        MotanGlobalConfigUtil.putConfig(testAppendKey, testAppendValue);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey);
        assertEquals(testAppendValue, value);
        // test remove
        MotanGlobalConfigUtil.remove(testAppendKey);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey);
        assertNull(value);
    }

    @Test
    public void testPutConfigs() {
        Map<String, String> newConfigs = new HashMap<>();
        newConfigs.put(testKey1, testNewValue1); // key already exists
        newConfigs.put(testAppendKey, testAppendValue); // new key

        String value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testValue1, value);

        // not override keys
        MotanGlobalConfigUtil.putConfigs(newConfigs, false);
        value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testValue1, value);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey); // new key
        assertEquals(testAppendValue, value);

        // override keys
        newConfigs = new HashMap<>();
        newConfigs.put(testKey1, testNewValue1);
        MotanGlobalConfigUtil.putConfigs(newConfigs, true);
        value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testNewValue1, value);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey); // other key
        assertEquals(testAppendValue, value);
    }

    @Test
    public void testSetGlobalConfig() {
        // check init values
        String value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testValue1, value);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey);
        assertNull(value);

        // set null
        GlobalConfig old = MotanGlobalConfigUtil.setInnerGlobalConfig(null);
        assertNull(old);
        value = MotanGlobalConfigUtil.getConfig(testKey1);
        assertEquals(testValue1, value);

        // set new GlobalConfig
        GlobalConfig newConfig = new DefaultGlobalConfig();
        newConfig.putConfig(testAppendKey, testAppendValue);
        old = MotanGlobalConfigUtil.setInnerGlobalConfig(newConfig);
        value = old.getConfig(testAppendKey);
        assertNull(value);
        value = MotanGlobalConfigUtil.getConfig(testAppendKey);
        assertEquals(testAppendValue, value);

        old = MotanGlobalConfigUtil.setInnerGlobalConfig(new DefaultGlobalConfig());
        assertSame(old, newConfig); // same object
    }

}