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

package com.weibo.api.motan.cluster.ha;

import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Failover ha strategy.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-21
 */
@SpiMeta(name = "failover")
public class FailoverHaStrategy<T> extends AbstractHaStrategy<T> {

    protected ThreadLocal<List<Referer<T>>> referersHolder = ThreadLocal.withInitial(ArrayList::new);

    @Override
    public Response call(Request request, LoadBalance<T> loadBalance) {
        if (loadBalance.canSelectMulti()) {
            return callWithMultiReferer(request, loadBalance);
        } else {
            return callWithSingleReferer(request, loadBalance);
        }
    }

    protected Response callWithSingleReferer(Request request, LoadBalance<T> loadBalance) {
        int tryCount = getTryCount(request);
        for (int i = 0; i <= tryCount; i++) {
            Referer<T> referer = loadBalance.select(request);
            if (referer == null) {
                throw new MotanServiceException(String.format("FailoverHaStrategy No referers for request:%s, load balance:%s", request,
                        loadBalance));
            }
            try {
                request.setRetries(i);
                return referer.call(request);
            } catch (RuntimeException e) {
                // For business exceptions, throw them directly
                if (ExceptionUtil.isBizException(e)) {
                    throw e;
                } else if (i >= tryCount) {
                    throw e;
                }
                LoggerUtil.warn(String.format("FailoverHaStrategy Call false for request:%s error=%s", request, e.getMessage()));
            }
        }
        throw new MotanFrameworkException("FailoverHaStrategy.call should not come here!");
    }

    // select multi referers at one time
    protected Response callWithMultiReferer(Request request, LoadBalance<T> loadBalance) {
        List<Referer<T>> referers = selectReferers(request, loadBalance);
        if (referers.isEmpty()) {
            throw new MotanServiceException(String.format("FailoverHaStrategy No referers for request:%s, loadbalance:%s", request,
                    loadBalance));
        }
        int tryCount = getTryCount(request);
        for (int i = 0; i <= tryCount; i++) {
            Referer<T> refer = referers.get(i % referers.size());
            try {
                request.setRetries(i);
                return refer.call(request);
            } catch (RuntimeException e) {
                // For business exceptions, throw them directly
                if (ExceptionUtil.isBizException(e)) {
                    throw e;
                } else if (i >= tryCount) {
                    throw e;
                }
                LoggerUtil.warn(String.format("FailoverHaStrategy Call false for request:%s error=%s", request, e.getMessage()));
            }
        }
        throw new MotanFrameworkException("FailoverHaStrategy.call should not come here!");
    }

    protected int getTryCount(Request request) {
        int tryCount =
                url.getMethodParameter(request.getMethodName(), request.getParamtersDesc(), URLParamType.retries.getName(),
                        URLParamType.retries.getIntValue());
        // If it is a negative number, not retry
        if (tryCount < 0) {
            tryCount = 0;
        }
        return tryCount;
    }

    protected List<Referer<T>> selectReferers(Request request, LoadBalance<T> loadBalance) {
        List<Referer<T>> referers = referersHolder.get();
        referers.clear();
        loadBalance.selectToHolder(request, referers);
        return referers;
    }

}
