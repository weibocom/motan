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

package com.weibo.api.motan.admin;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public abstract class AbstractAdminCommandHandler implements AdminCommandHandler {

    @Override
    public Response handle(Request request) {
        JSONObject result = new JSONObject();
        result.put("from", "Motan Admin");
        result.put("result", "ok"); // default result.
        process(request.getMethodName(), AdminUtil.getParams(request), request.getAttachments(), result);
        return AdminUtil.buildResponse(request, result.toJSONString());
    }

    /**
     * Process admin command.
     * If the processing fails, an exception can be thrown safely and the upper layer will handle it uniformly.
     *
     * @param command     admin command
     * @param params      request params, i.e. http parameters or rpc arguments
     * @param attachments http headers or rpc attachments
     * @param result      json result. it will be the response value
     */
    protected abstract void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result);
}
