/*
 * Copyright 2009-2016 Weibo, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.weibo.api.motan.protocol.support;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.ActivationComparator;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.filter.InitializableFilter;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.StringTools;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

import static com.weibo.api.motan.common.MotanConstants.DISABLE_FILTER_PREFIX;

/**
 * Decorate the protocol, to add more features.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-30
 */

public class ProtocolFilterDecorator implements Protocol {

    private Protocol protocol;

    public ProtocolFilterDecorator(Protocol protocol) {
        if (protocol == null) {
            throw new MotanFrameworkException("Protocol is null when construct ProtocolFilterDecorator",
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
        this.protocol = protocol;
    }

    @Override
    public <T> Exporter<T> export(Provider<T> provider, URL url) {
        return protocol.export(decorateWithFilter(provider, url), url);
    }

    @Override
    public <T> Referer<T> refer(Class<T> clz, URL url, URL serviceUrl) {
        return decorateWithFilter(protocol.refer(clz, url, serviceUrl), url);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

    public <T> Referer<T> decorateRefererFilter(Referer<T> referer, URL url) {
        return decorateWithFilter(referer, url);
    }

    private <T> Referer<T> decorateWithFilter(Referer<T> referer, URL url) {
        List<Filter> filters = getFilters(url, MotanConstants.NODE_TYPE_REFERER);
        Referer<T> lastRef = referer;
        for (Filter filter : filters) {
            final Filter f = filter;
            if (f instanceof InitializableFilter) {
                ((InitializableFilter) f).init(lastRef);
            }
            final Referer<T> lf = lastRef;
            lastRef = new Referer<T>() {
                @Override
                public Response call(Request request) {
                    Activation activation = f.getClass().getAnnotation(Activation.class);
                    if (activation != null && !activation.retry() && request.getRetries() != 0) {
                        return lf.call(request);
                    }
                    return f.filter(lf, request);
                }

                @Override
                public String desc() {
                    return lf.desc();
                }

                @Override
                public void destroy() {
                    lf.destroy();
                }

                @Override
                public Class<T> getInterface() {
                    return lf.getInterface();
                }

                @Override
                public URL getUrl() {
                    return lf.getUrl();
                }

                @Override
                public void init() {
                    lf.init();
                }

                @Override
                public boolean isAvailable() {
                    return lf.isAvailable();
                }

                @Override
                public int activeRefererCount() {
                    return lf.activeRefererCount();
                }


                @Override
                public URL getServiceUrl() {
                    return lf.getServiceUrl();
                }
            };
        }
        return lastRef;
    }

    private <T> Provider<T> decorateWithFilter(final Provider<T> provider, URL url) {
        List<Filter> filters = getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        if (filters == null || filters.size() == 0) {
            return provider;
        }
        Provider<T> lastProvider = provider;
        for (Filter filter : filters) {
            final Filter f = filter;
            if (f instanceof InitializableFilter) {
                ((InitializableFilter) f).init(lastProvider);
            }
            final Provider<T> lp = lastProvider;
            lastProvider = new Provider<T>() {
                @Override
                public Response call(Request request) {
                    return f.filter(lp, request);
                }

                @Override
                public String desc() {
                    return lp.desc();
                }

                @Override
                public void destroy() {
                    lp.destroy();
                }

                @Override
                public Class<T> getInterface() {
                    return lp.getInterface();
                }

                @Override
                public Method lookupMethod(String methodName, String methodDesc) {
                    return lp.lookupMethod(methodName, methodDesc);
                }

                @Override
                public URL getUrl() {
                    return lp.getUrl();
                }

                @Override
                public void init() {
                    lp.init();
                }

                @Override
                public boolean isAvailable() {
                    return lp.isAvailable();
                }

                @Override
                public T getImpl() {
                    return provider.getImpl();
                }
            };
        }
        return lastProvider;
    }

    /**
     * <pre>
     * 获取方式：
     * 1）先获取默认的filter列表；
     * 2）根据filter配置获取新的filters，并和默认的filter列表合并；
     * 3）如果filter配置中有'-'开头的filter name，表示disable某个filter，对应的filter不会装配
     * </pre>
     *
     * @param url
     * @param key
     * @return
     */
    protected List<Filter> getFilters(URL url, String key) {
        // load default filters
        List<Filter> filters = new ArrayList<>();
        List<Filter> defaultFilters = ExtensionLoader.getExtensionLoader(Filter.class).getExtensions(key);
        if (defaultFilters != null && defaultFilters.size() > 0) {
            filters.addAll(defaultFilters);
        }

        // add filters via "filter" config
        String filterStr = url.getParameter(URLParamType.filter.getName());
        if (StringUtils.isNotBlank(filterStr)) {
            HashSet<String> removedFilters = new HashSet<>();
            Set<String> filterNames = StringTools.splitSet(filterStr, MotanConstants.COMMA_SEPARATOR);
            for (String fn : filterNames) {
                if (fn.startsWith(DISABLE_FILTER_PREFIX)) { // disable filter
                    if (fn.length() > DISABLE_FILTER_PREFIX.length()) {
                        removedFilters.add(fn.substring(DISABLE_FILTER_PREFIX.length()).trim());
                    }
                } else {
                    Filter extFilter = ExtensionLoader.getExtensionLoader(Filter.class).getExtension(fn, false);
                    if (extFilter == null) {
                        LoggerUtil.warn("filter extension not found. filer name: " + fn);
                        continue;
                    }
                    filters.add(extFilter);
                }
            }

            // remove disabled filters
            if (!removedFilters.isEmpty()) {
                for (String removedName : removedFilters) {
                    filters.removeIf((filter) -> removedName.equals(filter.getClass().getAnnotation(SpiMeta.class).name()));
                }
            }
        }

        // sort the filters
        Collections.sort(filters, new ActivationComparator<>());
        Collections.reverse(filters);
        return filters;
    }
}
