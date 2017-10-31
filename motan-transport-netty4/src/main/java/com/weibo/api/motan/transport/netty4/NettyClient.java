package com.weibo.api.motan.transport.netty4;

import com.weibo.api.motan.common.ChannelState;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.transport.AbstractPoolClient;
import com.weibo.api.motan.transport.Channel;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.TransportException;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.StatisticCallback;
import com.weibo.api.motan.util.StatsUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.commons.pool.BasePoolableObjectFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author sunnights
 */
public class NettyClient extends AbstractPoolClient implements StatisticCallback {
    /**
     * 回收过期任务
     */
    private static ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(4);
    /**
     * 异步的request，需要注册callback future
     * 触发remove的操作有： 1) service的返回结果处理。 2) timeout thread cancel
     */
    protected ConcurrentMap<Long, ResponseFuture> callbackMap = new ConcurrentHashMap<>();
    private ScheduledFuture<?> timeMonitorFuture = null;
    private EventLoopGroup bossGroup;
    private Bootstrap bootstrap;
    /**
     * 连续失败次数
     */
    private AtomicLong errorCount = new AtomicLong(0);
    /**
     * 最大连接数
     */
    private int maxClientConnection = 0;

    public NettyClient(URL url) {
        super(url);
        maxClientConnection = url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());
        timeMonitorFuture = scheduledExecutor.scheduleWithFixedDelay(
                new TimeoutMonitor("timeout_monitor_" + url.getHost() + "_" + url.getPort()),
                MotanConstants.NETTY_TIMEOUT_TIMER_PERIOD, MotanConstants.NETTY_TIMEOUT_TIMER_PERIOD,
                TimeUnit.MILLISECONDS);
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    @Override
    protected BasePoolableObjectFactory createChannelFactory() {
        return new NettyChannelFactory(this);
    }

    @Override
    public Response request(Request request) throws TransportException {
        if (!isAvailable()) {
            throw new MotanServiceException("NettyChannel is unavailable: url=" + url.getUri() + MotanFrameworkUtil.toString(request));
        }
        boolean isAsync = false;
        Object async = RpcContext.getContext().getAttribute(MotanConstants.ASYNC_SUFFIX);
        if (async != null && async instanceof Boolean) {
            isAsync = (Boolean) async;
        }
        return request(request, isAsync);
    }

    @Override
    public void heartbeat(Request request) {
        // 如果节点还没有初始化或者节点已经被close掉了，那么heartbeat也不需要进行了
        if (state.isUnInitState() || state.isCloseState()) {
            LoggerUtil.warn("NettyClient heartbeat Error: state={} url={}", state.name(), url.getUri());
            return;
        }

        LoggerUtil.info("NettyClient heartbeat request: url={}", url.getUri());

        try {
            // async request后，如果service is
            // available，那么将会自动把该client设置成可用
            request(request, true);
        } catch (Exception e) {
            LoggerUtil.error("NettyClient heartbeat Error: url={}, {}", url.getUri(), e.getMessage());
        }
    }

    private Response request(Request request, boolean async) throws TransportException {
        Channel channel = null;
        Response response;
        try {
            // return channel or throw exception(timeout or connection_fail)
            channel = borrowObject();
            if (channel == null) {
                LoggerUtil.error("NettyClient borrowObject null: url=" + url.getUri() + " " + MotanFrameworkUtil.toString(request));
                return null;
            }

            // async request
            response = channel.request(request);
            // return channel to pool
            returnObject(channel);
        } catch (Exception e) {
            LoggerUtil.error("NettyClient request Error: url=" + url.getUri() + " " + MotanFrameworkUtil.toString(request), e);
            //TODO 对特定的异常回收channel
            invalidateObject(channel);

            if (e instanceof MotanAbstractException) {
                throw (MotanAbstractException) e;
            } else {
                throw new MotanServiceException("NettyClient request Error: url=" + url.getUri() + " " + MotanFrameworkUtil.toString(request), e);
            }
        }

        // aysnc or sync result
        response = asyncResponse(response, async);

        return response;
    }

    /**
     * 如果async是false，那么同步获取response的数据
     *
     * @param response
     * @param async
     * @return
     */
    private Response asyncResponse(Response response, boolean async) {
        if (async || !(response instanceof ResponseFuture)) {
            return response;
        }
        return new DefaultResponse(response);
    }

    @Override
    public synchronized boolean open() {
        if (isAvailable()) {
            return true;
        }

        if (bossGroup == null) {
            bossGroup = new NioEventLoopGroup();
        }

        bootstrap = new Bootstrap();
        int timeout = getUrl().getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
        if (timeout <= 0) {
            throw new MotanFrameworkException("NettyClient init Error: timeout(" + timeout + ") <= 0 is forbid.", MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        // 最大响应包限制
        final int maxContentLength = url.getIntParameter(URLParamType.maxContentLength.getName(), URLParamType.maxContentLength.getIntValue());
        bootstrap.group(bossGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new NettyDecoder(codec, NettyClient.this, maxContentLength));
                        pipeline.addLast("encoder", new NettyEncoder(codec, NettyClient.this));
                        pipeline.addLast("handler", new NettyChannelHandler(NettyClient.this, new MessageHandler() {
                            @Override
                            public Object handle(Channel channel, Object message) {
                                Response response = (Response) message;
                                ResponseFuture responseFuture = NettyClient.this.removeCallback(response.getRequestId());

                                if (responseFuture == null) {
                                    LoggerUtil.warn("NettyClient has response from server, but responseFuture not exist, requestId={}", response.getRequestId());
                                    return null;
                                }
                                if (response.getException() != null) {
                                    responseFuture.onFailure(response);
                                } else {
                                    responseFuture.onSuccess(response);
                                }
                                return null;
                            }
                        }));
                    }
                });

        // 初始化连接池
        initPool();

        LoggerUtil.info("NettyClient finish Open: url={}", url);

        // 注册统计回调
        StatsUtil.registryStatisticCallback(this);

        // 设置可用状态
        state = ChannelState.ALIVE;
        return state.isAliveState();
    }

    @Override
    public synchronized void close() {
        close(0);
    }

    @Override
    public synchronized void close(int timeout) {
        if (state.isCloseState()) {
            LoggerUtil.info("NettyClient close fail: already close, url={}", url.getUri());
            return;
        }

        // 如果当前nettyClient还没有初始化，那么就没有close的理由。
        if (state.isUnInitState()) {
            LoggerUtil.info("NettyClient close Fail: don't need to close because node is unInit state: url={}", url.getUri());
            return;
        }

        try {
            // 取消定期的回收任务
            timeMonitorFuture.cancel(true);
            // 关闭连接池
            pool.close();
            // 清空callback
            callbackMap.clear();
            // 设置close状态
            state = ChannelState.CLOSE;
            // 解除统计回调的注册
            StatsUtil.unRegistryStatisticCallback(this);
            LoggerUtil.info("NettyClient close Success: url={}", url.getUri());
        } catch (Exception e) {
            LoggerUtil.error("NettyClient close Error: url=" + url.getUri(), e);
        }
    }

    @Override
    public boolean isClosed() {
        return state.isCloseState();
    }

    @Override
    public boolean isAvailable() {
        return state.isAliveState();
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public String statisticCallback() {
        //避免消息泛滥，如果节点是可用状态，并且堆积的请求不超过100的话，那么就不记录log了
        if (isAvailable() && callbackMap.size() < 100) {
            return null;
        }

        return String.format("identity: %s available: %s concurrent_count: %s", url.getIdentity(), isAvailable(), callbackMap.size());
    }

    public ResponseFuture removeCallback(long requestId) {
        return callbackMap.remove(requestId);
    }

    /**
     * 增加调用失败的次数：
     * <p>
     * <pre>
     * 	 	如果连续失败的次数 >= maxClientConnection, 那么把client设置成不可用状态
     * </pre>
     */
    void incrErrorCount() {
        long count = errorCount.incrementAndGet();

        // 如果节点是可用状态，同时当前连续失败的次数超过限制maxClientConnection次，那么把该节点标示为不可用
        if (count >= maxClientConnection && state.isAliveState()) {
            synchronized (this) {
                count = errorCount.longValue();

                if (count >= maxClientConnection && state.isAliveState()) {
                    LoggerUtil.error("NettyClient unavailable Error: url=" + url.getIdentity() + " "
                            + url.getServerPortStr());
                    state = ChannelState.UNALIVE;
                }
            }
        }
    }

    /**
     * 重置调用失败的计数 ：
     * <pre>
     * 把节点设置成可用
     * </pre>
     */
    void resetErrorCount() {
        errorCount.set(0);

        if (state.isAliveState()) {
            return;
        }

        synchronized (this) {
            if (state.isAliveState()) {
                return;
            }

            // 如果节点是unalive才进行设置，而如果是 close 或者 uninit，那么直接忽略
            if (state.isUnAliveState()) {
                long count = errorCount.longValue();

                // 过程中有其他并发更新errorCount的，因此这里需要进行一次判断
                if (count < maxClientConnection) {
                    state = ChannelState.ALIVE;
                    LoggerUtil.info("NettyClient recover available: url=" + url.getIdentity() + " "
                            + url.getServerPortStr());
                }
            }
        }
    }

    /**
     * 注册回调的resposne
     * <pre>
     * 进行最大的请求并发数的控制，如果超过NETTY_CLIENT_MAX_REQUEST的话，那么throw reject exception
     * </pre>
     *
     * @param requestId
     * @param responseFuture
     * @throws MotanServiceException
     */
    public void registerCallback(long requestId, ResponseFuture responseFuture) {
        if (this.callbackMap.size() >= MotanConstants.NETTY_CLIENT_MAX_REQUEST) {
            // reject request, prevent from OutOfMemoryError
            throw new MotanServiceException("NettyClient over of max concurrent request, drop request, url: "
                    + url.getUri() + " requestId=" + requestId, MotanErrorMsgConstant.SERVICE_REJECT);
        }

        this.callbackMap.put(requestId, responseFuture);
    }

    /**
     * 回收超时任务
     *
     * @author maijunsheng
     */
    class TimeoutMonitor implements Runnable {
        private String name;

        public TimeoutMonitor(String name) {
            this.name = name;
        }

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Long, ResponseFuture> entry : callbackMap.entrySet()) {
                try {
                    ResponseFuture future = entry.getValue();

                    if (future.getCreateTime() + future.getTimeout() < currentTime) {
                        // timeout: remove from callback list, and then cancel
                        removeCallback(entry.getKey());
                        future.cancel();
                    }
                } catch (Exception e) {
                    LoggerUtil.error(name + " clear timeout future Error: uri=" + url.getUri() + " requestId=" + entry.getKey(), e);
                }
            }
        }
    }
}
