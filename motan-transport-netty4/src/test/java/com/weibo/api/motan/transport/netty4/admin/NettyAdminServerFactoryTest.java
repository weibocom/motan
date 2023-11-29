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

import com.weibo.api.motan.admin.AdminServer;
import com.weibo.api.motan.admin.DefaultAdminHandler;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import junit.framework.TestCase;

/**
 * @author zhanglei28
 * @date 2023/11/28.
 */
public class NettyAdminServerFactoryTest extends TestCase {

    public void testCreateServer() {
        NettyAdminServerFactory factory = new NettyAdminServerFactory();
        URL url = new URL("http", "127.0.0.1", 0, "");
        // adminHandler is null
        try {
            factory.createServer(url, null);
            fail();
        } catch (MotanFrameworkException ignore) {
        }
        // AdminHttpServer
        AdminServer server = factory.createServer(url, new DefaultAdminHandler());
        assertTrue(server instanceof AdminHttpServer);

        // AdminRpcServer
        url.setProtocol("motan2");
        server = factory.createServer(url, new DefaultAdminHandler());
        assertTrue(server instanceof AdminRpcServer);

        // unknown protocol
        url.setProtocol("unknown");
        try {
            factory.createServer(url, new DefaultAdminHandler());
            fail();
        } catch (MotanFrameworkException ignore) {
        }
    }
}