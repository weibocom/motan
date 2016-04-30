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

package com.weibo.api.motan.util;

import com.weibo.api.motan.switcher.LocalSwitcherService;
import com.weibo.api.motan.switcher.SwitcherListener;
import com.weibo.api.motan.switcher.SwitcherService;



/**
 * 静态开关工具类。一般全局开关使用此类。 可以替换switcherService为不同实现
 * 
 * @author zhanglei
 *
 */
public class MotanSwitcherUtil {
    private static SwitcherService switcherService = new LocalSwitcherService();

    public static void initSwitcher(String switcherName, boolean initialValue) {
        switcherService.initSwitcher(switcherName, initialValue);
    }

    /**
     * 检查开关是否开启。
     * 
     * @param switcherName
     * @return true ：设置了开关，并且开关值为true false：未设置开关或开关为false
     */
    public static boolean isOpen(String switcherName) {
        return switcherService.isOpen(switcherName);
    }

    /**
     * 检查开关是否开启，如果开关不存在则将开关置默认值，并返回。
     * 
     * @param switcherName
     * @param defaultValue
     * @return 开关存在时返回开关值，开关不存在时设置开关为默认值，并返回默认值。
     */
    public static boolean switcherIsOpenWithDefault(String switcherName, boolean defaultValue) {
        return switcherService.isOpen(switcherName, defaultValue);
    }

    /**
     * 设置开关状态。
     * 
     * @param switcherName
     * @param value
     * @return
     */
    public static void setSwitcherValue(String switcherName, boolean value) {
        switcherService.setValue(switcherName, value);
    }

    public static SwitcherService getSwitcherService() {
        return switcherService;
    }

    public static void setSwitcherService(SwitcherService switcherService) {
        MotanSwitcherUtil.switcherService = switcherService;
    }

    public static void registerSwitcherListener(String switcherName, SwitcherListener listener) {
        switcherService.registerListener(switcherName, listener);
    }

}
