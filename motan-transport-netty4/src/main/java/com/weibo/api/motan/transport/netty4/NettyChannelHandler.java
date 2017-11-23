package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.RpcContext;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.NetUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author sunnights
 */
public class NettyChannelHandler extends ChannelDuplexHandler {
    private ThreadPoolExecutor threadPoolExecutor;
    private MessageHandler messageHandler;
    private Channel serverChannel;

    public NettyChannelHandler(Channel serverChannel) {
        this.serverChannel = serverChannel;
    }

    public NettyChannelHandler(Channel serverChannel, MessageHandler messageHandler) {
        this.serverChannel = serverChannel;
        this.messageHandler = messageHandler;
    }

    public NettyChannelHandler(Channel serverChannel, MessageHandler messageHandler,
                               ThreadPoolExecutor threadPoolExecutor) {
        this.serverChannel = serverChannel;
        this.messageHandler = messageHandler;
        this.threadPoolExecutor = threadPoolExecutor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Request) {
            processRequest(ctx, (Request) msg);
        } else if (msg instanceof Response) {
            processResponse(ctx, msg);
        } else {
            LoggerUtil.error("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
            throw new MotanFrameworkException("NettyChannelHandler messageReceived type not support: class=" + msg.getClass());
        }
    }

    private void processRequest(final ChannelHandlerContext ctx, Request originRequest) {
        final Request request = originRequest;
        request.setAttachment(URLParamType.host.getName(), NetUtils.getHostName(ctx.channel().remoteAddress()));
        final long processStartTime = System.currentTimeMillis();
        // 使用线程池方式处理
        try {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        RpcContext.init(request);
                        Object result;
                        try {
                            result = messageHandler.handle(serverChannel, request);
                        } catch (Exception e) {
                            LoggerUtil.error("NettyChannelHandler processRequest fail! request:" + MotanFrameworkUtil.toString(request), e);
                            result = MotanFrameworkUtil.buildErrorResponse(request.getRequestId(), new MotanServiceException("process request fail. errmsg:" + e.getMessage()));
                        }
                        DefaultResponse response;
                        if (result instanceof DefaultResponse) {
                            response = (DefaultResponse) result;
                        } else {
                            response = new DefaultResponse(result);
                        }
                        response.setRequestId(request.getRequestId());
                        response.setProcessTime(System.currentTimeMillis() - processStartTime);
                        if (ctx.channel().isActive()) {
                            ctx.channel().writeAndFlush(response);
                        }
                    } finally {
                        RpcContext.destroy();
                    }
                }
            });
        } catch (RejectedExecutionException rejectException) {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setException(new MotanServiceException("process thread pool is full, reject", MotanErrorMsgConstant.SERVICE_REJECT));
            response.setProcessTime(System.currentTimeMillis() - processStartTime);
            ctx.channel().writeAndFlush(response);
            LoggerUtil.debug("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                    threadPoolExecutor.getActiveCount(), threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getCorePoolSize(), threadPoolExecutor.getMaximumPoolSize(),
                    threadPoolExecutor.getTaskCount(), request.getRequestId());
        }
    }

    private void processResponse(ChannelHandlerContext ctx, Object msg) {
        messageHandler.handle(serverChannel, msg);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelRegistered: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelUnregistered: remote={} local={}", ctx.channel().remoteAddress(), ctx.channel().localAddress());
        ctx.fireChannelUnregistered();
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
        ctx.fireExceptionCaught(cause);
    }
}
