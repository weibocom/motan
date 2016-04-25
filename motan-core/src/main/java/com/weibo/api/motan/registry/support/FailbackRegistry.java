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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;

/**
 * 
 * Failback registry
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */

public abstract class FailbackRegistry extends AbstractRegistry {

    private Set<URL> failedRegistered = new ConcurrentHashSet<URL>();
    private Set<URL> failedUnregistered = new ConcurrentHashSet<URL>();
    private ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedSubscribed =
            new ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>>();
    private ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedUnsubscribed =
            new ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>>();

    private static ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1);

    public FailbackRegistry(URL url) {
        super(url);
        long retryPeriod = url.getIntParameter(URLParamType.registryRetryPeriod.getName(), URLParamType.registryRetryPeriod.getIntValue());
        retryExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    retry();
                } catch (Exception e) {
                    LoggerUtil.warn(String.format("[%s] False when retry in failback registry", registryClassName), e);
                }

            }
        }, retryPeriod, retryPeriod, TimeUnit.MILLISECONDS);
    }

    @Override
    public void register(URL url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);

        try {
            super.register(url);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new MotanFrameworkException(String.format("[%s] false to registery %s to %s", registryClassName, url, getUrl()), e);
            }
            failedRegistered.add(url);
        }
    }

    @Override
    public void unregister(URL url) {
        failedRegistered.remove(url);
        failedUnregistered.remove(url);

        try {
            super.unregister(url);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new MotanFrameworkException(String.format("[%s] false to unregistery %s to %s", registryClassName, url, getUrl()), e);
            }
            failedUnregistered.add(url);
        }
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.subscribe(url, listener);
        } catch (Exception e) {
            List<URL> cachedUrls = getCachedUrls(url);
            if (cachedUrls != null && cachedUrls.size() > 0) {
                listener.notify(getUrl(), cachedUrls);
            } else if (isCheckingUrls(getUrl(), url)) {
                LoggerUtil.warn(String.format("[%s] false to subscribe %s from %s", registryClassName, url, getUrl()), e);
                throw new MotanFrameworkException(String.format("[%s] false to subscribe %s from %s", registryClassName, url, getUrl()), e);
            }
            addToFailedMap(failedSubscribed, url, listener);
        }
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        removeForFailedSubAndUnsub(url, listener);

        try {
            super.unsubscribe(url, listener);
        } catch (Exception e) {
            if (isCheckingUrls(getUrl(), url)) {
                throw new MotanFrameworkException(String.format("[%s] false to unsubscribe %s from %s", registryClassName, url, getUrl()),
                        e);
            }
            addToFailedMap(failedUnsubscribed, url, listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<URL> discover(URL url) {
        try {
            return super.discover(url);
        } catch (Exception e) {
            // 如果discover失败，返回一个empty list吧，毕竟是个下行动作，
            LoggerUtil.warn(String.format("Failed to discover url:%s in registry (%s)", url, getUrl()), e);
            return Collections.EMPTY_LIST;
        }
    }

    private boolean isCheckingUrls(URL... urls) {
        for (URL url : urls) {
            if (!Boolean.parseBoolean(url.getParameter(URLParamType.check.getName(), URLParamType.check.getValue()))) {
                return false;
            }
        }
        return true;
    }

    private void removeForFailedSubAndUnsub(URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedSubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
        listeners = failedUnsubscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    private void addToFailedMap(ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedMap, URL url, NotifyListener listener) {
        Set<NotifyListener> listeners = failedMap.get(url);
        if (listeners == null) {
            failedMap.putIfAbsent(url, new ConcurrentHashSet<NotifyListener>());
            listeners = failedMap.get(url);
        }
        listeners.add(listener);
    }

    private void retry() {
        if (!failedRegistered.isEmpty()) {
            Set<URL> failed = new HashSet<URL>(failedRegistered);
            LoggerUtil.info("[{}] Retry register {}", registryClassName, failed);
            try {
                for (URL url : failed) {
                    super.register(url);
                    failedRegistered.remove(url);
                }
            } catch (Exception e) {
                LoggerUtil.warn(String.format("[%s] Failed to retry register, retry later, failedRegistered.size=%s, cause=%s",
                        registryClassName, failedRegistered.size(), e.getMessage()), e);
            }

        }
        if (!failedUnregistered.isEmpty()) {
            Set<URL> failed = new HashSet<URL>(failedUnregistered);
            LoggerUtil.info("[{}] Retry unregister {}", registryClassName, failed);
            try {
                for (URL url : failed) {
                    super.unregister(url);
                    failedUnregistered.remove(url);
                }
            } catch (Exception e) {
                LoggerUtil.warn(String.format("[%s] Failed to retry unregister, retry later, failedUnregistered.size=%s, cause=%s",
                        registryClassName, failedUnregistered.size(), e.getMessage()), e);
            }

        }
        if (!failedSubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedSubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                LoggerUtil.info("[{}] Retry subscribe {}", registryClassName, failed);
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.subscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    LoggerUtil.warn(String.format("[%s] Failed to retry subscribe, retry later, failedSubscribed.size=%s, cause=%s",
                            registryClassName, failedSubscribed.size(), e.getMessage()), e);
                }
            }
        }
        if (!failedUnsubscribed.isEmpty()) {
            Map<URL, Set<NotifyListener>> failed = new HashMap<URL, Set<NotifyListener>>(failedUnsubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<URL, Set<NotifyListener>>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().size() == 0) {
                    failed.remove(entry.getKey());
                }
            }
            if (failed.size() > 0) {
                LoggerUtil.info("[{}] Retry unsubscribe {}", registryClassName, failed);
                try {
                    for (Map.Entry<URL, Set<NotifyListener>> entry : failed.entrySet()) {
                        URL url = entry.getKey();
                        Set<NotifyListener> listeners = entry.getValue();
                        for (NotifyListener listener : listeners) {
                            super.unsubscribe(url, listener);
                            listeners.remove(listener);
                        }
                    }
                } catch (Exception e) {
                    LoggerUtil.warn(String.format("[%s] Failed to retry unsubscribe, retry later, failedUnsubscribed.size=%s, cause=%s",
                            registryClassName, failedUnsubscribed.size(), e.getMessage()), e);
                }
            }
        }

    }

}
