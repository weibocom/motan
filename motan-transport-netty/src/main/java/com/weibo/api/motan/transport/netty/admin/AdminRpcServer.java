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

import com.weibo.api.motan.admin.AbstractAdminServer;
import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty.NettyServer;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class AdminRpcServer extends AbstractAdminServer {
    private final NettyServer server;

    public AdminRpcServer(URL url, AdminHandler adminHandler) {
        url.addParameter(URLParamType.codec.getName(), "motan-compatible");
        this.url = url;
        this.adminHandler = adminHandler;
        server = new NettyServer(url, new RpcServerHandler(adminHandler));
    }

    @Override
    public boolean open() {
        return server.open();
    }

    @Override
    public void close() {
        server.close();
    }
}
