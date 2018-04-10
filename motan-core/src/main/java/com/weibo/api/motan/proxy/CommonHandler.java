package com.weibo.api.motan.proxy;

/**
 * @author sunnights
 */
public interface CommonHandler {
    /**
     * call a service method with general handler
     *
     * @param methodName the method name of remote service
     * @param params     an array of objects containing the values of the arguments passed in the method invocation
     * @param returnType the class type that the method returns
     * @return
     * @throws Throwable
     */
    Object call(String methodName, Object[] params, Class returnType) throws Throwable;

    /**
     * async call a service with general handler
     *
     * @param methodName
     * @param params
     * @param returnType
     * @return
     * @throws Throwable
     */
    Object asyncCall(String methodName, Object[] params, Class returnType) throws Throwable;
}
