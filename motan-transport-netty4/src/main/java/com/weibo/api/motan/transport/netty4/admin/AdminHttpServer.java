package com.weibo.api.motan.transport.netty4.admin;

import com.weibo.api.motan.admin.AbstractAdminServer;
import com.weibo.api.motan.admin.AdminHandler;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.netty4.http.Netty4HttpServer;
import com.weibo.api.motan.transport.netty4.http.NettyHttpUtil;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public class AdminHttpServer extends AbstractAdminServer {
    private final Netty4HttpServer httpServer;

    public AdminHttpServer(URL url, AdminHandler adminHandler) {
        this.url = url;
        this.adminHandler = adminHandler;
        httpServer = new Netty4HttpServer(url, (channel, httpRequest) -> {
            FullHttpResponse httpResponse;
            try {
                httpResponse = convertHttpResponse(adminHandler.handle(convertRequest(httpRequest)));
            } catch (Exception e) {
                httpResponse = NettyHttpUtil.buildErrorResponse(e.getMessage());
            }
            return httpResponse;
        });
    }

    private Request convertRequest(FullHttpRequest httpRequest) throws IOException {
        DefaultRequest request = new DefaultRequest();
        Map<String, String> params = new ConcurrentHashMap<>();

        // add headers to attachments
        for (Map.Entry<String, String> entry : httpRequest.headers()) {
            request.setAttachment(entry.getKey(), entry.getValue());
        }

        // add method and query params
        request.setMethodName(NettyHttpUtil.addQueryParams(httpRequest.uri(), params));

        // add post params
        NettyHttpUtil.addPostParams(httpRequest, params);
        request.setArguments(new Object[]{params});
        return request;
    }

    private FullHttpResponse convertHttpResponse(Response response) {
        if (response.getException() != null) {
            String errMsg;
            if (response.getException() instanceof MotanAbstractException) {
                errMsg = ((MotanAbstractException) response.getException()).getOriginMessage();
            } else {
                errMsg = response.getException().getMessage();
            }
            return NettyHttpUtil.buildErrorResponse(errMsg);
        }
        return NettyHttpUtil.buildResponse(response.getValue().toString());
    }

    @Override
    public boolean open() {
        return httpServer.open();
    }

    @Override
    public void close() {
        httpServer.close();
    }
}
