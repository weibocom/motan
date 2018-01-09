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

import com.weibo.api.motan.rpc.DefaultResponse;
import org.jboss.resteasy.specimpl.BuiltResponse;

public class RestfulClientResponse extends DefaultResponse {
    private static final long serialVersionUID = -2780120101690526109L;

    private BuiltResponse httpResponse;

    public RestfulClientResponse() {
    }

    public RestfulClientResponse(long requestId) {
        super(requestId);
    }

    public BuiltResponse getHttpResponse() {
        return httpResponse;
    }

    public void setHttpResponse(BuiltResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

}
