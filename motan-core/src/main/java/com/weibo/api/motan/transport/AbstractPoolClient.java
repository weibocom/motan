/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.transport;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.DefaultThreadFactory;
import com.weibo.api.motan.core.StandardThreadExecutor;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 */
public abstract class AbstractPoolClient extends AbstractClient {
    private static ThreadPoolExecutor executor = new StandardThreadExecutor(1, 300, 20000,
            new DefaultThreadFactory("AbstractPoolClient-initPool-", true));
    protected static long defaultMinEvictableIdleTimeMillis = (long) 1000 * 60 * 60;//默认链接空闲时间
    protected static long defaultSoftMinEvictableIdleTimeMillis = (long) 1000 * 60 * 10;//
    protected static long defaultTimeBetweenEvictionRunsMillis = (long) 1000 * 60 * 10;//默认回收周期
    protected GenericObjectPool pool;
    protected GenericObjectPool.Config poolConfig;
    protected PoolableObjectFactory factory;

    public AbstractPoolClient(URL url) {
        super(url);
    }

    protected void initPool() {
        poolConfig = new GenericObjectPool.Config();
        poolConfig.minIdle =
                url.getIntParameter(URLParamType.minClientConnection.getName(), URLParamType.minClientConnection.getIntValue());
        poolConfig.maxIdle =
                url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());
        poolConfig.maxActive = poolConfig.maxIdle;
        poolConfig.maxWait = url.getIntParameter(URLParamType.requestTimeout.getName(), URLParamType.requestTimeout.getIntValue());
        poolConfig.lifo = url.getBooleanParameter(URLParamType.poolLifo.getName(), URLParamType.poolLifo.getBooleanValue());
        poolConfig.minEvictableIdleTimeMillis = defaultMinEvictableIdleTimeMillis;
        poolConfig.softMinEvictableIdleTimeMillis = defaultSoftMinEvictableIdleTimeMillis;
        poolConfig.timeBetweenEvictionRunsMillis = defaultTimeBetweenEvictionRunsMillis;
        factory = createChannelFactory();

        pool = new GenericObjectPool(factory, poolConfig);

        boolean lazyInit = url.getBooleanParameter(URLParamType.lazyInit.getName(), URLParamType.lazyInit.getBooleanValue());

        if (!lazyInit) {
            initConnection(true);
        }
    }

    protected void initConnection(boolean async) {
        if (async) {
            try {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        createConnections();
                        LoggerUtil.info("async initPool success!" + getUrl().getUri());
                    }
                });
                return;
            } catch (Exception e) {
                LoggerUtil.error("async initPool task execute fail!" + this.url.getUri(), e);
            }
        }
        createConnections();
    }

    private void createConnections() {
        for (int i = 0; i < poolConfig.minIdle; i++) {
            try {
                pool.addObject();
            } catch (Exception e) {
                LoggerUtil.error("NettyClient init pool create connect Error: url=" + url.getUri(), e);
            }
        }
    }


    protected abstract BasePoolableObjectFactory createChannelFactory();

    protected Channel borrowObject() throws Exception {
        Channel nettyChannel = (Channel) pool.borrowObject();

        if (nettyChannel != null && nettyChannel.isAvailable()) {
            return nettyChannel;
        }

        invalidateObject(nettyChannel);

        String errorMsg = this.getClass().getSimpleName() + " borrowObject Error: url=" + url.getUri();
        LoggerUtil.error(errorMsg);
        throw new MotanServiceException(errorMsg);
    }

    protected void invalidateObject(Channel nettyChannel) {
        if (nettyChannel == null) {
            return;
        }
        try {
            pool.invalidateObject(nettyChannel);
        } catch (Exception ie) {
            LoggerUtil.error(this.getClass().getSimpleName() + " invalidate client Error: url=" + url.getUri(), ie);
        }
    }

    protected void returnObject(Channel channel) {
        if (channel == null) {
            return;
        }

        try {
            pool.returnObject(channel);
        } catch (Exception ie) {
            LoggerUtil.error(this.getClass().getSimpleName() + " return client Error: url=" + url.getUri(), ie);
        }
    }

}
