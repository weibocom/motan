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

package com.weibo.api.motan.registry.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.ObjectUtils;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * @author maijunsheng
 * @version 创建时间：2013-5-24
 * 
 */
@SpiMeta(name = "local")
public class LocalRegistryService extends AbstractRegistry {

    /** Map<interface/nodeType, List<URL>>, List 中的url用identity/id来区分唯一性 */
    private ConcurrentMap<String, List<URL>> registeredServices = new ConcurrentHashMap<String, List<URL>>();

    private ConcurrentHashMap<String, ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>>> subscribeListeners =
            new ConcurrentHashMap<String, ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>>>();
    private URL registryUrl;

    public LocalRegistryService() {
        this(new URL(MotanConstants.REGISTRY_PROTOCOL_LOCAL, NetUtils.LOCALHOST, MotanConstants.DEFAULT_INT_VALUE,
                RegistryService.class.getName()));
    }

    public LocalRegistryService(URL url) {
        super(url);
        this.registryUrl = url;
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {

        String subscribeKey = getSubscribeKey(url);
        ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> urlListeners = subscribeListeners.get(subscribeKey);
        if (urlListeners == null) {
            subscribeListeners.putIfAbsent(subscribeKey, new ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>>());
            urlListeners = subscribeListeners.get(subscribeKey);
        }

        ConcurrentHashSet<NotifyListener> listeners = urlListeners.get(url);
        if (listeners == null) {
            urlListeners.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = urlListeners.get(url);
        }

        listeners.add(listener);

        List<URL> urls = discover(url);
        if (urls != null && urls.size() > 0) {
            listener.notify(getUrl(), urls);
        }

        LoggerUtil.info("LocalRegistryService subscribe: url={}", url);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {
        String subscribeKey = getSubscribeKey(url);
        ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> urlListeners = subscribeListeners.get(subscribeKey);
        if (urlListeners != null) {
            urlListeners.remove(url);
        }

        LoggerUtil.info("LocalRegistryService unsubscribe: url={}", url);
    }

    @Override
    public List<URL> doDiscover(URL url) {
        return registeredServices.get(getRegistryKey(url));
    }

    @Override
    protected void doAvailable(URL url) {
        //do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        //do nothing
    }

    @Override
    public void doRegister(URL url) {
        String registryKey = getRegistryKey(url);
        synchronized (registeredServices) {
            List<URL> urls = registeredServices.get(registryKey);

            if (urls == null) {
                registeredServices.putIfAbsent(registryKey, new ArrayList<URL>());
                urls = registeredServices.get(registryKey);
            }
            add(url, urls);

            LoggerUtil.info("LocalRegistryService register: url={}", url);

            notifyListeners(url);
        }
    }

    @Override
    public void doUnregister(URL url) {
        synchronized (registeredServices) {
            List<URL> urls = registeredServices.get(getRegistryKey(url));

            if (urls == null) {
                return;
            }

            remove(url, urls);

            LoggerUtil.info("LocalRegistryService unregister: url={}", url);
            // 在变更后立即进行通知
            notifyListeners(url);
        }
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    /**
     * 防止数据在外部被变更，因此copy一份
     * 
     * @return
     */
    public Map<String, List<URL>> getAllUrl() {
        Map<String, List<URL>> copyMap = new HashMap<String, List<URL>>(registeredServices.size());

        for (Map.Entry<String, List<URL>> entry : registeredServices.entrySet()) {
            String key = entry.getKey();

            List<URL> copyList = new ArrayList<URL>(entry.getValue().size());
            for (URL url : entry.getValue()) {
                copyList.add(url.createCopy());
            }

            copyMap.put(key, copyList);
        }

        return copyMap;
    }

    private void remove(URL url, List<URL> urls) {
        if (CollectionUtil.isEmpty(urls)) {
            return;
        }
        removeCachedUrlByIdentity(url, urls);
    }

    private void add(URL url, List<URL> urls) {
        removeCachedUrlByIdentity(url, urls);
        urls.add(url);
    }

    private void removeCachedUrlByIdentity(URL url, List<URL> urls) {
        if (CollectionUtil.isEmpty(urls)) {
            return;
        }
        URL oldUrl = null;
        for (URL cachedUrl : urls) {
            if (ObjectUtils.equals(url, cachedUrl)) {
                oldUrl = cachedUrl;
                break;
            }
        }

        if (oldUrl != null) {
            urls.remove(oldUrl);
        }
    }

    private void notifyListeners(URL changedUrl) {
        List<URL> interestingUrls = discover(changedUrl);
        if (interestingUrls != null) {
            ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> urlListeners = subscribeListeners.get(getSubscribeKey(changedUrl));
            if (urlListeners == null) {
                return;
            }

            for (ConcurrentHashSet<NotifyListener> listeners : urlListeners.values()) {
                for (NotifyListener ln : listeners) {
                    try {
                        ln.notify(getUrl(), interestingUrls);
                    } catch (Exception e) {
                        LoggerUtil.warn(String.format("Exception when notify listerner %s, changedUrl: %s", ln, changedUrl), e);
                    }
                }
            }

        }
    }

    private String getRegistryKey(URL url) {
        String keyPrefix = url.getPath();
        String nodeType = url.getParameter(URLParamType.nodeType.getName());
        if (nodeType != null) {
            return keyPrefix + MotanConstants.PATH_SEPARATOR + nodeType;
        } else {
            LoggerUtil.warn("Url need a nodeType as param in localRegistry, url={}", url);
            return keyPrefix;
        }
    }

    private String getSubscribeKey(URL url) {
        return getRegistryKey(url);
    }
}
