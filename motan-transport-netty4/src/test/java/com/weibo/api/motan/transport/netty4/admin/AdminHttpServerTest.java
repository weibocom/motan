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

package com.weibo.api.motan.transport.netty4.admin;

import com.weibo.api.motan.admin.DefaultAdminHandler;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.TestHttpClient;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/28.
 */
public class AdminHttpServerTest extends TestCase {

    @SuppressWarnings("unchecked")
    public void testAdminHttpServer() throws Exception {
        final Request[] requests = new Request[1];
        URL url = new URL("http", "127.0.0.1", 0, "tempPath");
        AdminHttpServer server = new AdminHttpServer(url, new DefaultAdminHandler() {
            @Override
            public Response handle(Request request) {
                requests[0] = request;
                if ("/exception".equals(request.getMethodName())) {
                    return MotanFrameworkUtil.buildErrorResponse(request, new MotanServiceException("wrong"));
                }
                return new DefaultResponse("ok", request.getRequestId());
            }
        });
        assertTrue(server.open());
        assertTrue(server.getUrl().getPort() != 0);
        TestHttpClient client = new TestHttpClient("127.0.0.1", server.getUrl().getPort(), 500);
        // test query params
        String result = client.get("/testOk?key1=value1&key2=value2");
        assertEquals("ok", result);
        assertEquals("value1", ((Map<String, String>) requests[0].getArguments()[0]).get("key1"));
        assertEquals("value2", ((Map<String, String>) requests[0].getArguments()[0]).get("key2"));
        // test post params and headers
        Map<String, String> params = new HashMap<>();
        params.put("xxx", "vvv");
        params.put("yyy", "1234");
        Map<String, String> headers = new HashMap<>();
        headers.put("token", "myToken");
        result = client.post("/testOk", params, headers);
        assertEquals("ok", result);
        assertEquals("vvv", ((Map<String, String>) requests[0].getArguments()[0]).get("xxx"));
        assertEquals("1234", ((Map<String, String>) requests[0].getArguments()[0]).get("yyy"));
        assertEquals("myToken", requests[0].getAttachments().get("token")); // check header
        // test exception
        result = client.get("/exception?key1=value1&key2=value2");
        assertEquals("wrong", result);
        server.close();
        client.close();
    }
}