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
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.*;
import org.apache.commons.lang3.StringUtils;

/**
 * <pre>
 * Access log filter
 *
 * 统计整个call的执行状况，尽量到最上层，最后执行.
 * 此filter会对性能产生一定影响，请求量较大时建议关闭。
 *
 * </pre>
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-22
 */
@SpiMeta(name = "access")
@Activation(sequence = 100, key = {MotanConstants.NODE_TYPE_SERVICE, MotanConstants.NODE_TYPE_REFERER})
public class AccessLogFilter implements Filter {

    public static final String ACCESS_LOG_SWITCHER_NAME = "feature.motan.filter.accessLog";
    public static final String PRINT_TRACE_LOG_SWITCHER_NAME = "feature.motan.printTraceLog.enable";
    private String side;
    private Boolean accessLog;

    static {
        // init global switcher
        MotanSwitcherUtil.initSwitcher(ACCESS_LOG_SWITCHER_NAME, false);
        MotanSwitcherUtil.initSwitcher(PRINT_TRACE_LOG_SWITCHER_NAME, true);
    }

    @Override
    public Response filter(Caller<?> caller, Request request) {
        if (accessLog == null) {
            accessLog = caller.getUrl().getBooleanParameter(URLParamType.accessLog.getName(), URLParamType.accessLog.getBooleanValue());
        }
        if (accessLog || needLog(request)) {
            long start = System.currentTimeMillis();
            boolean success = false;
            Response response = null;
            try {
                response = caller.call(request);
                if (response != null && response.getException() == null) {
                    success = true;
                }
                return response;
            } finally {
                processFinalLog(caller, request, response, start, success);
            }
        } else {
            return caller.call(request);
        }
    }

    private void processFinalLog(final Caller<?> caller, final Request request, final Response response, final long start, final boolean success) {
        long wholeTime = System.currentTimeMillis() - start;
        long segmentTime = wholeTime; // 分段耗时。server侧是内部业务耗时，client侧时server整体耗时+网络接收耗时

        if (request instanceof Traceable && response instanceof Traceable) { // 可以取得细分时间点
            if (caller instanceof Provider) { // server end
                if (response instanceof Callbackable) {// 因server侧完整耗时包括response发送时间，需要通过callback机制异步记录日志。
                    long finalSegmentTime = segmentTime;
                    ((Callbackable) response).addFinishCallback(() -> {
                        long responseSend = ((Traceable) response).getTraceableContext().getSendTime();
                        long requestReceive = ((Traceable) request).getTraceableContext().getReceiveTime();
                        long finalWholeTime = responseSend - requestReceive;
                        logAccess(caller, request, response, finalSegmentTime, finalWholeTime, success);
                    }, null);
                    return;
                }
            } else { // client end
                long requestSend = ((Traceable) request).getTraceableContext().getSendTime();
                long responseReceive = ((Traceable) response).getTraceableContext().getReceiveTime();
                segmentTime = responseReceive - requestSend;
            }
        }
        logAccess(caller, request, response, segmentTime, wholeTime, success); // 同步记录access日志
    }

    // 除了access log配置外，其他需要动态打印access的情况
    private boolean needLog(Request request) {
        if (MotanSwitcherUtil.isOpen(ACCESS_LOG_SWITCHER_NAME)) {
            return true;
        }

        // check trace log
        if (!MotanSwitcherUtil.isOpen(PRINT_TRACE_LOG_SWITCHER_NAME)) {
            return false;
        }
        return "true".equalsIgnoreCase(request.getAttachments().get(MotanConstants.ATT_PRINT_TRACE_LOG));
    }

    private void logAccess(Caller<?> caller, Request request, Response response, long segmentTime, long wholeTime, boolean success) {
        if (getSide() == null) {
            String side = caller instanceof Provider ? MotanConstants.NODE_TYPE_SERVICE : MotanConstants.NODE_TYPE_REFERER;
            setSide(side);
        }

        StringBuilder builder = new StringBuilder(128);
        append(builder, side);
        append(builder, caller.getUrl().getParameter(URLParamType.application.getName()));
        append(builder, caller.getUrl().getParameter(URLParamType.module.getName()));
        append(builder, NetUtils.getLocalAddress().getHostAddress());
        append(builder, request.getInterfaceName());
        append(builder, request.getMethodName());
        append(builder, request.getParamtersDesc());
        // 对于client，url中的remote ip, application, module,referer 和 service获取的地方不同
        if (MotanConstants.NODE_TYPE_REFERER.equals(side)) {
            append(builder, caller.getUrl().getHost());
            append(builder, caller.getUrl().getParameter(URLParamType.application.getName()));
            append(builder, caller.getUrl().getParameter(URLParamType.module.getName()));
        } else {
            append(builder, request.getAttachments().get(URLParamType.host.getName()));
            append(builder, request.getAttachments().get(URLParamType.application.getName()));
            append(builder, request.getAttachments().get(URLParamType.module.getName()));
        }

        append(builder, success);
        String requestId = request.getAttachments().get(URLParamType.requestIdFromClient.getName());
        if (StringUtils.isBlank(requestId)) {
            requestId = String.valueOf(request.getRequestId());
        }
        append(builder, requestId);
        append(builder, request.getAttachments().get(MotanConstants.CONTENT_LENGTH));
        append(builder, response == null ? "0" : response.getAttachments().get(MotanConstants.CONTENT_LENGTH));
        append(builder, segmentTime);
        append(builder, wholeTime);

        LoggerUtil.accessLog(builder.substring(0, builder.length() - 1));
    }

    private void append(StringBuilder builder, Object field) {
        if (field != null) {
            builder.append(StringTools.urlEncode(field.toString()));
        }
        builder.append(MotanConstants.SEPERATOR_ACCESS_LOG);
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }


}
