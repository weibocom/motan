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
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.support.FailbackRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZookeeperRegistry extends FailbackRegistry {

    private ZkClient zkClient;

    private ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> urlListeners = new ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>>();

    public ZookeeperRegistry(URL url) {
        super(url);

        try {
            zkClient = new ZkClient(url.getParameter("address"));
        } catch (ZkException e) {
            LoggerUtil.error("[ZookeeperRegistry] fail to connect zookeeper, cause: " + e.getMessage());
        }
    }


    /**
     * Unit Test中使用
     */
    public ZookeeperRegistry(URL url, ZkClient client) {
        super(url);
        zkClient = client;
    }

    public ConcurrentHashMap<URL, ConcurrentHashMap<NotifyListener, IZkChildListener>> getUrlListeners() {
        return urlListeners;
    }

    @Override
    protected void doRegister(URL url) {
        String info = url.toFullStr();
        String serverTypePath = toUnavaliableServerTypePath(url);
        if (!zkClient.exists(serverTypePath)) {
            zkClient.createPersistent(serverTypePath, true);
        }
        String avaliabeServerNodePath = toAvaliableServerNodePath(url);
        if (zkClient.exists(avaliabeServerNodePath)) {
            // 防止旧节点未正常注销
            LoggerUtil.info(String.format("[ZookeeperRegistry] register: node exists, will be delete, path=%s", avaliabeServerNodePath));
            zkClient.delete(avaliabeServerNodePath);

        }
        String unavaliabeServerNodePath = toUnavaliableServerNodePath(url);
        if (zkClient.exists(unavaliabeServerNodePath)) {
            // 防止旧节点未正常注销
            LoggerUtil.info(String.format("[ZookeeperRegistry] register: node exists, will be delete, path=%s", unavaliabeServerNodePath));
            zkClient.delete(unavaliabeServerNodePath);
        }

        zkClient.createEphemeral(unavaliabeServerNodePath, info);

        LoggerUtil.info(String.format("[ZookeeperRegistry] register: path=%s, info=%s", unavaliabeServerNodePath, info));
    }

    @Override
    protected void doUnregister(URL url) {
        String avaliableNodePath = toAvaliableServerNodePath(url);
        if (zkClient.exists(avaliableNodePath)) {
            zkClient.delete(avaliableNodePath);
            LoggerUtil.info(String.format("[ZookeeperRegistry] unregister: path=%s", avaliableNodePath));
        }
        String unavaliableNodePath = toUnavaliableServerNodePath(url);
        if (zkClient.exists(unavaliableNodePath)) {
            zkClient.delete(unavaliableNodePath);
            LoggerUtil.info(String.format("[ZookeeperRegistry] unregister: path=%s", unavaliableNodePath));
        }
    }

    @Override
    protected void doSubscribe(final URL url, final NotifyListener notifyListener) {
        ConcurrentHashMap<NotifyListener, IZkChildListener> childChangeListeners = urlListeners.get(url);
        if (childChangeListeners == null) {
            urlListeners.putIfAbsent(url, new ConcurrentHashMap<NotifyListener, IZkChildListener>());
            childChangeListeners = urlListeners.get(url);
        }
        IZkChildListener zkChildListener = childChangeListeners.get(notifyListener);
        if (zkChildListener == null) {
            childChangeListeners.putIfAbsent(notifyListener, new IZkChildListener() {
                @Override
                public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                    ZookeeperRegistry.this.notify(url, notifyListener, nodeChildsToUrls(parentPath, currentChilds));
                    LoggerUtil.info(String.format("[ZookeeperRegistry] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                }
            });
            zkChildListener = childChangeListeners.get(notifyListener);
        }

        // 写入Client结点
        String info = url.toFullStr();
        String clientTypePath = toClientTypePath(url);
        if (!zkClient.exists(clientTypePath)) {
            zkClient.createPersistent(clientTypePath, true);
        }
        String clientNodePath = toClientNodePath(url);
        if (zkClient.exists(clientNodePath)) {
            // 防止旧节点未正常注销
            LoggerUtil.info(String.format("[ZookeeperRegistry] register: node exists, will be delete, path=%s", clientNodePath));
            zkClient.delete(clientNodePath);
        }

        zkClient.createEphemeral(clientNodePath, info);

        // 获取当前可用server
        List<String> currentChilds = zkClient.subscribeChildChanges(toAvaliableServerTypePath(url), zkChildListener);
        LoggerUtil.info(String.format("[ZookeeperRegistry] subscribe: path=%s, info=%s", toAvaliableServerNodePath(url), info));
        notify(url, notifyListener, nodeChildsToUrls(toAvaliableServerTypePath(url), currentChilds));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener notifyListener) {
        Map<NotifyListener, IZkChildListener> childChangeListeners = urlListeners.get(url);
        if (childChangeListeners != null) {
            IZkChildListener zkChildListener = childChangeListeners.get(notifyListener);
            if (zkChildListener != null) {
                zkClient.unsubscribeChildChanges(toClientTypePath(url), zkChildListener);
                childChangeListeners.remove(notifyListener);
            }
        }
    }

    @Override
    protected List<URL> doDiscover(URL url) {
        return nodeChildsToUrls(toAvaliableServerTypePath(url));
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                String info = u.toFullStr();

                String unavaliabeServerNodePath = toUnavaliableServerNodePath(u);
                if (zkClient.exists(unavaliabeServerNodePath)) {
                    zkClient.delete(unavaliabeServerNodePath);
                } else {
                    LoggerUtil.warn(String.format("[ZookeeperRegistry] set available: no unavaliabe node exist, path=%s", unavaliabeServerNodePath));
                }

                String avaliableServerNodePath = toAvaliableServerNodePath(u);
                if (zkClient.exists(avaliableServerNodePath)) {
                    // 防止旧节点未正常注销
                    LoggerUtil.info(String.format("[ZookeeperRegistry] register: node exists, will be delete, path=%s", avaliableServerNodePath));
                    zkClient.delete(avaliableServerNodePath);
                }
                zkClient.createEphemeral(avaliableServerNodePath, info);

                LoggerUtil.info(String.format("[ZookeeperRegistry] set avaliable: path=%s", avaliableServerNodePath));
            }
        } else {
            throw new UnsupportedOperationException("consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            for (URL u : getRegisteredServiceUrls()) {
                String info = u.toFullStr();

                String avaliableServerNodePath = toAvaliableServerNodePath(u);
                if (zkClient.exists(avaliableServerNodePath)) {
                    zkClient.delete(avaliableServerNodePath);
                } else {
                    LoggerUtil.warn(String.format("[ZookeeperRegistry] set unavailable: no avaliabe node exist, path=%s", avaliableServerNodePath));
                }

                String unavaliableServerNodePath = toUnavaliableServerNodePath(u);
                if (zkClient.exists(unavaliableServerNodePath)) {
                    // 防止旧节点未正常注销
                    LoggerUtil.info(String.format("[ZookeeperRegistry] register: node exists, will be delete, path=%s", unavaliableServerNodePath));
                    zkClient.delete(unavaliableServerNodePath);
                }
                zkClient.createEphemeral(unavaliableServerNodePath, info);

                LoggerUtil.info(String.format("[ZookeeperRegistry] set unavaliable: path=%s", unavaliableServerNodePath));
            }
        } else {
            throw new UnsupportedOperationException("consul registry not support available by urls yet");
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

    private List<URL> nodeChildsToUrls(String parentPath) {
        List<String> currentChilds = zkClient.getChildren(parentPath);
        return nodeChildsToUrls(parentPath, currentChilds);
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

    private String toUnavaliableServerNodePath(URL url) {
        return toNodeTypePath(url, "unavalibleServer") + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private String toAvaliableServerNodePath(URL url) {
        return toNodeTypePath(url, "server") + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private String toClientNodePath(URL url) {
        return toNodeTypePath(url, "client") + MotanConstants.PATH_SEPARATOR + url.getServerPortStr();
    }

    private String toUnavaliableServerTypePath(URL url) {
        return toNodeTypePath(url, "unavalibleServer");
    }

    private String toAvaliableServerTypePath(URL url) {
        return toNodeTypePath(url, "server");
    }

    private String toClientTypePath(URL url) {
        return toNodeTypePath(url, "client");
    }
}
