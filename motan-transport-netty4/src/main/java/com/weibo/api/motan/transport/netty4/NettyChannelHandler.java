package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StatisticCallback;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunnights
 */
public class NettyChannelHandler extends ChannelDuplexHandler implements StatisticCallback {
    private ThreadPoolExecutor threadPoolExecutor;
    private MessageHandler messageHandler;
    private Channel channel;
    private Codec codec;
    private AtomicInteger rejectCounter = new AtomicInteger(0);

    public NettyChannelHandler(Channel channel, MessageHandler messageHandler) {
        this.channel = channel;
        this.messageHandler = messageHandler;
        codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(channel.getUrl().getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
    }

    public NettyChannelHandler(Channel channel, MessageHandler messageHandler,
                               ThreadPoolExecutor threadPoolExecutor) {
        this.channel = channel;
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = threadPoolExecutor;
        codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(channel.getUrl().getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
    }

    private String getRemoteIp(ChannelHandlerContext ctx) {
        String ip = "";
        SocketAddress remote = ctx.channel().remoteAddress();
        if (remote != null) {
            try {
                ip = ((InetSocketAddress) remote).getAddress().getHostAddress();
            } catch (Exception e) {
                LoggerUtil.warn("get remoteIp error! default will use. msg:{}, remote:{}", e.getMessage(), remote.toString());
            }
        }
        return ip;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        if (msg instanceof NettyMessage) {
            if (threadPoolExecutor != null) {
                try {
                    threadPoolExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            processMessage(ctx, ((NettyMessage) msg));
                        }
                    });
                } catch (RejectedExecutionException rejectException) {
                    if (((NettyMessage) msg).isRequest()) {
                        rejectMessage(ctx, (NettyMessage) msg);
                    } else {
                        LoggerUtil.warn("process thread pool is full, run in io thread, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                                threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(), threadPoolExecutor.getCorePoolSize(),
                                threadPoolExecutor.getMaximumPoolSize(), threadPoolExecutor.getTaskCount(), ((NettyMessage) msg).getRequestId());
                        processMessage(ctx, (NettyMessage) msg);
                    }
                }
            } else {
                processMessage(ctx, (NettyMessage) msg);
            }
        } else {
            LoggerUtil.error("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
            throw new MotanFrameworkException("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
        }
    }

    private void rejectMessage(ChannelHandlerContext ctx, NettyMessage msg) {
        if (msg.isRequest()) {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(msg.getRequestId());
            response.setException(new MotanServiceException("process thread pool is full, reject by server: " + ctx.channel().localAddress(), MotanErrorMsgConstant.SERVICE_REJECT));
            sendResponse(ctx, response);

            LoggerUtil.error("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                    threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(), threadPoolExecutor.getCorePoolSize(),
                    threadPoolExecutor.getMaximumPoolSize(), threadPoolExecutor.getTaskCount(), msg.getRequestId());
            rejectCounter.incrementAndGet();
        }
    }

    private void processMessage(ChannelHandlerContext ctx, NettyMessage msg) {
        String remoteIp = getRemoteIp(ctx);
        Object result;
        try {
            result = codec.decode(channel, remoteIp, msg.getData());
        } catch (Exception e) {
            LoggerUtil.error("NettyDecoder decode fail! requestid" + msg.getRequestId() + ", size:" + msg.getData().length + ", ip:" + remoteIp + ", e:" + e.getMessage());
            if (msg.isRequest()) {
                Response response = buildExceptionResponse(msg.getRequestId(), e);
                sendResponse(ctx, response);
            } else {
                Response response = buildExceptionResponse(msg.getRequestId(), e);
                processResponse(response);
            }
            return;
        }

        if (result instanceof Request) {
            MotanFrameworkUtil.logRequestEvent(((Request) result).getRequestId(), "receive rpc request: " + MotanFrameworkUtil.getFullMethodString((Request) result), msg.getStartTime());
            MotanFrameworkUtil.logRequestEvent(((Request) result).getRequestId(), "after decode rpc request: " + MotanFrameworkUtil.getFullMethodString((Request) result), System.currentTimeMillis());
            if (result instanceof TraceableRequest) {
                ((TraceableRequest) result).setStartTime(msg.getStartTime());
            }
            processRequest(ctx, (Request) result);
        } else if (result instanceof Response) {
            MotanFrameworkUtil.logRequestEvent(((Response) result).getRequestId(), "receive rpc response " + channel.getUrl().getServerPortStr(), msg.getStartTime());
            processResponse(result);
        }
    }

    private Response buildExceptionResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(requestId);
        response.setException(e);
        return response;
    }

    private void processRequest(final ChannelHandlerContext ctx, final Request request) {
        request.setAttachment(URLParamType.host.getName(), NetUtils.getHostName(ctx.channel().remoteAddress()));
        final long processStartTime = System.currentTimeMillis();
        try {
            RpcContext.init(request);
            Object result;
            try {
                result = messageHandler.handle(channel, request);
            } catch (Exception e) {
                LoggerUtil.error("NettyChannelHandler processRequest fail! request:" + MotanFrameworkUtil.toString(request), e);
                result = MotanFrameworkUtil.buildErrorResponse(request.getRequestId(), new MotanServiceException("process request fail. errmsg:" + e.getMessage()));
            }
            MotanFrameworkUtil.logRequestEvent(request.getRequestId(), "after invoke biz method: " + MotanFrameworkUtil.getFullMethodString(request), System.currentTimeMillis());
            DefaultResponse response;
            if (result instanceof DefaultResponse) {
                response = (DefaultResponse) result;
            } else {
                response = new DefaultResponse(result);
            }
            response.setRequestId(request.getRequestId());
            response.setProcessTime(System.currentTimeMillis() - processStartTime);

            ChannelFuture channelFuture = sendResponse(ctx, response);
            if (channelFuture != null && request instanceof TraceableRequest) {
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        MotanFrameworkUtil.logRequestEvent(request.getRequestId(), "after send rpc response: " + MotanFrameworkUtil.getFullMethodString(request), System.currentTimeMillis());
                        ((TraceableRequest) request).onFinish();
                    }
                });
            }
        } finally {
            RpcContext.destroy();
        }
    }

    private ChannelFuture sendResponse(ChannelHandlerContext ctx, Response response) {
        byte[] msg = CodecUtil.encodeObjectToBytes(channel, codec, response);
        if (ctx.channel().isActive()) {
            return ctx.channel().writeAndFlush(msg);
        }
        return null;
    }

    private void processResponse(Object msg) {
        messageHandler.handle(channel, msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelActive: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelInactive: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LoggerUtil.error("NettyChannelHandler exceptionCaught: remote={} local={} event={}", ctx.channel().remoteAddress(), ctx.channel().localAddress(), cause.getMessage(), cause);
        ctx.channel().close();
    }

    @Override
    public String statisticCallback() {
        int count = rejectCounter.getAndSet(0);
        if (count > 0) {
            return String.format("type: motan name: reject_request_pool total_count: %s reject_count: %s", threadPoolExecutor.getPoolSize(), count);
        } else {
            return null;
        }
    }
}
