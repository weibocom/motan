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

package com.weibo.api.motan.transport.netty.admin;

import com.weibo.api.motan.admin.AdminUtil;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import junit.framework.TestCase;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.DynamicChannelBuffer;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class AdminHttpServerTest extends TestCase {
    URL url = new URL("http", "127.0.0.1", 0, "tempPath");

    public void testAdminHttpServer() {
        AdminHttpServer server = new AdminHttpServer(url, AdminUtil.getDefaultAdminHandler());
        assertTrue(server.open());
        assertTrue(url.getPort() != 0); // random port
        server.close();
    }

    public void testConvertRequest() {
        AdminHttpServer server = new AdminHttpServer(url, AdminUtil.getDefaultAdminHandler());
        String path = "/testPath";
        HttpRequest httpRequest = new DefaultHttpRequest(HTTP_1_1, HttpMethod.POST, path + "?q1=qv1&q2=qv2");
        httpRequest.setHeader("h1", "hv1");
        httpRequest.setHeader("h2", "hv2");
        byte[] content = "p1=pv1&p2=pv2".getBytes(StandardCharsets.UTF_8);
        ChannelBuffer buffer = new DynamicChannelBuffer(content.length);
        buffer.writeBytes(content);
        httpRequest.setContent(buffer);

        Request request = server.convertRequest(httpRequest);
        // check path
        assertEquals(path, request.getMethodName());
        // check query params
        checkParam(request, "q1", "qv1");
        checkParam(request, "q2", "qv2");
        // check post params
        checkParam(request, "p1", "pv1");
        checkParam(request, "p2", "pv2");
        // check headers
        assertEquals("hv1", request.getAttachments().get("h1"));
        assertEquals("hv2", request.getAttachments().get("h2"));
    }

    @SuppressWarnings("unchecked")
    private void checkParam(Request request, String k, String v) {
        assertEquals(v, ((Map<String, String>) request.getArguments()[0]).get(k));
    }

    public void testConvertResponse() {
        AdminHttpServer server = new AdminHttpServer(url, AdminUtil.getDefaultAdminHandler());
        DefaultResponse response = new DefaultResponse();
        response.setValue("ok");
        HttpResponse httpResponse = server.convertHttpResponse(response);
        // check normal
        assertEquals(OK, httpResponse.getStatus());
        assertEquals("ok", httpResponse.getContent().toString(StandardCharsets.UTF_8));

        // motan exception
        response.setException(new MotanServiceException("motan exception"));
        httpResponse = server.convertHttpResponse(response);
        assertEquals(SERVICE_UNAVAILABLE, httpResponse.getStatus());
        assertEquals("motan exception", httpResponse.getContent().toString(StandardCharsets.UTF_8));

        // other exception
        response.setException(new RuntimeException("other exception"));
        httpResponse = server.convertHttpResponse(response);
        assertEquals(SERVICE_UNAVAILABLE, httpResponse.getStatus());
        assertEquals("other exception", httpResponse.getContent().toString(StandardCharsets.UTF_8));
    }

}