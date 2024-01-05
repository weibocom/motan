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

package com.weibo.api.motan.admin.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.admin.AbstractAdminCommandHandler;
import com.weibo.api.motan.admin.AdminUtil;
import com.weibo.api.motan.admin.DefaultAdminHandler;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class CommandListHandler extends AbstractAdminCommandHandler {

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        JSONArray jsonArray = new JSONArray();
        if (AdminUtil.getDefaultAdminHandler() instanceof DefaultAdminHandler) {
            jsonArray.addAll(((DefaultAdminHandler) AdminUtil.getDefaultAdminHandler()).getCommandSet());
        }
        result.put("commandList", jsonArray);
    }

    @Override
    public String[] getCommandName() {
        return new String[]{"/command/list"};
    }
}
