package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author sunnights
 */
public class NettyServerTest {
    private NettyServer nettyServer;
    private URL url;
    private String interfaceName = "com.weibo.api.motan.protocol.example.IHello";

    @Before
    public void setUp() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("requestTimeout", "500");
        url = new URL("netty", "localhost", 18080, interfaceName, parameters);
    }

    @After
    public void tearDown() {
        if (nettyServer != null) {
            nettyServer.close();
        }
    }

    @Test
    public void testMaxServerConnection() throws InterruptedException {
        int minClientConnection = 5;
        int maxServerConnection = 7;
        url.addParameter(URLParamType.minClientConnection.getName(), String.valueOf(minClientConnection));
        url.addParameter(URLParamType.maxServerConnection.getName(), String.valueOf(maxServerConnection));
        url.addParameter(URLParamType.requestTimeout.getName(), "10000");
        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                Request request = (Request) message;
                DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());
                return response;
            }
        });
        nettyServer.open();
        Assert.assertEquals(0, nettyServer.channelManage.getChannels().size());

        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();
        Thread.sleep(100);
        Assert.assertEquals(minClientConnection, nettyServer.channelManage.getChannels().size());

        NettyClient nettyClient2 = new NettyClient(url);
        nettyClient2.open();
        Thread.sleep(100);
        Assert.assertEquals(maxServerConnection, nettyServer.channelManage.getChannels().size());

        nettyClient.close();
        nettyClient2.close();
        Thread.sleep(100);
        Assert.assertEquals(0, nettyServer.channelManage.getChannels().size());
    }

}