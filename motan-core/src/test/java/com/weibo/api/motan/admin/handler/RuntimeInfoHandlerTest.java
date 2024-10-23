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
import com.weibo.api.motan.mock.MockServer;
import com.weibo.api.motan.registry.support.DirectRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

import static com.weibo.api.motan.runtime.RuntimeInfoKeys.*;

/**
 * @author zhanglei28
 */
public class RuntimeInfoHandlerTest extends TestCase {

    public void testProcess() {
        RuntimeInfoHandler handler = new RuntimeInfoHandler();
        String command = "/runtime/info";
        Map<String, String> params = new HashMap<>();
        Map<String, String> attachments = new HashMap<>();
        JSONObject result = new JSONObject();

        // add runtime info
        GlobalRuntime.addServer("server1", new MockServer(new URL("motan", "localhost", 8080, "com.weibo.api.motan.mock.MockServer")));
        Map<String, String> registryParams = new HashMap<>();
        registryParams.put("address", "127.0.0.1:8002");
        GlobalRuntime.addRegistry("register1", new DirectRegistry(new URL("direct", "localhost", 8080, "com.weibo.api.motan.registry.support.DirectRegistry", registryParams)));

        // get all runtime info
        handler.process(command, params, attachments, result);
        assertEquals("motan-java", result.get(INSTANCE_TYPE_KEY));
        assertTrue(result.containsKey(GLOBAL_CONFIG_KEY));
        assertTrue(result.containsKey(SERVERS_KEY));
        assertTrue(result.getJSONObject(SERVERS_KEY).containsKey("server1"));
        assertTrue(result.containsKey(REGISTRIES_KEY));
        assertTrue(result.getJSONObject(REGISTRIES_KEY).containsKey("register1"));

        // get runtime info by single key
        String keys = SERVERS_KEY;
        params.put("keys", keys);
        result = new JSONObject();
        handler.process(command, params, attachments, result);
        assertEquals("motan-java", result.get(INSTANCE_TYPE_KEY));
        assertTrue(result.containsKey(SERVERS_KEY));
        assertTrue(result.getJSONObject(SERVERS_KEY).containsKey("server1"));
        assertFalse(result.containsKey(REGISTRIES_KEY));

        // get runtime info by multi keys
        keys = SERVERS_KEY + ",  " + REGISTRIES_KEY;
        params.put("keys", keys);
        result = new JSONObject();
        handler.process(command, params, attachments, result);
        assertEquals("motan-java", result.get(INSTANCE_TYPE_KEY));
        assertTrue(result.containsKey(SERVERS_KEY));
        assertTrue(result.getJSONObject(SERVERS_KEY).containsKey("server1"));
        assertTrue(result.containsKey(REGISTRIES_KEY));
        assertTrue(result.getJSONObject(REGISTRIES_KEY).containsKey("register1"));
    }
}