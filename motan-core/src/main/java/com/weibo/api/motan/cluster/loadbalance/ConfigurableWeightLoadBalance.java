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

package com.weibo.api.motan.cluster.loadbalance;

import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;

import java.util.List;

/**
 * 权重可配置的负载均衡器
 *
 * @author chengya1
 */
@Deprecated
// Group weights are handled uniformly by GroupWeightLoadBalance.
// For version compatibility, RandomLoadBalance is used internally
@SpiMeta(name = "configurableWeight")
public class ConfigurableWeightLoadBalance<T> extends ActiveWeightLoadBalance<T> {
    LoadBalance<T> loadBalance = new RandomLoadBalance<>();

    @Override
    public void onRefresh(List<Referer<T>> referers) {
        super.onRefresh(referers);
        loadBalance.onRefresh(referers);
    }

    @Override
    protected Referer<T> doSelect(Request request) {
        return loadBalance.select(request);
    }

    @Override
    protected void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        loadBalance.selectToHolder(request, refersHolder);
    }

}
