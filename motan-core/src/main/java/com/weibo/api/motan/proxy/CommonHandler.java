package com.weibo.api.motan.proxy;

import com.weibo.api.motan.rpc.Request;

/**
 * @author sunnights
 */
public interface CommonHandler {
    /**
     * call a service method with general handler
     *
     * @param methodName the method name of remote service
     * @param arguments     an array of objects containing the values of the arguments passed in the method invocation
     * @param returnType the class type that the method returns
     * @return
     * @throws Throwable
     */
    Object call(String methodName, Object[] arguments, Class returnType) throws Throwable;

    /**
     * async call a service with general handler
     *
     * @param methodName
     * @param arguments
     * @param returnType
     * @return
     * @throws Throwable
     */
    Object asyncCall(String methodName, Object[] arguments, Class returnType) throws Throwable;

    /**
     * call a service method with request
     *
     * @param request
     * @param returnType
     * @return
     */
    Object call(Request request, Class returnType) throws Throwable;

    /**
     * async call a service with request
     *
     * @param request
     * @param returnType
     * @return
     */
    Object asyncCall(Request request, Class returnType) throws Throwable;

    /**
     * build request with methodName and arguments
     *
     * @param methodName
     * @param arguments
     * @return
     */
    Request buildRequest(String methodName, Object[] arguments);
}
