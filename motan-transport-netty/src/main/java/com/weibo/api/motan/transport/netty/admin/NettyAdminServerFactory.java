package com.weibo.api.motan.transport.netty.admin;

import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.admin.AdminServer;
import com.weibo.api.motan.admin.AdminServerFactory;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.URL;
import org.jboss.netty.handler.codec.http.HttpServerCodec;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
@SpiMeta(name = "netty3")
public class NettyAdminServerFactory implements AdminServerFactory {

    @Override
    public AdminServer createServer(URL url, AdminHandler adminHandler) {
        if (adminHandler == null) {
            throw new MotanFrameworkException("AdminHandler can not be null");
        }
        String protocol = url.getProtocol();
        if ("http".equals(protocol)) {
            return new AdminHttpServer(url, adminHandler);
        } else if ("motan2".equals(protocol)) {
            return new AdminRpcServer(url, adminHandler);
        }
        throw new MotanFrameworkException("unsupported admin server protocol: " + protocol);
    }
}
