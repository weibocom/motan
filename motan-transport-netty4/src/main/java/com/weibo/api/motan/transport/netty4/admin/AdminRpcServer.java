package com.weibo.api.motan.transport.netty4.admin;

import com.weibo.api.motan.admin.AbstractAdminServer;
import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.NettyServer;

/**
 * @author zhanglei28
 * @date 2023/11/3.
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
