package com.weibo.api.motan.transport.netty4.server;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.NetUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by guohang.bao on 16/5/17.
 */
@ChannelHandler.Sharable
public class Netty4ServerHandler extends SimpleChannelInboundHandler<Request> {

    private ThreadPoolExecutor executor;
    private MessageHandler messageHandler;
    private Channel serverChannel;


    public Netty4ServerHandler(ThreadPoolExecutor executor, MessageHandler messageHandler, Channel serverChannel) {
        this.executor = executor;
        this.messageHandler = messageHandler;
        this.serverChannel = serverChannel;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelRegistered: remote=" + ctx.channel().remoteAddress()
                + " local=" + ctx.channel().localAddress());

    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        LoggerUtil.info("NettyChannelHandler channelDisconnected: remote=" + ctx.channel().remoteAddress()
                + " local=" + ctx.channel().localAddress());

    }


    /**
     * <pre>
     *  request process: 主要来自于client的请求，需要使用threadPoolExecutor进行处理，避免service message处理比较慢导致iothread被阻塞
     * </pre>
     *
     * @param ctx
     * @param request
     */
    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Request request) throws Exception {
        request.setAttachment(URLParamType.host.getName(), NetUtils.getHostName(ctx.channel().remoteAddress()));

        final long processStartTime = System.currentTimeMillis();

        // 使用线程池方式处理
        try {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    processRequest(ctx, request, processStartTime);
                }
            });
        } catch (RejectedExecutionException rejectException) {
            DefaultResponse response = new DefaultResponse();
            response.setRequestId(request.getRequestId());
            response.setException(new MotanServiceException("process thread pool is full, reject",
                    MotanErrorMsgConstant.SERVICE_REJECT));
            response.setProcessTime(System.currentTimeMillis() - processStartTime);
            ctx.channel().writeAndFlush(response);

            LoggerUtil
                    .debug("process thread pool is full, reject, active={} poolSize={} corePoolSize={} maxPoolSize={} taskCount={} requestId={}",
                            executor.getActiveCount(), executor.getPoolSize(),
                            executor.getCorePoolSize(), executor.getMaximumPoolSize(),
                            executor.getTaskCount(), request.getRequestId());
        }
    }


    private void processRequest(ChannelHandlerContext ctx, Request request, long processStartTime) {
        Object result = messageHandler.handle(serverChannel, request);

        DefaultResponse response = null;

        if (!(result instanceof DefaultResponse)) {
            response = new DefaultResponse(result);
        } else {
            response = (DefaultResponse) result;
        }

        response.setRequestId(request.getRequestId());
        response.setProcessTime(System.currentTimeMillis() - processStartTime);

        if (ctx.channel().isOpen()) {
            ctx.channel().writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        LoggerUtil.error("NettyChannelHandler exceptionCaught: remote=" + ctx.channel().remoteAddress()
                + " local=" + ctx.channel().localAddress() + " event=" + e.getCause(), e.getCause());

        ctx.channel().close();
    }
}
