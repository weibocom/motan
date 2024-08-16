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
import com.weibo.api.motan.filter.GlobalDynamicFilter;

import java.util.Map;

/**
 * @author zhanglei28
 */
public class DynamicFilterHandler extends AbstractAdminCommandHandler {
    private static final String[] commands = new String[]{
            "/dynamicFilter/set",
            "/dynamicFilter/get",
            "/dynamicFilter/clear"};

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        if (commands[0].equals(command)) {
            // set dynamic filter, an exception will be thrown if the filter does not exist
            GlobalDynamicFilter.setDynamicFilter(new GlobalDynamicFilter.ConditionFilter(params.get("filter"), params.get("condition")));
        } else if (commands[1].equals(command)) {
            GlobalDynamicFilter.ConditionFilter filter = GlobalDynamicFilter.getDynamicFilter();
            if (filter != null) {
                result.put("filter", filter.toJson());
            }
        } else if (commands[2].equals(command)) {
            GlobalDynamicFilter.setDynamicFilter(null);
        }
    }

    @Override
    public String[] getCommandName() {
        return commands;
    }
}
