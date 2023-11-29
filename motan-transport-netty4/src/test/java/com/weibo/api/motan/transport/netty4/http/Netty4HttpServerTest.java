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

package com.weibo.api.motan.transport.netty4.http;

import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.TestHttpClient;
import junit.framework.TestCase;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhanglei28
 * @date 2023/11/28.
 */
public class Netty4HttpServerTest extends TestCase {

    public void testHttpServer() throws Exception {
        final AtomicBoolean checkHost = new AtomicBoolean();
        URL url = new URL("http", "127.0.0.1", 0, "tempPath");
        Netty4HttpServer server = new Netty4HttpServer(url, (channel, request) -> {
            if ("127.0.0.1".equals(request.headers().get("host"))) {
                checkHost.set(true); // Check if remote host is set
            }
            return NettyHttpUtil.buildResponse("ok");
        });
        server.open();
        assertTrue(server.isAvailable());
        assertTrue(server.isBound());
        assertTrue(server.getUrl().getPort() != 0); // check random port
        TestHttpClient client = new TestHttpClient("127.0.0.1", server.getUrl().getPort(), 500);
        String result = client.get("/test");
        assertEquals("ok", result);
        assertTrue(checkHost.get());
        client.close();
        server.close();
        assertFalse(server.isAvailable());
        assertFalse(server.isBound());
        assertTrue(server.isClosed());
    }
}