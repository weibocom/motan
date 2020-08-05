package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.v2motan.MotanV2Codec;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.RequestIdGenerator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;

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
    public void repeatDecodeInvokedByOldCode() {
        NettyOldFakeDecoder nettyDecoder = new NettyOldFakeDecoder(new MotanV2Codec(), nettyServer, 24);
        NettyChannelHandler handler = new NettyChannelHandler(nettyServer, messageHandler, (ThreadPoolExecutor) Executors.newFixedThreadPool(4));

        EmbeddedChannel channel = new EmbeddedChannel(nettyDecoder, handler);

        ByteBuf buf = Unpooled.wrappedBuffer(new byte[]{'a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd', 'e', 'a', 'b', 'c', 'd',
                'e', 'a', 'b', 'c', 'd', 'e',});
        channel.writeInbound(buf.copy());
        buf.release();
        assertEquals(false, 1 == nettyDecoder.getDecodeInvokeCnt());
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

    class NettyOldFakeDecoder extends NettyDecoder {

        private int decodeInvokeCnt = 0;

        public NettyOldFakeDecoder(Codec codec, Channel channel, int maxContentLength) {
            super(codec, channel, maxContentLength);
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            decodeInvokeCnt++;
            in.markReaderIndex();
            short type = in.readShort();
            if (type != MotanConstants.NETTY_MAGIC_TYPE) {
                in.resetReaderIndex();
                throw new MotanFrameworkException("NettyDecoder transport header not support, type: " + type);
            }
            throw new MotanServiceException("NettyDecoder transport data content length over of limit");
        }

        public int getDecodeInvokeCnt() {
            return decodeInvokeCnt;
        }
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
