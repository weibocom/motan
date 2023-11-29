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
import com.weibo.api.motan.exception.MotanServiceException;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/23.
 */
public class HelloHandler extends AbstractAdminCommandHandler {
    private static final String[] NAMES = new String[]{"/hello", "/error"};

    @Override
    public String[] getCommandName() {
        return NAMES;
    }

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        if (NAMES[0].equals(command)) {
            result.put("hello", "hi");
        } else {
            throw new MotanServiceException("expect error");
        }
    }
}
