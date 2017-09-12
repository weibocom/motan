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

package com.weibo.api.motan.rpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Local app info.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-20
 */
@Deprecated()
public class ApplicationInfo {

    public static final String STATISTIC = "statisitic";
    public static final ConcurrentMap<String, Application> applications = new ConcurrentHashMap<String, Application>();

    public static Application getApplication(URL url) {
        String app = url.getApplication();
        String module = url.getModule();
        Application application = applications.get(app + "_" + module);
        if (application == null) {
            applications.putIfAbsent(app + "_" + module, new Application(app, module));
            application = applications.get(app + "_" + module);
        }
        return application;
    }

    public static void addService(URL url) {
        String app = url.getApplication();
        String module = url.getModule();
        Application application = applications.get(app + "_" + module);
        if (application == null) {
            applications.putIfAbsent(app + "_" + module, new Application(app, module));
        }
    }
}
