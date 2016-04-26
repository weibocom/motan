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
            setUnavailable(url);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to register %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            String availableNodePath = toServerNodePath(url, toAvailableServerTypePath(url));
            deleteNode(availableNodePath);
            String unavailableNodePath = toServerNodePath(url, toUnavailableServerTypePath(url));
            deleteNode(unavailableNodePath);
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

            String clientNodePath = toClientNodePath(url);
            // 防止旧节点未正常注销
            deleteNode(clientNodePath);

            createClientNode(url, toClientTypePath(url));

            // 获取当前可用server
            String serverTypePath = toAvailableServerTypePath(url);
            List<String> currentChilds = zkClient.subscribeChildChanges(serverTypePath, zkChildListener);
            LoggerUtil.info(String.format("[ZookeeperRegistry] subscribe: path=%s, info=%s", toServerNodePath(url, serverTypePath), url.toFullStr()));
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
                    zkClient.unsubscribeChildChanges(toClientTypePath(url), zkChildListener);
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
            String parentPath = toAvailableServerTypePath(url);
            List<String> currentChilds = zkClient.getChildren(parentPath);
            return nodeChildsToUrls(parentPath, currentChilds);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to discover %s from zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                setAvailable(u);
            }
        } else {
            setAvailable(url);
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                setUnavailable(u);
            }
        } else {
            setUnavailable(url);
        }
    }

    private void setAvailable(URL url) {
        doUnregister(url);
        String serverTypePath = toAvailableServerTypePath(url);
        createServerNode(url, serverTypePath);
        LoggerUtil.info(String.format("[ZookeeperRegistry] set available: path=%s", toServerNodePath(url, serverTypePath)));
    }

    private void setUnavailable(URL url) {
        doUnregister(url);
        String serverTypePath = toUnavailableServerTypePath(url);
        createServerNode(url, serverTypePath);
        LoggerUtil.info(String.format("[ZookeeperRegistry] set unavailable: path=%s", toServerNodePath(url, serverTypePath)));
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

    private void createServerNode(URL url, String serverTypePath) {
        if (!zkClient.exists(serverTypePath)) {
            zkClient.createPersistent(serverTypePath, true);
        }
        zkClient.createEphemeral(toServerNodePath(url, serverTypePath), url.toFullStr());
    }

    private void createClientNode(URL url, String clientTypePath) {
        if (!zkClient.exists(clientTypePath)) {
            zkClient.createPersistent(clientTypePath, true);
        }
        zkClient.createEphemeral(toClientNodePath(url), url.toFullStr());
    }

    private void deleteNode(String path) {
        if (zkClient.exists(path)) {
            zkClient.delete(path);
        }
    }

    private String toGroupPath(URL url) {
        return MotanConstants.ZOOKEEPER_REGISTRY_NAMESPACE + MotanConstants.PATH_SEPARATOR + url.getGroup();
    }

    private String toServicePath(URL url) {
        return toGroupPath(url) + MotanConstants.PATH_SEPARATOR + url.getPath();
    }

    private String toNodeTypePath(URL url, String nodeType) {
        return toServicePath(url) + MotanConstants.PATH_SEPARATOR + nodeType;
    }

    private String toServerNodePath(URL url, String ServerTypePath) {
        return ServerTypePath + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private String toClientNodePath(URL url) {
        return toClientTypePath(url) + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private String toUnavailableServerTypePath(URL url) {
        return toNodeTypePath(url, "unavailbleServer");
    }

    private String toAvailableServerTypePath(URL url) {
        return toNodeTypePath(url, "server");
    }

    private String toClientTypePath(URL url) {
        return toNodeTypePath(url, "client");
    }
}
