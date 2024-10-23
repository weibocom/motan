/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

package com.weibo.api.motan.admin.handler;

import com.alibaba.fastjson.JSONObject;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanglei28
 */
public class SwitcherHandlerTest extends TestCase {

    public void testProcess() {
        SwitcherHandler handler = new SwitcherHandler();
        String switcherName = "switcher1";

        // set
        Map<String, String> params = new HashMap<>();
        params.put("name", switcherName);
        params.put("value", "true");
        JSONObject result = new JSONObject();
        handler.process("/switcher/set", params, null, result);
        assertTrue(result.getBoolean(switcherName));

        // get
        params = new HashMap<>();
        params.put("name", switcherName);
        result = new JSONObject();
        handler.process("/switcher/get", params, null, result);
        assertTrue(result.getBoolean(switcherName));


        params.put("value", "false");
        result = new JSONObject();
        handler.process("/switcher/set", params, null, result);
        assertFalse(result.getBoolean(switcherName));

        // get
        params = new HashMap<>();
        params.put("name", switcherName);
        result = new JSONObject();
        handler.process("/switcher/get", params, null, result);
        assertFalse(result.getBoolean(switcherName));

        // getAll
        for (int i = 0; i < 6; i++) {
            params = new HashMap<>();
            params.put("name", switcherName + i);
            params.put("value", i % 2 == 0 ? "true" : "false");
            result = new JSONObject();
            handler.process("/switcher/set", params, null, result);
        }
        result = new JSONObject();
        handler.process("/switcher/getAll", params, null, result);
        assertTrue(result.getJSONObject("switchers").size() >= 6);
        JSONObject switchers = result.getJSONObject("switchers");
        for (int i = 0; i < 6; i++) {
            assertEquals(switchers.getBoolean(switcherName + i).booleanValue(), i % 2 == 0);
        }
    }
}