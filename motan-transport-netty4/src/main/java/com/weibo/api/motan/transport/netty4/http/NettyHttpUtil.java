package com.weibo.api.motan.transport.netty4.http;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/22.
 */
public class NettyHttpUtil {

    /**
     * @param uri    request uri
     * @param params used to save the parsed parameters
     * @return request path from uri
     */
    public static String addQueryParams(String uri, Map<String, String> params) {
        QueryStringDecoder decoder = new QueryStringDecoder(uri);
        // add query params
        for (Map.Entry<String, List<String>> entry : decoder.parameters().entrySet()) {
            params.put(entry.getKey(), entry.getValue().get(0));
        }
        return decoder.path();
    }

    public static void addPostParams(HttpRequest request, Map<String, String> params) throws IOException {
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        List<InterfaceHttpData> postList = decoder.getBodyHttpDatas();
        for (InterfaceHttpData data : postList) {
            if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                params.put(data.getName(), ((Attribute) data).getString());
            }
        }
    }

    public static FullHttpResponse buildResponse(String msg) {
        return buildDefaultResponse(msg, HttpResponseStatus.OK);
    }

    public static FullHttpResponse buildErrorResponse(String errMsg) {
        return buildDefaultResponse(errMsg, HttpResponseStatus.SERVICE_UNAVAILABLE);
    }

    public static FullHttpResponse buildDefaultResponse(String msg, HttpResponseStatus status) {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(msg
                .getBytes()));
    }
}
