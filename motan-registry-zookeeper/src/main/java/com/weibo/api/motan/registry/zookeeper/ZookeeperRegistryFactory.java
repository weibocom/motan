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

package com.weibo.api.motan.registry.zookeeper;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.support.AbstractRegistryFactory;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;

@SpiMeta(name = "zookeeper")
public class ZookeeperRegistryFactory extends AbstractRegistryFactory {
    @Override
    protected Registry createRegistry(URL registryUrl) {
        try {
            int timeout = registryUrl.getIntParameter(URLParamType.connectTimeout.getName(), URLParamType.connectTimeout.getIntValue());
            int sessionTimeout =
                    registryUrl.getIntParameter(URLParamType.registrySessionTimeout.getName(),
                            URLParamType.registrySessionTimeout.getIntValue());
            ZkClient zkClient = new ZkClient(registryUrl.getParameter("address"), sessionTimeout, timeout, new StringSerializer());
            return new ZookeeperRegistry(registryUrl, zkClient);
        } catch (ZkException e) {
            LoggerUtil.error("[ZookeeperRegistry] fail to connect zookeeper, cause: " + e.getMessage());
            throw e;
        }
    }
}
