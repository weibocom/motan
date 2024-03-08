/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
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

package com.weibo.api.motan.transport;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.ResponseFuture;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.MotanClientUtil;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2023/2/13.
 */
public class DefaultMeshClientTest {

    @Test
    public void testDefaultInstance() {
        DefaultMeshClient meshClient = DefaultMeshClient.DEFAULT_MESH_CLIENT;
        assertNotNull(meshClient);
        assertNull(meshClient.innerReferer);
        assertFalse(meshClient.isAvailable());

        try {
            DefaultMeshClient.getDefault(); // an exception will be thrown since there is no 'motan' EndpointFactory extension
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("get extension fail"));
        }
    }

    @Test
    public void testInit() {
        DefaultMeshClient meshClient = buildMockDefaultMeshClient();
        assertNotNull(meshClient);
        assertNull(meshClient.innerReferer);
        assertFalse(meshClient.isAvailable());

        meshClient.init();
        assertNotNull(meshClient.innerReferer);
        assertTrue(meshClient.isAvailable());
    }

    @Test
    public void call() throws Exception {
        DefaultMeshClient meshClient = buildMockDefaultMeshClient();
        meshClient.init();
        // call with request
        Request request = MotanClientUtil.buildRequest("testService", "echo", new Object[]{"hello"});
        Response response = meshClient.call(request);
        assertNotNull(response);
        assertEquals("hello", response.getValue());
        // call with return type
        String result = meshClient.call(request, String.class);
        assertEquals("hello", result);
        // async call
        ResponseFuture future = meshClient.asyncCall(request, String.class);
        assertNotNull(future);
        assertEquals("hello", future.getValue());
    }

    @Test
    public void testRuntimeInfo() {
        DefaultMeshClient meshClient = buildMockDefaultMeshClient();
        meshClient.init();

        Map<String, Object> info = meshClient.getRuntimeInfo();
        assertEquals(meshClient.getUrl().toFullStr(), info.get(RuntimeInfoKeys.URL_KEY));
        assertTrue(((Map<String, Object>) info.get(RuntimeInfoKeys.REFERERS_KEY)).containsKey(RuntimeInfoKeys.CURRENT_CALL_COUNT_KEY));
    }

    private DefaultMeshClient buildMockDefaultMeshClient() {
        URL url = new URL("motan2", "local", 18002, MeshClient.class.getName(), DefaultMeshClient.getDefaultParams());
        url.addParameter(URLParamType.endpointFactory.getName(), "mockEndpoint");
        return new DefaultMeshClient(url);
    }
}