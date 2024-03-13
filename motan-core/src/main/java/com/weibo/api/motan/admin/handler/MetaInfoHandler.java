/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.admin.AbstractAdminCommandHandler;
import com.weibo.api.motan.runtime.GlobalRuntime;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2024/3/13.
 */
public class MetaInfoHandler extends AbstractAdminCommandHandler {
    private static final String[] commands = new String[]{
            "/meta/update",
            "/meta/delete",
            "/meta/get",
            "/meta/getAll"
    };

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        if (commands[0].equals(command)) {
            params.forEach(GlobalRuntime::putDynamicMeta);
        } else if (commands[1].equals(command)) {
            GlobalRuntime.removeDynamicMeta(params.get("key"));
        } else if (commands[2].equals(command)) {
            result.put(params.get("key"), GlobalRuntime.getDynamicMeta().get(params.get("key")));
        } else if (commands[3].equals(command)) {
            result.put("meta", GlobalRuntime.getDynamicMeta());
        }
    }

    @Override
    public String[] getCommandName() {
        return commands;
    }
}
