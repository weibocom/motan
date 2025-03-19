package com.weibo.api.motan.proxy;

import java.util.List;

import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.MotanClientUtil;

/**
 * @author sunnights
 */
public class RefererCommonHandler<T> extends AbstractRefererHandler<T> implements CommonHandler {

    public RefererCommonHandler(String interfaceName, List<Caller<T>> callers) {
        this.interfaceName = interfaceName;
        this.callers = callers;
        init();
    }

    @Override
    public Object call(String methodName, Object[] arguments, Class<?> returnType) throws Throwable {
        return invokeRequest(buildRequest(methodName, arguments), returnType, false);
    }

    @Override
    public Object asyncCall(String methodName, Object[] arguments, Class<?> returnType) throws Throwable {
        return invokeRequest(buildRequest(methodName, arguments), returnType, true);
    }

    @Override
    public Object call(Request request, Class<?> returnType) throws Throwable {
        return invokeRequest(request, returnType, false);
    }

    @Override
    public Object asyncCall(Request request, Class<?> returnType) throws Throwable {
        return invokeRequest(request, returnType, true);
    }

    @Override
    public Request buildRequest(String methodName, Object[] arguments) {
        return buildRequest(interfaceName, methodName, arguments);
    }

    @Override
    public Request buildRequest(String interfaceName, String methodName, Object[] arguments) {
        return MotanClientUtil.buildRequest(interfaceName, methodName, arguments);
    }
}
