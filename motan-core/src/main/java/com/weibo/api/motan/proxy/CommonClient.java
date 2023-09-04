/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.proxy;

import com.weibo.api.motan.rpc.Request;

/**
 * @since 1.2.1
 */
public interface CommonClient {
    /**
     * call a service method
     *
     * @param methodName the method name of remote service
     * @param arguments  an array of objects containing the values of the arguments passed in the method invocation
     * @param returnType the class type that the method returns
     * @return return value
     * @throws Throwable exception
     */
    Object call(String methodName, Object[] arguments, Class<?> returnType) throws Throwable;

    /**
     * call a service method asynchronously
     */
    Object asyncCall(String methodName, Object[] arguments, Class<?> returnType) throws Throwable;

    /**
     * call a service method with request
     */
    Object call(Request request, Class<?> returnType) throws Throwable;

    /**
     * call a service with request asynchronously
     */
    Object asyncCall(Request request, Class<?> returnType) throws Throwable;

    /**
     * build request with methodName and arguments
     */
    Request buildRequest(String methodName, Object[] arguments);

    /**
     * build request with interfaceName, methodName and arguments
     */
    Request buildRequest(String interfaceName, String methodName, Object[] arguments);

}
