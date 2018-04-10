package com.weibo.api.motan.proxy;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.api.motan.util.RequestIdGenerator;

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

    public Object call(String methodName, Object[] params, Class returnType, boolean async) throws Throwable {
        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setArguments(params);
        request.setMethodName(methodName);
        request.setParamtersDesc(ReflectUtil.getParamsDesc(params));
        request.setInterfaceName(interfaceName);

        return invokeRequest(request, returnType, async);
    }

    @Override
    public Object call(String methodName, Object[] params, Class returnType) throws Throwable {
        return call(methodName, params, returnType, false);
    }

    @Override
    public Object asyncCall(String methodName, Object[] params, Class returnType) throws Throwable {
        return call(methodName, params, returnType, true);
    }
}
