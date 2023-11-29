package com.weibo.api.motan.transport.netty4.http;

import com.weibo.api.motan.transport.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author zhanglei28
 * @date 2023/11/22.
 */
public interface HttpMessageHandler {
    FullHttpResponse handle(Channel channel, FullHttpRequest httpRequest);
}
