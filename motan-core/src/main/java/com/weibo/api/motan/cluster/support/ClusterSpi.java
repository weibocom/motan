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

package com.weibo.api.motan.cluster.support;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.cluster.HaStrategy;
import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cluster spi.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-21
 */
@SpiMeta(name = "default")
public class ClusterSpi<T> implements Cluster<T> {

    private HaStrategy<T> haStrategy;

    private LoadBalance<T> loadBalance;

    private List<Referer<T>> referers;

    private AtomicBoolean available = new AtomicBoolean(false);

    private URL url;

    @Override
    public void init() {
        onRefresh(referers);
        available.set(true);
    }

    @Override
    public Class<T> getInterface() {
        if (referers == null || referers.isEmpty()) {
            return null;
        }

        return referers.get(0).getInterface();
    }

    @Override
    public Response call(Request request) {
        if (available.get()) {
            try {
                return haStrategy.call(request, loadBalance);
            } catch (Exception e) {
                return callFalse(request, e);
            }
        }
        throw new MotanServiceException(String.format("ClusterSpi Call false for request: %s, ClusterSpi not created or destroyed", request),
                MotanErrorMsgConstant.SERVICE_UNFOUND, false);
    }

    @Override
    public String desc() {
        return toString();
    }

    @Override
    public void destroy() {
        available.set(false);
        for (Referer<T> referer : this.referers) {
            referer.destroy();
        }
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public String toString() {
        return "cluster: {" + "ha=" + haStrategy + ",loadBalance=" + loadBalance +
                "referers=" + referers + "}";

    }

    @Override
    public synchronized void onRefresh(List<Referer<T>> referers) {
        if (CollectionUtil.isEmpty(referers)) {
            return;
        }

        loadBalance.onRefresh(referers);
        List<Referer<T>> oldReferers = this.referers;
        this.referers = referers;
        haStrategy.setUrl(getUrl());

        if (oldReferers == null || oldReferers.isEmpty()) {
            return;
        }

        List<Referer<T>> delayDestroyReferers = new ArrayList<>();

        for (Referer<T> referer : oldReferers) {
            if (referers.contains(referer)) {
                continue;
            }

            delayDestroyReferers.add(referer);
        }

        if (!delayDestroyReferers.isEmpty()) {
            RefererSupports.delayDestroy(delayDestroyReferers);
        }
    }

    public AtomicBoolean getAvailable() {
        return available;
    }

    public void setAvailable(AtomicBoolean available) {
        this.available = available;
    }

    public HaStrategy<T> getHaStrategy() {
        return haStrategy;
    }

    @Override
    public void setHaStrategy(HaStrategy<T> haStrategy) {
        this.haStrategy = haStrategy;
    }

    @Override
    public LoadBalance<T> getLoadBalance() {
        return loadBalance;
    }

    @Override
    public void setLoadBalance(LoadBalance<T> loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public List<Referer<T>> getReferers() {
        return referers;
    }

    protected Response callFalse(Request request, Exception cause) {

        // biz exception 无论如何都要抛出去
        if (ExceptionUtil.isBizException(cause)) {
            throw (RuntimeException) cause;
        }

        // 其他异常根据配置决定是否抛，如果抛异常，需要统一为MotanException
        if (Boolean.parseBoolean(getUrl().getParameter(URLParamType.throwException.getName(), URLParamType.throwException.getValue()))) {
            if (cause instanceof MotanAbstractException) {
                throw (MotanAbstractException) cause;
            } else {
                throw new MotanServiceException(String.format("ClusterSpi Call false for request: %s", request), cause);
            }
        }

        return MotanFrameworkUtil.buildErrorResponse(request, cause);
    }

    @Override
    public Map<String, Object> getRuntimeInfo() {
        Map<String, Object> infos = new HashMap<>();
        infos.put(RuntimeInfoKeys.URL_KEY, url.toFullStr());
        infos.put(RuntimeInfoKeys.REFERER_SIZE_KEY, referers == null ? 0 : referers.size());
        if (!CollectionUtil.isEmpty(referers)) {
            // common infos
            addCommonInfos(referers.get(0), infos, RuntimeInfoKeys.CODEC_KEY, RuntimeInfoKeys.FUSING_THRESHOLD_KEY);

            // available referers
            Map<String, Object> availableInfos = new HashMap<>();
            Map<String, Object> unavailableInfos = new HashMap<>();
            int i = 0;
            for (Referer<?> referer : referers) {
                if (referer.isAvailable()) {
                    availableInfos.put(i + "-" + referer.getUrl().toTinyString(), removeCommonInfos(referer, RuntimeInfoKeys.CODEC_KEY, RuntimeInfoKeys.FUSING_THRESHOLD_KEY));
                } else {
                    unavailableInfos.put(i + "-" + referer.getUrl().toTinyString(), removeCommonInfos(referer, RuntimeInfoKeys.CODEC_KEY, RuntimeInfoKeys.FUSING_THRESHOLD_KEY));
                }
                i++;
            }
            Map<String, Object> refererInfos = new HashMap<>();
            refererInfos.put(RuntimeInfoKeys.AVAILABLE_KEY, availableInfos);
            refererInfos.put(RuntimeInfoKeys.UNAVAILABLE_KEY, unavailableInfos);
            infos.put(RuntimeInfoKeys.REFERERS_KEY, refererInfos);
        }
        return infos;
    }

    private Map<String, Object> removeCommonInfos(Referer<?> referer, String... keys) {
        Map<String, Object> refererInfos = referer.getRuntimeInfo();
        if (!CollectionUtil.isEmpty(refererInfos)) {
            for (String key : keys) {
                refererInfos.remove(key);
            }
        }
        return refererInfos;
    }

    private void addCommonInfos(Referer<?> referer, Map<String, Object> infos, String... keys) {
        Map<String, Object> refererInfos = referer.getRuntimeInfo();
        if (!CollectionUtil.isEmpty(refererInfos)) {
            for (String key : keys) {
                if (refererInfos.get(key) != null) {
                    infos.put(key, refererInfos.get(key));
                }
            }
        }
    }
}
