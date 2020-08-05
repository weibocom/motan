package com.weibo.api.motan.transport;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chenxl on 2020/6/9.
 */
@SpiMeta(name = "none")
public class UnprotectedStrategy implements ProviderProtectedStrategy {

    public UnprotectedStrategy() {
    }

    @Override
    public Response call(Request request, Provider<?> provider) {
        return provider.call(request);
    }

    @Override
    public void setMethodCounter(AtomicInteger methodCounter) {
    }
}
