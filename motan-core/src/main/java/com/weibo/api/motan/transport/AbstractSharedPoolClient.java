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
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MathUtil;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunnights
 */
public abstract class AbstractSharedPoolClient extends AbstractClient {
    private static ExecutorService rebuildExecutorService;
    private final AtomicInteger idx = new AtomicInteger();
    protected SharedObjectFactory factory;
    protected Object[] objects;
    private int size = 0;

    public AbstractSharedPoolClient(URL url) {
        super(url);
    }

    protected void initPool() {
        factory = createChannelFactory();
        int max = url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());
        ArrayList<Object> list = new ArrayList<>();

        try {
            for (int i = 0; i < max; i++) {
                list.add(factory.makeObject());
                size++;
            }
        } catch (Exception e) {
            throw new MotanServiceException("NettyClient init pool create connect Error: " + factory.toString(), e);
        }
        if (size == 0) {
            throw new MotanServiceException("NettyClient init pool create connect Error: " + factory.toString());
        }
        objects = list.toArray();
        rebuildExecutorService = new ThreadPoolExecutor(1, 3, 10L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(size),
                new DefaultThreadFactory("RebuildExecutorService", true),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    protected abstract SharedObjectFactory createChannelFactory();

    protected Channel getObject() throws MotanServiceException {
        int index = MathUtil.getPositive(idx.getAndIncrement()) % size;
        Channel channel = (Channel) objects[index];

        if (channel.isAvailable()) {
            return channel;
        } else {
            rebuildExecutorService.submit(new RebuildTask(channel));
        }

        for (int i = index + 1; i < size + index; i++) {
            channel = (Channel) objects[i % size];
            if (channel.isAvailable()) {
                return channel;
            } else {
                rebuildExecutorService.submit(new RebuildTask(channel));
            }
        }

        String errorMsg = this.getClass().getSimpleName() + " getObject Error: url=" + url.getUri();
        LoggerUtil.error(errorMsg);
        throw new MotanServiceException(errorMsg);
    }

    class RebuildTask implements Runnable {
        private Channel channel;

        public RebuildTask(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void run() {
            try {
                factory.rebuildObject(channel);
            } catch (Exception e) {
                LoggerUtil.error("rebuild error: " + factory.toString() + ", " + channel.getUrl(), e);
            }
        }
    }
}
