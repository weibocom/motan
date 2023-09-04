package com.weibo.api.motan.proxy;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.MotanClientUtil;

import java.util.List;

/**
 * @author sunnights
 */
public class RefererCommonHandler<T> extends AbstractRefererHandler<T> implements CommonHandler {

    public RefererCommonHandler(String interfaceName, List<Cluster<T>> clusters) {
        this.interfaceName = interfaceName;
        this.clusters = clusters;
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

    public Request buildRequestV1(String interfaceName, String methodName, Object[] arguments,String parametersDesc) {
        return MotanClientUtil.buildRequestV1(interfaceName, methodName, arguments,parametersDesc);
    }

}
