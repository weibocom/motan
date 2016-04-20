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

package com.weibo.api.motan.switcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
@SpiMeta(name = "localSwitcherService")
public class LocalSwitcherService implements SwitcherService {

    private static ConcurrentMap<String, Switcher> switchers = new ConcurrentHashMap<String, Switcher>();

    @Override
    public Switcher getSwitcher(String name) {
        return switchers.get(name);
    }

    @Override
    public List<Switcher> getAllSwitchers() {
        return new ArrayList<Switcher>(switchers.values());
    }

    public static Switcher getSwitcherStatic(String name) {
        return switchers.get(name);
    }

    public static List<Switcher> getAllSwitchersStatic() {
        return new ArrayList<Switcher>(switchers.values());
    }

    public static void putSwitcher(Switcher switcher) {
        if (switcher == null) {
            throw new MotanFrameworkException("LocalSwitcherService addSwitcher Error: switcher is null");
        }

        switchers.put(switcher.getName(), switcher);
    }

    public static void putSwitcher(String name, boolean on) {
        if (switchers.get(name) != null) {
            LoggerUtil.warn("LocalSwitcherService addSwitcher Error: switcher exists");
            return;
        }
        Switcher switcher = new Switcher(name, on);

        switchers.putIfAbsent(name, switcher);
    }

    public static void onSwitcher(String name) {
        Switcher switcher = switchers.get(name);
        if (switcher == null) {
            switcher = new Switcher(name, true);
            putSwitcher(switcher);
        }

        switcher.onSwitcher();
    }

    public static void offSwitcher(String name) {
        Switcher switcher = switchers.get(name);
        if (switcher == null) {
            switcher = new Switcher(name, false);
            putSwitcher(switcher);
        }

        switcher.offSwitcher();
    }

    @Override
    public void initSwitcher(String switcherName, boolean initialValue) {
        putSwitcher(switcherName, initialValue);
    }

    @Override
    public boolean switcherIsOpen(String switcherName) {
        Switcher switcher = switchers.get(switcherName);
        return switcher != null && switcher.isOn();
    }

    @Override
    public boolean switcherIsOpen(String switcherName, boolean defaultValue) {
        Switcher switcher = switchers.get(switcherName);
        if (switcher == null) {
            switchers.putIfAbsent(switcherName, new Switcher(switcherName, defaultValue));
            switcher = switchers.get(switcherName);
        }
        return switcher.isOn();
    }

    @Override
    public void setSwitcher(String switcherName, boolean value) {
        putSwitcher(new Switcher(switcherName, value));
    }

}
