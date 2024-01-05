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
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.transport.netty4.NettyClient;
import com.weibo.api.motan.util.MotanClientUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/28.
 */
public class AdminRpcServerTest extends TestCase {

    public void testAdminRpcServer() throws Exception {
        final Request[] requests = new Request[1];
        URL url = new URL("motan2", "127.0.0.1", 0, "tempPath");
        AdminRpcServer server = new AdminRpcServer(url, new DefaultAdminHandler() {
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
        url.addParameter(URLParamType.asyncInitConnection.getName(), "false");
        NettyClient client = new NettyClient(url);
        client.open();
        Map<String, String> params = new HashMap<>();
        params.put("k", "v");
        Request request = MotanClientUtil.buildRequest("noUse", "/test", new Object[]{params});
        request.setAttachment("token", "myToken");
        Response response = client.request(request);
        assertEquals("ok", ((DeserializableObject) response.getValue()).deserialize(String.class));
        assertEquals(params, requests[0].getArguments()[0]);
        assertEquals("myToken", requests[0].getAttachments().get("token"));
        // exception
        request = MotanClientUtil.buildRequest("noUse", "/exception", new Object[]{params});
        try {
            client.request(request);
            fail();
        } catch (MotanServiceException e) {
            assertEquals("wrong", e.getOriginMessage());
        }
        server.close();
        client.close();
    }

}