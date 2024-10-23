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
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.switcher.Switcher;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 */
public class SwitcherHandler extends AbstractAdminCommandHandler {
    private static final String[] commands = new String[]{
            "/switcher/set",
            "/switcher/get",
            "/switcher/getAll"};

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        if (commands[0].equals(command)) { // set switcher
            String name = params.get("name");
            String value = params.get("value");
            if (StringUtils.isBlank(name) || StringUtils.isBlank(value)) {
                throw new MotanServiceException("switcher name or value is empty");
            }
            name = name.trim();
            boolean bValue = Boolean.parseBoolean(value.trim());
            try {
                MotanSwitcherUtil.setSwitcherValue(name, bValue);
            } catch (Exception ignore) { // if set switcher failed, try initializing a new switcher
                Switcher switcher = MotanSwitcherUtil.getOrInitSwitcher(name, bValue);
                switcher.setValue(bValue);
            }
            result.put(name, MotanSwitcherUtil.isOpen(name));
        } else if (commands[1].equals(command)) { // get switcher
            String name = params.get("name");
            if (StringUtils.isBlank(name)) {
                throw new MotanServiceException("switcher name is empty");
            }
            name = name.trim();
            result.put(name, MotanSwitcherUtil.isOpen(name));
        } else if (commands[2].equals(command)) { // get all switcher
            JSONObject jsonObject = new JSONObject();
            List<Switcher> switchers = MotanSwitcherUtil.getSwitcherService().getAllSwitchers();
            if (switchers != null && !switchers.isEmpty()) {
                for (Switcher switcher : switchers) {
                    jsonObject.put(switcher.getName(), switcher.isOn());
                }
            }
            result.put("switchers", jsonObject);
        }
    }

    @Override
    public String[] getCommandName() {
        return commands;
    }
}
