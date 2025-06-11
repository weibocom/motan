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
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * loadbalance
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-22
 */

public abstract class AbstractLoadBalance<T> implements LoadBalance<T> {
    public static final int MAX_REFERER_COUNT = 10;

    protected URL clusterUrl;
    private List<Referer<T>> referers;

    @Override
    public void init(URL clusterUrl) {
        this.clusterUrl = clusterUrl;
    }

    @Override
    public void onRefresh(List<Referer<T>> referers) {
        onRefresh(referers, true);
    }

    protected void onRefresh(List<Referer<T>> referers, boolean shuffle) {
        if (shuffle && !referers.isEmpty()) {
            Collections.shuffle(referers);
        }
        // replaced only
        this.referers = referers;
    }

    @Override
    public Referer<T> select(Request request) {
        List<Referer<T>> referers = this.referers;
        if (CollectionUtil.isEmpty(referers)) {
            throw new MotanServiceException(this.getClass().getSimpleName() + " No available referers for call request:" + request);
        }
        Referer<T> ref = null;
        if (referers.size() > 1) {
            ref = doSelect(request);

        } else if (referers.size() == 1) {
            ref = referers.get(0).isAvailable() ? referers.get(0) : null;
        }

        if (ref != null) {
            return ref;
        }
        throw new MotanServiceException(this.getClass().getSimpleName() + " No available referers for call request:" + request);
    }

    @Override
    public void selectToHolder(Request request, List<Referer<T>> refersHolder) {
        List<Referer<T>> referers = this.referers;
        if (CollectionUtil.isEmpty(referers)) {
            throw new MotanServiceException(this.getClass().getSimpleName() + " No available referers for call : referers_size= 0 "
                    + MotanFrameworkUtil.toString(request));
        }

        if (referers.size() > 1) {
            doSelectToHolder(request, refersHolder);

        } else if (referers.size() == 1 && referers.get(0).isAvailable()) {
            refersHolder.add(referers.get(0));
        }
        if (refersHolder.isEmpty()) {
            throw new MotanServiceException(this.getClass().getSimpleName() + " No available referers for call : referers_size="
                    + referers.size() + " " + MotanFrameworkUtil.toString(request));
        }
    }

    protected List<Referer<T>> getReferers() {
        return referers;
    }

    protected Referer<T> selectFromRandomStart(List<Referer<T>> referers) {
        int size = referers.size();
        if (size == 0) {
            return null;
        }
        int index = ThreadLocalRandom.current().nextInt(size);
        Referer<T> ref;
        for (int i = 0; i < size; i++) {
            ref = referers.get((i + index) % size);
            if (ref.isAvailable()) {
                return ref;
            }
        }
        return null;
    }

    protected void addToSelectHolderFromStart(List<Referer<T>> referers, List<Referer<T>> refersHolder, int start) {
        addToSelectHolderFromStart(referers, refersHolder, start, null);
    }

    private void addToSelectHolderFromStart(List<Referer<T>> referers, List<Referer<T>> refersHolder, int start, Referer<T> exclude) {
        int size = referers.size();
        if (size == 0) {
            return;
        }
        int maxSize = MAX_REFERER_COUNT;
        if (exclude != null) {
            maxSize--;
        }
        for (int i = 0, count = 0; i < size && count < maxSize; i++) {
            Referer<T> referer = referers.get((i + start) % size);
            if (referer.isAvailable() && referer != exclude) {
                refersHolder.add(referer);
                count++;
            }
        }
    }


    @Override
    public void setWeightString(String weightString) {
        LoggerUtil.info("ignore weightString:" + weightString);
    }

    protected abstract Referer<T> doSelect(Request request);

    protected void doSelectToHolder(Request request, List<Referer<T>> refersHolder) {
        Referer<T> referer = doSelect(request);
        if (referer != null) {
            refersHolder.add(referer);
        }
        List<Referer<T>> referers = getReferers();
        addToSelectHolderFromStart(referers, refersHolder, ThreadLocalRandom.current().nextInt(referers.size()), referer);
    }
}
