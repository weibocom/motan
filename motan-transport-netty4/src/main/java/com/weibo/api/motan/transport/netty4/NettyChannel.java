package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.codec.Codec;
import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import io.netty.channel.ChannelFuture;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author sunnights
 */
public class NettyChannel implements Channel {
    private volatile ChannelState state = ChannelState.UNINIT;
    private NettyClient nettyClient;
    private io.netty.channel.Channel channel = null;
    private InetSocketAddress remoteAddress = null;
    private InetSocketAddress localAddress = null;
    private ReentrantLock lock = new ReentrantLock();
    private Codec codec;

    public NettyChannel(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
        this.remoteAddress = new InetSocketAddress(nettyClient.getUrl().getHost(), nettyClient.getUrl().getPort());

        codec = ExtensionLoader.getExtensionLoader(Codec.class).getExtension(nettyClient.getUrl().getParameter(URLParamType.codec.getName(), URLParamType.codec.getValue()));
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public Response request(Request request) throws TransportException {
        int timeout = nettyClient.getUrl().getMethodParameter(request.getMethodName(), request.getParamtersDesc(), URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getIntValue());
        if (timeout <= 0) {
            throw new MotanFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        ResponseFuture response = new DefaultResponseFuture(request, timeout, this.nettyClient.getUrl());
        this.nettyClient.registerCallback(request.getRequestId(), response);
        byte[] msg = CodecUtil.encodeObjectToBytes(this, codec, request);
        ChannelFuture writeFuture = this.channel.writeAndFlush(msg);
        boolean result = writeFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);

        if (result && writeFuture.isSuccess()) {
            response.addListener(new FutureListener() {
                @Override
                public void operationComplete(Future future) throws Exception {
                    if (future.isSuccess() || (future.isDone() && ExceptionUtil.isBizException(future.getException()))) {
                        // 成功的调用
                        nettyClient.resetErrorCount();
                    } else {
                        // 失败的调用
                        nettyClient.incrErrorCount();
                    }
                }
            });
            return response;
        }

        writeFuture.cancel(true);
        response = this.nettyClient.removeCallback(request.getRequestId());
        if (response != null) {
            response.cancel();
        }
        // 失败的调用
        nettyClient.incrErrorCount();

        if (writeFuture.cause() != null) {
            throw new MotanServiceException("NettyChannel send request to server Error: url="
                    + nettyClient.getUrl().getUri() + " local=" + localAddress + " "
                    + MotanFrameworkUtil.toString(request), writeFuture.cause());
        } else {
            throw new MotanServiceException("NettyChannel send request to server Timeout: url="
                    + nettyClient.getUrl().getUri() + " local=" + localAddress + " "
                    + MotanFrameworkUtil.toString(request));
        }
    }

    @Override
    public synchronized boolean open() {
        if (isAvailable()) {
            LoggerUtil.warn("the channel already open, local: " + localAddress + " remote: " + remoteAddress + " url: " + nettyClient.getUrl().getUri());
            return true;
        }

        ChannelFuture channelFuture = null;
        try {
            long start = System.currentTimeMillis();
            channelFuture = nettyClient.getBootstrap().connect(remoteAddress);
            int timeout = nettyClient.getUrl().getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
            if (timeout <= 0) {
                throw new MotanFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
            }
            // 不去依赖于connectTimeout
            boolean result = channelFuture.awaitUninterruptibly(timeout, TimeUnit.MILLISECONDS);
            boolean success = channelFuture.isSuccess();

            if (result && success) {
                channel = channelFuture.channel();
                if (channel.localAddress() != null && channel.localAddress() instanceof InetSocketAddress) {
                    localAddress = (InetSocketAddress) channel.localAddress();
                }
                state = ChannelState.ALIVE;
                return true;
            }
            boolean connected = false;
            if (channelFuture.channel() != null) {
                connected = channelFuture.channel().isActive();
            }

            if (channelFuture.cause() != null) {
                channelFuture.cancel(true);
                throw new MotanServiceException("NettyChannel failed to connect to server, url: " + nettyClient.getUrl().getUri() + ", result: " + result + ", success: " + success + ", connected: " + connected, channelFuture.cause());
            } else {
                channelFuture.cancel(true);
                throw new MotanServiceException("NettyChannel connect to server timeout url: " + nettyClient.getUrl().getUri() + ", cost: " + (System.currentTimeMillis() - start) + ", result: " + result + ", success: " + success + ", connected: " + connected);
            }
        } catch (MotanServiceException e) {
            throw e;
        } catch (Exception e) {
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            throw new MotanServiceException("NettyChannel failed to connect to server, url: " + nettyClient.getUrl().getUri(), e);
        } finally {
            if (!state.isAliveState()) {
                nettyClient.incrErrorCount();
            }
        }
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        try {
            state = ChannelState.CLOSE;

            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            LoggerUtil.error("NettyChannel close Error: " + nettyClient.getUrl().getUri() + " local=" + localAddress, e);
        }
    }

    @Override
    public boolean isClosed() {
        return state.isCloseState();
    }

    @Override
    public boolean isAvailable() {
        return state.isAliveState() && channel != null && channel.isActive();
    }

    public void reconnect() {
        state = ChannelState.INIT;
    }

    public boolean isReconnect() {
        return state.isInitState();
    }

    @Override
    public URL getUrl() {
        return nettyClient.getUrl();
    }

    public ReentrantLock getLock() {
        return lock;
    }
}
