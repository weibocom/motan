/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.mock;

import com.weibo.api.motan.cluster.loadbalance.AbstractWeightedLoadBalance;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.MetaUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhanglei28
 * @date 2024/3/21.
 */
public class MockDynamicReferer<T> extends MockReferer<T> {
    private static final Response DEFAULT_RESPONSE = new DefaultResponse();
    private static final String WEIGHT_KEY = MetaUtil.ENV_META_PREFIX + AbstractWeightedLoadBalance.WEIGHT_META_SUFFIX_KEY;

    public int id;
    public AtomicInteger count = new AtomicInteger(0); // call count
    public int staticWeight = 0;
    public int dynamicWeight = 0;
    public ConcurrentHashMap<String, String> dynamicMeta = new ConcurrentHashMap<>();

    public Response response = DEFAULT_RESPONSE;

    public MockDynamicReferer(URL url, int id) {
        this.url = url;
        this.serviceUrl = url.createCopy();
        this.id = id;
    }

    public MockDynamicReferer(URL url, int id, int staticWeight) {
        this(url, id);
        this.staticWeight = staticWeight;
        this.url.addParameter(WEIGHT_KEY, String.valueOf(staticWeight));
    }

    @Override
    public Response call(Request request) {
        if (isMetaServiceRequest(request)) {
            return new DefaultResponse(dynamicMeta);
        }
        count.incrementAndGet();
        return response;
    }

    public void setWeight(boolean isDynamic, int weight) {
        if (isDynamic) {
            this.dynamicWeight = weight;
            dynamicMeta.put(WEIGHT_KEY, String.valueOf(weight));
        } else {
            this.staticWeight = weight;
            this.url.addParameter(WEIGHT_KEY, String.valueOf(weight));
        }
    }

    public void clearWeight(boolean isDynamic) {
        if (isDynamic) {
            this.dynamicWeight = 0;
            dynamicMeta.remove(WEIGHT_KEY);
        } else {
            this.staticWeight = 0;
            this.url.removeParameter(WEIGHT_KEY);
        }
    }

    private boolean isMetaServiceRequest(Request request) {
        return request != null
                && MetaUtil.SERVICE_NAME.equals(request.getInterfaceName())
                && MetaUtil.METHOD_NAME.equals(request.getMethodName())
                && "y".equals(request.getAttachment(MotanConstants.FRAMEWORK_SERVICE));
    }
}
