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
import com.weibo.api.motan.filter.AccessLogFilter;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.filter.InitializableFilter;
import com.weibo.api.motan.rpc.*;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
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

    public void destroy() {
        protocol.destroy();
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
	 * 3）再根据一些其他配置判断是否需要增加其他filter，如根据accessLog进行判断，是否需要增加accesslog
	 * </pre>
     *
     * @param url
     * @param key
     * @return
     */
    private List<Filter> getFilters(URL url, String key) {

        // load default filters
        List<Filter> filters = new ArrayList<Filter>();
        List<Filter> defaultFilters = ExtensionLoader.getExtensionLoader(Filter.class).getExtensions(key);
        if (defaultFilters != null && defaultFilters.size() > 0) {
            filters.addAll(defaultFilters);
        }

        // add filters via "filter" config
        String filterStr = url.getParameter(URLParamType.filter.getName());
        if (StringUtils.isNotBlank(filterStr)) {
            String[] filterNames = MotanConstants.COMMA_SPLIT_PATTERN.split(filterStr);
            for (String fn : filterNames) {
                addIfAbsent(filters, fn);
            }
        }

        // add filter via other configs, like accessLog and so on
        boolean accessLog = url.getBooleanParameter(URLParamType.accessLog.getName(), URLParamType.accessLog.getBooleanValue());
        if (accessLog) {
            addIfAbsent(filters, AccessLogFilter.class.getAnnotation(SpiMeta.class).name());
        }

        // sort the filters
        Collections.sort(filters, new ActivationComparator<Filter>());
        Collections.reverse(filters);
        return filters;
    }

    private void addIfAbsent(List<Filter> filters, String extensionName) {
        if (StringUtils.isBlank(extensionName)) {
            return;
        }

        Filter extFilter = ExtensionLoader.getExtensionLoader(Filter.class).getExtension(extensionName);
        if (extFilter == null) {
            return;
        }

        boolean exists = false;
        for (Filter f : filters) {
            if (f.getClass() == extFilter.getClass()) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            filters.add(extFilter);
        }

    }
}
