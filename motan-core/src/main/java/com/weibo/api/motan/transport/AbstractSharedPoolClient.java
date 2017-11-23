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
import com.weibo.api.motan.util.MathUtil;

import java.util.ArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author sunnights
 */
public abstract class AbstractSharedPoolClient extends AbstractClient {
    private static final ThreadPoolExecutor executor = new StandardThreadExecutor(1, 300, 20000,
            new DefaultThreadFactory("AbstractPoolClient-initPool-", true));
    private final AtomicInteger idx = new AtomicInteger();
    protected SharedObjectFactory factory;
    protected ArrayList<Object> objects;
    private int connections;

    public AbstractSharedPoolClient(URL url) {
        super(url);
    }

    protected void initPool() {
        factory = createChannelFactory();
        connections = url.getIntParameter(URLParamType.maxClientConnection.getName(), URLParamType.maxClientConnection.getIntValue());

        objects = new ArrayList<>(connections);
        for (int i = 0; i < connections; i++) {
            objects.add(factory.makeObject());
        }

        initConnections(false);
    }

    protected abstract SharedObjectFactory createChannelFactory();

    protected void initConnections(boolean async) {
        if (async) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    createConnections();
                }
            });
        } else {
            createConnections();
        }
    }

    private void createConnections() {
        for (Object object : objects) {
            try {
                factory.initObject(object);
            } catch (Exception e) {
                LoggerUtil.error("NettyClient init pool create connect Error: url=" + url.getUri(), e);
            }
        }
    }

    protected Channel getObject() throws MotanServiceException {
        int index = MathUtil.getPositive(idx.getAndIncrement()) % connections;
        Channel channel = (Channel) objects.get(index);

        if (channel.isAvailable()) {
            return channel;
        } else {
            factory.rebuildObject(channel);
        }

        for (int i = index + 1; i < connections + index; i++) {
            channel = (Channel) objects.get(i % connections);
            if (channel.isAvailable()) {
                return channel;
            } else {
                factory.rebuildObject(channel);
            }
        }

        String errorMsg = this.getClass().getSimpleName() + " getObject Error: url=" + url.getUri();
        LoggerUtil.error(errorMsg);
        throw new MotanServiceException(errorMsg);
    }
}
