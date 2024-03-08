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

import com.weibo.api.motan.closable.ShutDownHook;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Failback registry
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */

public abstract class FailbackRegistry extends AbstractRegistry {

    private final Set<URL> failedRegistered = new ConcurrentHashSet<>();
    private final Set<URL> failedUnregistered = new ConcurrentHashSet<>();
    private final ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedSubscribed =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<URL, ConcurrentHashSet<NotifyListener>> failedUnsubscribed =
            new ConcurrentHashMap<>();

    private static final ScheduledExecutorService retryExecutor = Executors.newScheduledThreadPool(1);

    static {
        ShutDownHook.registerShutdownHook(() -> {
            if (!retryExecutor.isShutdown()) {
                retryExecutor.shutdown();
            }
        });
    }

    public FailbackRegistry(URL url) {
        super(url);
        long retryPeriod = url.getIntParameter(URLParamType.registryRetryPeriod.getName(), URLParamType.registryRetryPeriod.getIntValue());
        retryExecutor.scheduleAtFixedRate(() -> {
            try {
                retry();
            } catch (Exception e) {
                LoggerUtil.warn(String.format("[%s] False when retry in failback registry", registryClassName), e);
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
                throw new MotanFrameworkException(String.format("[%s] false to registry %s to %s", registryClassName, url, getUrl()), e);
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
                throw new MotanFrameworkException(String.format("[%s] false to unRegistry %s to %s", registryClassName, url, getUrl()), e);
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
            if (cachedUrls != null && !cachedUrls.isEmpty()) {
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
            failedMap.putIfAbsent(url, new ConcurrentHashSet<>());
            listeners = failedMap.get(url);
        }
        listeners.add(listener);
    }

    private void retry() {
        if (!failedRegistered.isEmpty()) {
            Set<URL> failed = new HashSet<>(failedRegistered);
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
            Set<URL> failed = new HashSet<>(failedUnregistered);
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
            Map<URL, Set<NotifyListener>> failed = new HashMap<>(failedSubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    failed.remove(entry.getKey());
                    failedSubscribed.remove(entry.getKey());
                }
            }
            if (!failed.isEmpty()) {
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
            Map<URL, Set<NotifyListener>> failed = new HashMap<>(failedUnsubscribed);
            for (Map.Entry<URL, Set<NotifyListener>> entry : new HashMap<>(failed).entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    failed.remove(entry.getKey());
                    failedUnsubscribed.remove(entry.getKey());
                }
            }
            if (!failed.isEmpty()) {
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

    @Override
    public Map<String, Object> getRuntimeInfo() {
        Map<String, Object> infos = super.getRuntimeInfo();
        // add register failed info
        if (!failedRegistered.isEmpty()) {
            infos.put(RuntimeInfoKeys.FAILED_REGISTER_URLS_KEY, failedRegistered.stream().map(URL::toFullStr).collect(Collectors.toList()));
        }
        // add unregister failed info
        if (!failedUnregistered.isEmpty()) {
            infos.put(RuntimeInfoKeys.FAILED_UNREGISTER_URLS_KEY, failedUnregistered.stream().map(URL::toFullStr).collect(Collectors.toList()));
        }
        // add subscribe failed info
        if (!failedSubscribed.isEmpty()) {
            infos.put(RuntimeInfoKeys.FAILED_SUBSCRIBE_URLS_KEY, failedSubscribed.keySet().stream().map(URL::toFullStr).collect(Collectors.toList()));
        }
        // add unsubscribe failed info
        if (!failedUnsubscribed.isEmpty()) {
            infos.put(RuntimeInfoKeys.FAILED_UNSUBSCRIBE_URLS_KEY, failedUnsubscribed.keySet().stream().map(URL::toFullStr).collect(Collectors.toList()));
        }
        return infos;
    }
}
