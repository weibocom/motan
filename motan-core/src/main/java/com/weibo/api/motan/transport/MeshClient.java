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

package com.weibo.api.motan.transport;

import com.weibo.api.motan.rpc.*;

/**
 * @author zhanglei28
 * @date 2022/12/26.
 * 与Mesh通信的client
 */
public interface MeshClient extends Caller {
    /**
     * original calling method. for framework.
     */
    Response call(Request request);

    /**
     * sync call to mesh with return type. for user
     */
    <T> T call(Request request, Class<T> returnType) throws Exception;

    /**
     * async call to mesh. for user
     */
    ResponseFuture asyncCall(Request request, Class<?> returnType) throws Exception;

    /**
     * after this method is executed, the client state will become available
     */
    void init();

    /**
     * @return true if available
     */
    boolean isAvailable();

    /**
     * mesh client will destroy inner referer(client and connection)
     */
    void destroy();

    /**
     * @return mesh client url
     */
    URL getUrl();
}
