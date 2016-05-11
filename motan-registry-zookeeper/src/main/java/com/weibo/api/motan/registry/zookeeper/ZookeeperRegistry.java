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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.support.FailbackRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperRegistry extends FailbackRegistry {
    private ZkClient zkClient;
    private ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = new ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>>();

    public ZookeeperRegistry(URL url, ZkClient client) {
        super(url);
        this.zkClient = client;
    }

    public ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> getUrlListeners() {
        return urlListeners;
    }

    @Override
    protected void doRegister(URL url) {
        try {
            // 防止旧节点未正常注销
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to register %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to unregister %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doSubscribe(final URL url, final NotifyListener notifyListener) {
        try {
            ConcurrentHashMap<NotifyListener, IZkChildListener> childChangeListeners = urlListeners.get(url);
            if (childChangeListeners == null) {
                urlListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, IZkChildListener>());
                childChangeListeners = urlListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(notifyListener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(notifyListener, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) {
                        ZookeeperRegistry.this.notify(url, notifyListener, nodeChildsToUrls(parentPath, currentChilds));
                        LoggerUtil.info(String.format("[ZookeeperRegistry] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                    }
                });
                zkChildListener = childChangeListeners.get(notifyListener);
            }

            // 防止旧节点未正常注销
            removeNode(url, ZkNodeType.CLIENT);
            createNode(url, ZkNodeType.CLIENT);

            // 订阅server节点，并获取当前可用server
            String serverTypePath = toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            List<String> currentChilds = zkClient.subscribeChildChanges(serverTypePath, zkChildListener);
            LoggerUtil.info(String.format("[ZookeeperRegistry] subscribe: path=%s, info=%s", toNodePath(url, ZkNodeType.AVAILABLE_SERVER), url.toFullStr()));
            notify(url, notifyListener, nodeChildsToUrls(serverTypePath, currentChilds));
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener notifyListener) {
        try {
            Map<NotifyListener, IZkChildListener> childChangeListeners = urlListeners.get(url);
            if (childChangeListeners != null) {
                IZkChildListener zkChildListener = childChangeListeners.get(notifyListener);
                if (zkChildListener != null) {
                    zkClient.unsubscribeChildChanges(toNodeTypePath(url, ZkNodeType.CLIENT), zkChildListener);
                    childChangeListeners.remove(notifyListener);
                }
            }
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to unsubscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected List<URL> doDiscover(URL url) {
        try {
            String parentPath = toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            List<String> currentChilds = new ArrayList<String>();
            if (zkClient.exists(parentPath)) {
                currentChilds = zkClient.getChildren(parentPath);
            }
            return nodeChildsToUrls(parentPath, currentChilds);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to discover %s from zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(u, ZkNodeType.AVAILABLE_SERVER);
            }
        } else {
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            createNode(url, ZkNodeType.AVAILABLE_SERVER);
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(u, ZkNodeType.UNAVAILABLE_SERVER);
            }
        } else {
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        }
    }

    private List<URL> nodeChildsToUrls(String parentPath, List<String> currentChilds) {
        List<URL> urls = new ArrayList<URL>();
        for (String node : currentChilds) {
            String nodePath = parentPath + MotanConstants.PATH_SEPARATOR + node;
            String data = zkClient.readData(nodePath, true);
            try {
                URL url = URL.valueOf(data);
                urls.add(url);
            } catch (Exception e) {
                LoggerUtil.warn(String.format("Found malformed urls from zookeeperRegistry, path=%s", nodePath), e);
            }
        }
        return urls;
    }

    private String toGroupPath(URL url) {
        return MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE + MotanConstants.PATH_SEPARATOR + url.getGroup();
    }

    private String toServicePath(URL url) {
        return toGroupPath(url) + MotanConstants.PATH_SEPARATOR + url.getPath();
    }

    private String toNodeTypePath(URL url, ZkNodeType nodeType) {
        String type;
        if (nodeType == ZkNodeType.AVAILABLE_SERVER) {
            type = "server";
        } else if (nodeType == ZkNodeType.UNAVAILABLE_SERVER) {
            type = "unavailableServer";
        } else if (nodeType == ZkNodeType.CLIENT) {
            type = "client";
        } else {
            throw new MotanFrameworkException(String.format("Failed to get nodeTypePath, url: %s type: %s", url, nodeType.toString()));
        }
        return toServicePath(url) + MotanConstants.PATH_SEPARATOR + type;
    }

    private String toNodePath(URL url, ZkNodeType nodeType) {
        return toNodeTypePath(url, nodeType) + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private void createNode(URL url, ZkNodeType nodeType) {
        String nodeTypePath = toNodeTypePath(url, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            zkClient.createPersistent(nodeTypePath, true);
        }
        zkClient.createEphemeral(toNodePath(url, nodeType), url.toFullStr());
    }

    private void removeNode(URL url, ZkNodeType nodeType) {
        String nodePath = toNodePath(url, nodeType);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }
}
