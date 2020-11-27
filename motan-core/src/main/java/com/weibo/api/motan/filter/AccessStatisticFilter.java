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

package com.weibo.api.motan.filter;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.StatsUtil;
import com.weibo.api.motan.util.StatsUtil.AccessStatus;

import static com.weibo.api.motan.common.MotanConstants.APPLICATION_STATISTIC;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 */
@SpiMeta(name = "statistic")
public class AccessStatisticFilter implements Filter {
    private static final String RPC_SERVICE = "rpc_service";

    @Override
    public Response filter(final Caller<?> caller, final Request request) {
        long start = System.currentTimeMillis();
        AccessStatus accessStatus = AccessStatus.NORMAL;
        final long bizProcessTime;
        Response response = null;

        try {
            response = caller.call(request);
            if (response != null && response.getException() != null) {
                if (ExceptionUtil.isBizException(response.getException())) {
                    accessStatus = AccessStatus.BIZ_EXCEPTION;
                } else {
                    accessStatus = AccessStatus.OTHER_EXCEPTION;
                }
            }
            return response;
        } finally {
            long end = System.currentTimeMillis();

            if (response == null) {
                accessStatus = AccessStatus.OTHER_EXCEPTION;
                bizProcessTime = end - start;
            } else {
                bizProcessTime = response.getProcessTime();
            }

            final String statName = caller.getUrl().getProtocol() + MotanConstants.PROTOCOL_SEPARATOR + MotanFrameworkUtil.getGroupMethodString(request);
            final int slowCost = caller.getUrl().getIntParameter(URLParamType.slowThreshold.getName(), URLParamType.slowThreshold.getIntValue());
            final Response finalResponse = response;
            if (caller instanceof Provider) {
                StatsUtil.accessStatistic(statName, APPLICATION_STATISTIC, RPC_SERVICE, end, end - start, bizProcessTime, slowCost, accessStatus);
                if (response instanceof Callbackable) {
                    final AccessStatus finalAccessStatus = accessStatus;
                    ((Callbackable) response).addFinishCallback(new Runnable() {
                        @Override
                        public void run() {
                            if (request instanceof Traceable && finalResponse instanceof Traceable) {
                                long responseSend = ((Traceable) finalResponse).getTraceableContext().getSendTime();
                                long requestReceive = ((Traceable) request).getTraceableContext().getReceiveTime();
                                StatsUtil.accessStatistic(statName + "_WHOLE", caller.getUrl().getApplication(), caller.getUrl().getModule(), responseSend, responseSend - requestReceive, bizProcessTime, slowCost, finalAccessStatus);
                            }
                        }
                    }, null);
                }
            }
            StatsUtil.accessStatistic(statName, caller.getUrl().getApplication(), caller.getUrl().getModule(), end, end - start, bizProcessTime, slowCost, accessStatus);
        }
    }
}
