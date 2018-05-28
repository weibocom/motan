/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.grpc.http;

import io.grpc.netty.ProtocolNegotiator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;

/**
 * 
 * @Description HttpProtocolNegotiator process http2.0 request or http1.1 for json request
 * @author zhanglei
 * @date Oct 20, 2016
 *
 */
public class HttpProtocolNegotiator implements ProtocolNegotiator {
    NettyHttpRequestHandler httpHandler;


    public HttpProtocolNegotiator(NettyHttpRequestHandler httpHandler) {
        super();
        this.httpHandler = httpHandler;
    }

    @Override
    public Handler newHandler(Http2ConnectionHandler handler) {
        return new HttpAdapter(handler);
    }

    class HttpAdapter extends ChannelInboundHandlerAdapter implements Handler {

        private Http2ConnectionHandler handler;
        private int maxContentLength = 1024 * 1024 * 64;

        public HttpAdapter(Http2ConnectionHandler handler) {
            super();
            this.handler = handler;
        }

        @Override
        public AsciiString scheme() {
            return AsciiString.of("http");
        }

        public int getMaxContentLength() {
            return maxContentLength;
        }

        public void setMaxContentLength(int maxContentLength) {
            this.maxContentLength = maxContentLength;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // TODO graceful validation
            ByteBuf bf = ((ByteBuf) msg).copy();
            Http2Validator validator = new Http2Validator();
            validator.channelRead(null, bf);
            if (validator.isHttp2()) {
                ctx.pipeline().replace(this, null, handler);
            } else {
                ctx.pipeline().addLast("http-decoder", new HttpRequestDecoder());
                ctx.pipeline().addLast("http-aggregator", new HttpObjectAggregator(maxContentLength));
                ctx.pipeline().addLast("http-encoder", new HttpResponseEncoder());
                ctx.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
                ctx.pipeline().addLast("serverHandler", httpHandler);
            }
            ctx.fireChannelRead(msg);
        }
    }

    class Http2Validator extends HttpRequestDecoder {

        private static final String HTTP2 = "HTTP/2.0";
        private boolean isHttp2 = false;

        @Override
        protected HttpMessage createMessage(String[] initialLine) throws Exception {
            if (HTTP2.equals(initialLine[2])) {
                isHttp2 = true;
            }
            return super.createMessage(initialLine);
        }

        public boolean isHttp2() {
            return isHttp2;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                super.channelRead(ctx, msg);
            } catch (Exception ignore) {}
        }

    }


}
