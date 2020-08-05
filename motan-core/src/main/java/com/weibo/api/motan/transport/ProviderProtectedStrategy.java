package com.weibo.api.motan.transport;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chenxl on 2020/6/9.
 */
@Spi(scope = Scope.PROTOTYPE)
public interface ProviderProtectedStrategy {

    Response call(Request request, Provider<?> provider);

    void setMethodCounter(AtomicInteger methodCounter);

}
