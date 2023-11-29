package com.weibo.api.motan.transport.netty4.admin;

import com.weibo.api.motan.admin.AbstractAdminServer;
import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.transport.netty4.NettyServer;

import java.io.IOException;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public class AdminRpcServer extends AbstractAdminServer {
    private final NettyServer server;
    private static final Class<?>[] paramClass = new Class[]{Map.class};

    public AdminRpcServer(URL url, AdminHandler adminHandler) {
        url.addParameter(URLParamType.codec.getName(), "motan-compatible");
        this.url = url;
        this.adminHandler = adminHandler;
        server = new NettyServer(url, (channel, message) -> {
            if (channel == null || message == null) {
                throw new MotanFrameworkException("AdminRpcServer handler(channel, message) params is null");
            }

            if (!(message instanceof Request)) {
                throw new MotanFrameworkException("AdminRpcServer message type not support: " + message.getClass());
            }

            Request request = (Request) message;
            // process parameter, the parameter type is unified as Map<String, String>
            processLazyDeserialize(request);
            Response response = adminHandler.handle(request);
            response.setSerializeNumber(request.getSerializeNumber());
            response.setRpcProtocolVersion(request.getRpcProtocolVersion());
            return response;
        });
    }

    private void processLazyDeserialize(Request request) {
        if (request.getArguments() != null && request.getArguments().length == 1
                && request.getArguments()[0] instanceof DeserializableObject
                && request instanceof DefaultRequest) {
            try {
                Object[] args = ((DeserializableObject) request.getArguments()[0]).deserializeMulti(paramClass);
                ((DefaultRequest) request).setArguments(args);
            } catch (IOException e) {
                throw new MotanFrameworkException("deserialize parameters fail: " + request + ", error:" + e.getMessage());
            }
        }
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
