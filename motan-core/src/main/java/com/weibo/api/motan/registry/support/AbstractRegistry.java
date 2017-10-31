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

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.switcher.SwitcherListener;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <pre>
 * Abstract registry。
 *
 * 对进出的url都进行createCopy保护，避免registry中的对象被修改，避免潜在的并发问题。
 *
 * </pre>
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */

public abstract class AbstractRegistry implements Registry {

    private ConcurrentHashMap<URL, Map<String, List<URL>>> subscribedCategoryResponses =
            new ConcurrentHashMap<URL, Map<String, List<URL>>>();

    private URL registryUrl;
    private Set<URL> registeredServiceUrls = new ConcurrentHashSet<URL>();
    protected String registryClassName = this.getClass().getSimpleName();

    public AbstractRegistry(URL url) {
        this.registryUrl = url.createCopy();
        // register a heartbeat switcher to perceive service state change and change available state
        MotanSwitcherUtil.initSwitcher(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, false);
        MotanSwitcherUtil.registerSwitcherListener(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, new SwitcherListener() {

            @Override
            public void onValueChanged(String key, Boolean value) {
                if (key != null && value != null) {
                    if (value) {
                        available(null);
                    } else {
                        unavailable(null);
                    }
                }
            }
        });
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            LoggerUtil.warn("[{}] register with malformed param, url is null", registryClassName);
            return;
        }
        LoggerUtil.info("[{}] Url ({}) will register to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        doRegister(removeUnnecessaryParmas(url.createCopy()));
        registeredServiceUrls.add(url);
        // available if heartbeat switcher already open
        if (MotanSwitcherUtil.isOpen(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER)) {
            available(url);
        }
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            LoggerUtil.warn("[{}] unregister with malformed param, url is null", registryClassName);
            return;
        }
        LoggerUtil.info("[{}] Url ({}) will unregister to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        doUnregister(removeUnnecessaryParmas(url.createCopy()));
        registeredServiceUrls.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            LoggerUtil.warn("[{}] subscribe with malformed param, url:{}, listener:{}", registryClassName, url, listener);
            return;
        }
        LoggerUtil.info("[{}] Listener ({}) will subscribe to url ({}) in Registry [{}]", registryClassName, listener, url,
                registryUrl.getIdentity());
        doSubscribe(url.createCopy(), listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            LoggerUtil.warn("[{}] unsubscribe with malformed param, url:{}, listener:{}", registryClassName, url, listener);
            return;
        }
        LoggerUtil.info("[{}] Listener ({}) will unsubscribe from url ({}) in Registry [{}]", registryClassName, listener, url,
                registryUrl.getIdentity());
        doUnsubscribe(url.createCopy(), listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> discover(URL url) {
        if (url == null) {
            LoggerUtil.warn("[{}] discover with malformed param, refUrl is null", registryClassName);
            return Collections.EMPTY_LIST;
        }
        url = url.createCopy();
        List<URL> results = new ArrayList<URL>();

        Map<String, List<URL>> categoryUrls = subscribedCategoryResponses.get(url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<URL> urls : categoryUrls.values()) {
                for (URL tempUrl : urls) {
                    results.add(tempUrl.createCopy());
                }
            }
        } else {
            List<URL> urlsDiscovered = doDiscover(url);
            if (urlsDiscovered != null) {
                for (URL u : urlsDiscovered) {
                    results.add(u.createCopy());
                }
            }
        }
        return results;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    @Override
    public Collection<URL> getRegisteredServiceUrls() {
        return registeredServiceUrls;
    }

    @Override
    public void available(URL url) {
        LoggerUtil.info("[{}] Url ({}) will set to available to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doAvailable(removeUnnecessaryParmas(url.createCopy()));
        } else {
            doAvailable(null);
        }
    }

    @Override
    public void unavailable(URL url) {
        LoggerUtil.info("[{}] Url ({}) will set to unavailable to Registry [{}]", registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doUnavailable(removeUnnecessaryParmas(url.createCopy()));
        } else {
            doUnavailable(null);
        }
    }

    protected List<URL> getCachedUrls(URL url) {
        Map<String, List<URL>> rsUrls = subscribedCategoryResponses.get(url);
        if (rsUrls == null || rsUrls.size() == 0) {
            return null;
        }

        List<URL> urls = new ArrayList<URL>();
        for (List<URL> us : rsUrls.values()) {
            for (URL tempUrl : us) {
                urls.add(tempUrl.createCopy());
            }
        }
        return urls;
    }

    protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
        if (listener == null || urls == null) {
            return;
        }
        Map<String, List<URL>> nodeTypeUrlsInRs = new HashMap<String, List<URL>>();
        for (URL surl : urls) {
            String nodeType = surl.getParameter(URLParamType.nodeType.getName(), URLParamType.nodeType.getValue());
            List<URL> oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            if (oneNodeTypeUrls == null) {
                nodeTypeUrlsInRs.put(nodeType, new ArrayList<URL>());
                oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            }
            oneNodeTypeUrls.add(surl);
        }
        Map<String, List<URL>> curls = subscribedCategoryResponses.get(refUrl);
        if (curls == null) {
            subscribedCategoryResponses.putIfAbsent(refUrl, new ConcurrentHashMap<String, List<URL>>());
            curls = subscribedCategoryResponses.get(refUrl);
        }

        // refresh local urls cache
        for (String nodeType : nodeTypeUrlsInRs.keySet()) {
            curls.put(nodeType, nodeTypeUrlsInRs.get(nodeType));
        }

        for (List<URL> us : nodeTypeUrlsInRs.values()) {
            listener.notify(getUrl(), us);
        }
    }

    /**
     * 移除不必提交到注册中心的参数。这些参数不需要被client端感知。
     *
     * @param url
     */
    private URL removeUnnecessaryParmas(URL url) {
        // codec参数不能提交到注册中心，如果client端没有对应的codec会导致client端不能正常请求。
        url.getParameters().remove(URLParamType.codec.getName());
        return url;
    }

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

    protected abstract List<URL> doDiscover(URL url);

    protected abstract void doAvailable(URL url);

    protected abstract void doUnavailable(URL url);

}
