package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import com.weibo.api.motan.util.RequestIdGenerator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;

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
        Assert.assertTrue(nettyServer.channelManage.getChannels().size() < minClientConnection * 2);

        nettyClient.close();
        nettyClient2.close();
        Thread.sleep(100);
        Assert.assertEquals(0, nettyServer.channelManage.getChannels().size());
    }

    @Test
    public void testServerTrace() throws InterruptedException {
        MotanSwitcherUtil.setSwitcherValue(MotanConstants.MOTAN_TRACE_INFO_SWITCHER, true);
        final Set<String> serverTraceKey = new HashSet<>();
        serverTraceKey.add(MotanConstants.TRACE_SRECEIVE);
        serverTraceKey.add(MotanConstants.TRACE_SDECODE);
        serverTraceKey.add(MotanConstants.TRACE_SEXECUTOR_START);
        serverTraceKey.add(MotanConstants.TRACE_PROCESS);
        serverTraceKey.add(MotanConstants.TRACE_SENCODE);
        serverTraceKey.add(MotanConstants.TRACE_SSEND);

        nettyServer = new NettyServer(url, new MessageHandler() {
            @Override
            public Object handle(Channel channel, Object message) {
                final Request request = (Request) message;

                if (request instanceof Traceable) {
                    if (((Traceable) request).getTraceableContext().getReceiveTime() != 0) {
                        serverTraceKey.remove(MotanConstants.TRACE_SRECEIVE);
                    }
                    serverTraceKey.removeAll(((Traceable) request).getTraceableContext().getTraceInfoMap().keySet());
                }
                final DefaultResponse response = new DefaultResponse();
                response.setRequestId(request.getRequestId());
                response.setValue("method: " + request.getMethodName() + " requestId: " + request.getRequestId());
                response.addFinishCallback(new Runnable() {
                    @Override
                    public void run() {
                        serverTraceKey.removeAll(((Traceable) response).getTraceableContext().getTraceInfoMap().keySet());
                        if (((Traceable) response).getTraceableContext().getSendTime() != 0) {
                            serverTraceKey.remove(MotanConstants.TRACE_SSEND);
                        }
                    }
                }, null);
                return response;
            }
        });
        nettyServer.open();

        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName("hello");
        request.setParamtersDesc("void");
        TraceableContext requestTraceableContext = request.getTraceableContext();
        Assert.assertEquals(0, requestTraceableContext.getSendTime());
        Assert.assertEquals(0, requestTraceableContext.getReceiveTime());

        NettyClient nettyClient = new NettyClient(url);
        nettyClient.open();

        Response response;
        try {
            response = nettyClient.request(request);
            Object result = response.getValue();
            Assert.assertEquals("method: " + request.getMethodName() + " requestId: " + request.getRequestId(), result);
            Assert.assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CONNECTION));
            Assert.assertNotNull(requestTraceableContext.getTraceInfo(MotanConstants.TRACE_CENCODE));
            Assert.assertFalse(0 == requestTraceableContext.getSendTime());
            if (response instanceof Traceable) {
                Assert.assertFalse(0 == ((Traceable) response).getTraceableContext().getReceiveTime());
                Assert.assertNotNull(((Traceable) response).getTraceableContext().getTraceInfo(MotanConstants.TRACE_CDECODE));
            }
        } catch (Exception e) {
            fail();
        }
        Thread.sleep(100);
        nettyClient.close();
        Assert.assertTrue(serverTraceKey.isEmpty());
    }

}