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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.MathUtil;

/**
 * 
 * Use consistent hash to choose referer
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-21
 */
@SpiMeta(name = "consistent")
public class ConsistentHashLoadBalance<T> extends AbstractLoadBalance<T> {

    private List<Referer<T>> consistentHashReferers;

    @Override
    public void onRefresh(List<Referer<T>> referers) {
        super.onRefresh(referers);

        List<Referer<T>> copyReferers = new ArrayList<Referer<T>>(referers);
        List<Referer<T>> tempRefers = new ArrayList<Referer<T>>();
        for (int i = 0; i < MotanConstants.DEFAULT_CONSISTENT_HASH_BASE_LOOP; i++) {
            Collections.shuffle(copyReferers);
            for (Referer<T> ref : copyReferers) {
                tempRefers.add(ref);
            }
        }

        consistentHashReferers = tempRefers;
    }

    @Override
    protected Referer<T> doSelect(Request request) {

        int hash = getHash(request);
        Referer<T> ref;
        for (int i = 0; i < getReferers().size(); i++) {
            ref = consistentHashReferers.get((hash + i) % consistentHashReferers.size());
            if (ref.isAvailable()) {
                return ref;
            }
        }
        return null;
    }

    @Override
    protected void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        List<Referer<T>> referers = getReferers();

        int hash = getHash(request);
        for (int i = 0; i < referers.size(); i++) {
            Referer<T> ref = consistentHashReferers.get((hash + i) % consistentHashReferers.size());
            if (ref.isAvailable()) {
                refersHolder.add(ref);
            }
        }
    }

    private int getHash(Request request) {
        int hashcode;
        if (request.getArguments() == null || request.getArguments().length == 0) {
            hashcode = request.hashCode();
        } else {
            hashcode = Arrays.hashCode(request.getArguments());
        }
        return MathUtil.getNonNegative(hashcode);
    }


}
