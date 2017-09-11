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
package com.weibo.api.motan.protocol.restful.support;

import org.jboss.resteasy.spi.HttpRequest;

import com.weibo.api.motan.rpc.DefaultRequest;

/**
 * 服务端接收到的rpc request
 *
 * @author zhouhaocheng
 *
 */
public class RestfulContainerRequest extends DefaultRequest {
    private static final long serialVersionUID = 5226548801729702089L;

    private HttpRequest httpRequest;

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

}
