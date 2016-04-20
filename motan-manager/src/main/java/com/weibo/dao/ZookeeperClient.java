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

package com.weibo.dao;

import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.utils.ManagerConstants;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZookeeperClient {

    private ZkClient zkClient;

    private ZookeeperClient() {
        try {
            zkClient = new ZkClient(ManagerConstants.ZOOKEEPER_URL, 10000);
        } catch (ZkException e) {
            LoggerUtil.error("[ZookeeperRegistry] fail to init zookeeper, cause: " + e.getMessage());
        }
    }

    public static ZookeeperClient getInstance() {
        return ZookeeperClientHolder.INSTANCE;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public List<String> getChildren(String path) {
        List<String> children = new ArrayList<String>();
        if (zkClient.exists(path)) {
            children = zkClient.getChildren(path);
        }
        return children;
    }


    private static class ZookeeperClientHolder {
        private static final ZookeeperClient INSTANCE = new ZookeeperClient();
    }
}
