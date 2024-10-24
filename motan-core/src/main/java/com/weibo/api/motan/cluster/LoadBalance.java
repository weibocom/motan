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

package com.weibo.api.motan.cluster;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;

import java.util.List;

/**
 * Loadbalance for select referer
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-16
 */
@Spi(scope = Scope.PROTOTYPE)
public interface LoadBalance<T> {

    void onRefresh(List<Referer<T>> referers);

    Referer<T> select(Request request);

    @Deprecated
    void selectToHolder(Request request, List<Referer<T>> refersHolder);

    void setWeightString(String weightString);

    // Whether to support obtaining multiple referers at one time
    default boolean canSelectMulti() {
        return true;
    }

    default void init(URL clusterUrl) {
    }

    default void destroy() {
    }
}
