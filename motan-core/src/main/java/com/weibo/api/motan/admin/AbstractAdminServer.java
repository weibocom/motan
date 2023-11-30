package com.weibo.api.motan.admin;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;

import java.io.IOException;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public abstract class AbstractAdminServer implements AdminServer {
    protected URL url;
    protected AdminHandler adminHandler;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public AdminHandler getAdminHandler() {
        return adminHandler;
    }

    // adapt to Motan RPC server
    public static class RpcServerHandler implements MessageHandler {
        private final Class<?>[] paramClass = new Class[]{Map.class};
        private final AdminHandler adminHandler;

        public RpcServerHandler(AdminHandler adminHandler) {
            this.adminHandler = adminHandler;
        }

        @Override
        public Object handle(Channel channel, Object message) {
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
        }

        protected void processLazyDeserialize(Request request) {
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
    }
}
