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

import com.weibo.api.motan.closable.Closable;
import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.support.command.CommandFailbackRegistry;
import com.weibo.api.motan.registry.support.command.CommandListener;
import com.weibo.api.motan.registry.support.command.ServiceListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.Watcher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class ZookeeperRegistry extends CommandFailbackRegistry implements Closable {
    private ZkClient zkClient;
    private Set<URL> availableServices = new ConcurrentHashSet<URL>();
    private ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>> serviceListeners = new ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>>();
    private ConcurrentHashMap<URL, ConcurrentHashMap<CommandListener, IZkDataListener>> commandListeners = new ConcurrentHashMap<URL, ConcurrentHashMap<CommandListener, IZkDataListener>>();
    private final ReentrantLock clientLock = new ReentrantLock();
    private final ReentrantLock serverLock = new ReentrantLock();
    
    public ZookeeperRegistry(URL url, ZkClient client) {
        super(url);
        this.zkClient = client;
        IZkStateListener zkStateListener = new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState state) throws Exception {
                // do nothing
            }

            @Override
            public void handleNewSession() throws Exception {
                LoggerUtil.info("zkRegistry get new session notify.");
                reconnectService();
                reconnectClient();
            }
        };
        zkClient.subscribeStateChanges(zkStateListener);
        ShutDownHook.registerShutdownHook(this);
    }

    public ConcurrentHashMap<URL, ConcurrentHashMap<ServiceListener, IZkChildListener>> getServiceListeners() {
        return serviceListeners;
    }

    public ConcurrentHashMap<URL, ConcurrentHashMap<CommandListener, IZkDataListener>> getCommandListeners() {
        return commandListeners;
    }

    @Override
    protected void subscribeService(final URL url, final ServiceListener serviceListener) {
        try {
            clientLock.lock();
            ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners == null) {
                serviceListeners.putIfAbsent(url, new ConcurrentHashMap<ServiceListener, IZkChildListener>());
                childChangeListeners = serviceListeners.get(url);
            }
            IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
            if (zkChildListener == null) {
                childChangeListeners.putIfAbsent(serviceListener, new IZkChildListener() {
                    @Override
                    public void handleChildChange(String parentPath, List<String> currentChilds) {
                        serviceListener.notifyService(url, getUrl(), nodeChildsToUrls(url, parentPath, currentChilds));
                        LoggerUtil.info(String.format("[ZookeeperRegistry] service list change: path=%s, currentChilds=%s", parentPath, currentChilds.toString()));
                    }
                });
                zkChildListener = childChangeListeners.get(serviceListener);
            }

            // 防止旧节点未正常注销
            removeNode(url, ZkNodeType.CLIENT);
            createNode(url, ZkNodeType.CLIENT);

            String serverTypePath = ZkUtils.toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            zkClient.subscribeChildChanges(serverTypePath, zkChildListener);
            LoggerUtil.info(String.format("[ZookeeperRegistry] subscribe service: path=%s, info=%s", ZkUtils.toNodePath(url, ZkNodeType.AVAILABLE_SERVER), url.toFullStr()));
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void subscribeCommand(final URL url, final CommandListener commandListener) {
        try {
            clientLock.lock();
            ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners == null) {
                commandListeners.putIfAbsent(url, new ConcurrentHashMap<CommandListener, IZkDataListener>());
                dataChangeListeners = commandListeners.get(url);
            }
            IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
            if (zkDataListener == null) {
                dataChangeListeners.putIfAbsent(commandListener, new IZkDataListener() {
                    @Override
                    public void handleDataChange(String dataPath, Object data) throws Exception {
                        commandListener.notifyCommand(url, (String) data);
                        LoggerUtil.info(String.format("[ZookeeperRegistry] command data change: path=%s, command=%s", dataPath, (String) data));
                    }

                    @Override
                    public void handleDataDeleted(String dataPath) throws Exception {
                        commandListener.notifyCommand(url, null);
                        LoggerUtil.info(String.format("[ZookeeperRegistry] command deleted: path=%s", dataPath));
                    }
                });
                zkDataListener = dataChangeListeners.get(commandListener);
            }

            String commandPath = ZkUtils.toCommandPath(url);
            zkClient.subscribeDataChanges(commandPath, zkDataListener);
            LoggerUtil.info(String.format("[ZookeeperRegistry] subscribe command: path=%s, info=%s", commandPath, url.toFullStr()));
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to subscribe %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeService(URL url, ServiceListener serviceListener) {
        try {
            clientLock.lock();
            Map<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
            if (childChangeListeners != null) {
                IZkChildListener zkChildListener = childChangeListeners.get(serviceListener);
                if (zkChildListener != null) {
                    zkClient.unsubscribeChildChanges(ZkUtils.toNodeTypePath(url, ZkNodeType.CLIENT), zkChildListener);
                    childChangeListeners.remove(serviceListener);
                }
            }
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to unsubscribe service %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected void unsubscribeCommand(URL url, CommandListener commandListener) {
        try {
            clientLock.lock();
            Map<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
            if (dataChangeListeners != null) {
                IZkDataListener zkDataListener = dataChangeListeners.get(commandListener);
                if (zkDataListener != null) {
                    zkClient.unsubscribeDataChanges(ZkUtils.toCommandPath(url), zkDataListener);
                    dataChangeListeners.remove(commandListener);
                }
            }
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to unsubscribe command %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            clientLock.unlock();
        }
    }

    @Override
    protected List<URL> discoverService(URL url) {
        try {
            String parentPath = ZkUtils.toNodeTypePath(url, ZkNodeType.AVAILABLE_SERVER);
            List<String> currentChilds = new ArrayList<String>();
            if (zkClient.exists(parentPath)) {
                currentChilds = zkClient.getChildren(parentPath);
            }
            return nodeChildsToUrls(url, parentPath, currentChilds);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to discover service %s from zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        }
    }

    @Override
    protected String discoverCommand(URL url) {
        try {
            String commandPath = ZkUtils.toCommandPath(url);
            String command = "";
            if (zkClient.exists(commandPath)) {
                command = zkClient.readData(commandPath);
            }
            return command;
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to discover command %s from zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()));
        }
    }

    @Override
    protected void doRegister(URL url) {
        try {
            serverLock.lock();
            // 防止旧节点未正常注销
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to register %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doUnregister(URL url) {
        try {
            serverLock.lock();
            removeNode(url, ZkNodeType.AVAILABLE_SERVER);
            removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
        } catch (Throwable e) {
            throw new MotanFrameworkException(String.format("Failed to unregister %s to zookeeper(%s), cause: %s", url, getUrl(), e.getMessage()), e);
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doAvailable(URL url) {
        try{
            serverLock.lock();
            if (url == null) {
                availableServices.addAll(getRegisteredServiceUrls());
                for (URL u : getRegisteredServiceUrls()) {
                    removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                    removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                    createNode(u, ZkNodeType.AVAILABLE_SERVER);
                }
            } else {
                availableServices.add(url);
                removeNode(url, ZkNodeType.AVAILABLE_SERVER);
                removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(url, ZkNodeType.AVAILABLE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        try{
            serverLock.lock();
            if (url == null) {
                availableServices.removeAll(getRegisteredServiceUrls());
                for (URL u : getRegisteredServiceUrls()) {
                    removeNode(u, ZkNodeType.AVAILABLE_SERVER);
                    removeNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                    createNode(u, ZkNodeType.UNAVAILABLE_SERVER);
                }
            } else {
                availableServices.remove(url);
                removeNode(url, ZkNodeType.AVAILABLE_SERVER);
                removeNode(url, ZkNodeType.UNAVAILABLE_SERVER);
                createNode(url, ZkNodeType.UNAVAILABLE_SERVER);
            }
        } finally {
            serverLock.unlock();
        }
    }

    private List<URL> nodeChildsToUrls(URL url, String parentPath, List<String> currentChilds) {
        List<URL> urls = new ArrayList<URL>();
        if (currentChilds != null) {
            for (String node : currentChilds) {
                String nodePath = parentPath + MotanConstants.PATH_SEPARATOR + node;
                String data = null;
                try{
                    data = zkClient.readData(nodePath, true);
                } catch (Exception e){
                    LoggerUtil.warn("get zkdata fail!" + e.getMessage());
                }
                URL newurl = null;
                if (StringUtils.isNotBlank(data)) {
                    try {
                        newurl = URL.valueOf(data);
                    } catch (Exception e) {
                        LoggerUtil.warn(String.format("Found malformed urls from ZookeeperRegistry, path=%s", nodePath), e);
                    }
                }
                if (newurl == null) {
                    newurl = url.createCopy();
                    String host = "";
                    int port = 80;
                    if (node.indexOf(":") > -1) {
                        String[] hp = node.split(":");
                        if (hp.length > 1) {
                            host = hp[0];
                            try {
                                port = Integer.parseInt(hp[1]);
                            } catch (Exception ignore) {
                            }
                        }
                    } else {
                        host = node;
                    }
                    newurl.setHost(host);
                    newurl.setPort(port);
                }
                urls.add(newurl);
            }
        }
        return urls;
    }

    private void createNode(URL url, ZkNodeType nodeType) {
        String nodeTypePath = ZkUtils.toNodeTypePath(url, nodeType);
        if (!zkClient.exists(nodeTypePath)) {
            zkClient.createPersistent(nodeTypePath, true);
        }
        zkClient.createEphemeral(ZkUtils.toNodePath(url, nodeType), url.toFullStr());
    }

    private void removeNode(URL url, ZkNodeType nodeType) {
        String nodePath = ZkUtils.toNodePath(url, nodeType);
        if (zkClient.exists(nodePath)) {
            zkClient.delete(nodePath);
        }
    }
    
    private void reconnectService() {
        Collection<URL> allRegisteredServices = getRegisteredServiceUrls();
        if (allRegisteredServices != null && !allRegisteredServices.isEmpty()) {
            try {
                serverLock.lock();
                for (URL url : getRegisteredServiceUrls()) {
                    doRegister(url);
                }
                LoggerUtil.info("[{}] reconnect: register services {}", registryClassName, allRegisteredServices);

                for (URL url : availableServices) {
                    if (!getRegisteredServiceUrls().contains(url)) {
                        LoggerUtil.warn("reconnect url not register. url:{}", url);
                        continue;
                    }
                    doAvailable(url);
                }
                LoggerUtil.info("[{}] reconnect: available services {}", registryClassName, availableServices);
            } finally {
                serverLock.unlock();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private void reconnectClient() {
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            try {
                clientLock.lock();
                for (Map.Entry entry : serviceListeners.entrySet()) {
                    URL url = (URL) entry.getKey();
                    ConcurrentHashMap<ServiceListener, IZkChildListener> childChangeListeners = serviceListeners.get(url);
                    if (childChangeListeners != null) {
                        for (Map.Entry e : childChangeListeners.entrySet()) {
                            subscribeService(url, (ServiceListener) e.getKey());
                        }
                    }
                }
                for (Map.Entry entry : commandListeners.entrySet()) {
                    URL url = (URL) entry.getKey();
                    ConcurrentHashMap<CommandListener, IZkDataListener> dataChangeListeners = commandListeners.get(url);
                    if (dataChangeListeners != null) {
                        for (Map.Entry e : dataChangeListeners.entrySet()) {
                            subscribeCommand(url, (CommandListener) e.getKey());
                        }
                    }
                }
                LoggerUtil.info("[{}] reconnect all clients", registryClassName);
            } finally {
                clientLock.unlock();
            }
        }
    }

    @Override
    public void close() {
        this.zkClient.close();
    }
}
