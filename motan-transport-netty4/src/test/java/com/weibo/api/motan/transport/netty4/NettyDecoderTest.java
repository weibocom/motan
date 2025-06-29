package com.weibo.api.motan.transport.netty4;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.protocol.v2motan.MotanV2Codec;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * @author single-wolf root@mail.zhongm.in
 */
public class NettyDecoderTest {

    private NettyServer nettyServer;
    private MessageHandler messageHandler;
    private URL url;
    private String interfaceName = "com.weibo.api.motan.protocol.example.IHello";

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("requestTimeout", "500");

        url = new URL("netty", "localhost", 18080, interfaceName, parameters);
        messageHandler = new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());

                return response;
            }
        };
        nettyServer = new NettyServer(url, messageHandler);
        nettyServer.open();
    }

    @After
    public void tearDown() {
        nettyServer.close();
    }

    @Test
    public void onlyOneDecodeInvoked() {
        NettyNewCntDecoder nettyDecoder = new NettyNewCntDecoder(new MotanV2Codec(), nettyServer, 24);
        NettyChannelHandler handler = new NettyChannelHandler(nettyServer, messageHandler, (ThreadPoolExecutor) Executors.newFixedThreadPool(4));

        EmbeddedChannel channel = new EmbeddedChannel(nettyDecoder, handler);

        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{'a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd',
                'e', 'a', 'b', 'c', 'd', 'e',});
        channel.writeInbound(buf.copy());
        buf.release();
        assertEquals(true, 1 == nettyDecoder.getDecodeInvokeCnt());
    }

    class NettyNewCntDecoder extends NettyDecoder {

        private int decodeInvokeCnt = 0;

        public NettyNewCntDecoder(Codec codec, Channel channel, int maxContentLength) {
            super(codec, channel, maxContentLength);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            decodeInvokeCnt++;
            super.decode(ctx, in, out);
        }

        public int getDecodeInvokeCnt() {
            return decodeInvokeCnt;
        }
    }
}
