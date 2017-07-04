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
package com.weibo.api.motan.protocol.restful;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.protocol.restful.support.RestfulContainerRequest;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

@SpiMeta(name = "serverf")
public class ServerFilter implements Filter {

    @Override
    public Response filter(Caller<?> caller, Request request) {
        assert request instanceof RestfulContainerRequest;

        RestfulContainerRequest req = (RestfulContainerRequest) request;
        if (!"testName".equals(req.getAttachments().get("testName"))) {
            DefaultResponse resp = new DefaultResponse(request.getRequestId());
            resp.setException(new IllegalStateException("must contain testName attachment"));
            return resp;
        }

        assert request.getInterfaceName() != null;
        assert request.getMethodName() != null;
        assert request.getParamtersDesc() != null;

        // obtain httpRequest、 requestUri、httpMethod and so on
        req.getHttpRequest();

        return caller.call(request);
    }

}
