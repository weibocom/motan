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
import com.weibo.api.motan.admin.handler.CommandListHandler;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Response;
import junit.framework.TestCase;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class DefaultAdminHandlerTest extends TestCase {
    public void testDefaultAdminHandler() {
        // test default permission check
        DefaultRequest request = new DefaultRequest();
        request.setAttachment(URLParamType.host.getName(), "10.0.0.1");
        checkException(request, "not allowed");

        // test update permission checker
        AdminUtil.updatePermissionChecker(request1 -> "10.0.0.1".equals(request1.getAttachments().get(URLParamType.host.getName())));

        // test command not found
        request.setMethodName("/command/list");
        checkException(request, "unknown command " + request.getMethodName());

        // test add command handler
        AdminUtil.addCommandHandler(new CommandListHandler());
        checkOk(request, "commandList");
        request.setMethodName("/hello");
        AdminUtil.addCommandHandler(new HelloHandler());
        checkOk(request, "hello");
        request.setMethodName("/error"); // check exception from command handler
        checkException(request, "expect error");
    }

    private void checkException(DefaultRequest request, String expectError) {
        Response response = AdminUtil.getDefaultAdminHandler().handle(request);
        assertNotNull(response.getException());
        JSONObject jsonObject = JSONObject.parseObject(((MotanServiceException) response.getException()).getOriginMessage());
        assertEquals("fail", jsonObject.get("result"));
        assertEquals(expectError, jsonObject.get("error"));
    }

    private void checkOk(DefaultRequest request, String existKey) {
        Response response = AdminUtil.getDefaultAdminHandler().handle(request);
        JSONObject jsonObject = JSONObject.parseObject((String) response.getValue());
        assertEquals("ok", jsonObject.get("result"));
        assertNotNull(jsonObject.get(existKey));
    }
}