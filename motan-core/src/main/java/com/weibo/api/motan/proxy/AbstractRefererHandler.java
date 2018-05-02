package com.weibo.api.motan.proxy;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.serialize.DeserializableObject;
import com.weibo.api.motan.switcher.Switcher;
import com.weibo.api.motan.switcher.SwitcherService;
import com.weibo.api.motan.util.ExceptionUtil;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author sunnights
 */
public class AbstractRefererHandler<T> {
    protected List<Cluster<T>> clusters;
    protected Class<T> clz;
    protected SwitcherService switcherService = null;
    protected String interfaceName;

    void init() {
        // clusters 不应该为空
        String switchName = this.clusters.get(0).getUrl().getParameter(URLParamType.switcherService.getName(), URLParamType.switcherService.getValue());
        switcherService = ExtensionLoader.getExtensionLoader(SwitcherService.class).getExtension(switchName);
    }

    Object invokeRequest(Request request, Class returnType, boolean async) throws Throwable {
        RpcContext curContext = RpcContext.getContext();
        curContext.putAttribute(MotanConstants.ASYNC_SUFFIX, async);

        // set rpc context attachments to request
        Map<String, String> attachments = curContext.getRpcAttachments();
        if (!attachments.isEmpty()) {
            for (Map.Entry<String, String> entry : attachments.entrySet()) {
                request.setAttachment(entry.getKey(), entry.getValue());
            }
        }

        // add to attachment if client request id is set
        if (StringUtils.isNotBlank(curContext.getClientRequestId())) {
            request.setAttachment(URLParamType.requestIdFromClient.getName(), curContext.getClientRequestId());
        }

        // 当 referer配置多个protocol的时候，比如A,B,C，
        // 那么正常情况下只会使用A，如果A被开关降级，那么就会使用B，B也被降级，那么会使用C
        for (Cluster<T> cluster : clusters) {
            String protocolSwitcher = MotanConstants.PROTOCOL_SWITCHER_PREFIX + cluster.getUrl().getProtocol();

            Switcher switcher = switcherService.getSwitcher(protocolSwitcher);

            if (switcher != null && !switcher.isOn()) {
                continue;
            }

            request.setAttachment(URLParamType.version.getName(), cluster.getUrl().getVersion());
            request.setAttachment(URLParamType.clientGroup.getName(), cluster.getUrl().getGroup());
            // 带上client的application和module
            request.setAttachment(URLParamType.application.getName(), cluster.getUrl().getApplication());
            request.setAttachment(URLParamType.module.getName(), cluster.getUrl().getModule());

            Response response;
            boolean throwException = Boolean.parseBoolean(cluster.getUrl().getParameter(URLParamType.throwException.getName(), URLParamType.throwException.getValue()));
            try {
                response = cluster.call(request);
                if (async) {
                    if (response instanceof ResponseFuture) {
                        ((ResponseFuture) response).setReturnType(returnType);
                        return response;
                    } else {
                        ResponseFuture responseFuture = new DefaultResponseFuture(request, 0, cluster.getUrl());
                        if (response.getException() != null) {
                            responseFuture.onFailure(response);
                        } else {
                            responseFuture.onSuccess(response);
                        }
                        responseFuture.setReturnType(returnType);
                        return responseFuture;
                    }
                } else {
                    Object value = response.getValue();
                    if (value != null && value instanceof DeserializableObject) {
                        try {
                            value = ((DeserializableObject) value).deserialize(returnType);
                        } catch (IOException e) {
                            LoggerUtil.error("deserialize response value fail! deserialize type:" + returnType, e);
                            throw new MotanFrameworkException("deserialize return value fail! deserialize type:" + returnType, e);
                        }
                    }
                    return value;
                }
            } catch (RuntimeException e) {
                if (ExceptionUtil.isBizException(e)) {
                    Throwable t = e.getCause();
                    // 只抛出Exception，防止抛出远程的Error
                    if (t != null && t instanceof Exception) {
                        throw t;
                    } else {
                        String msg = t == null ? "biz exception cause is null. origin error msg : " + e.getMessage() : ("biz exception cause is throwable error:" + t.getClass() + ", errmsg:" + t.getMessage());
                        throw new MotanServiceException(msg, MotanErrorMsgConstant.SERVICE_DEFAULT_ERROR);
                    }
                } else if (!throwException) {
                    LoggerUtil.warn("RefererInvocationHandler invoke false, so return default value: uri=" + cluster.getUrl().getUri() + " " + MotanFrameworkUtil.toString(request), e);
                    return getDefaultReturnValue(returnType);
                } else {
                    LoggerUtil.error("RefererInvocationHandler invoke Error: uri=" + cluster.getUrl().getUri() + " " + MotanFrameworkUtil.toString(request), e);
                    throw e;
                }
            }
        }
        throw new MotanServiceException("Referer call Error: cluster not exist, interface=" + interfaceName + " " + MotanFrameworkUtil.toString(request), MotanErrorMsgConstant.SERVICE_UNFOUND);
    }

    private Object getDefaultReturnValue(Class<?> returnType) {
        if (returnType != null && returnType.isPrimitive()) {
            return PrimitiveDefault.getDefaultReturnValue(returnType);
        }
        return null;
    }

    private static class PrimitiveDefault {
        private static boolean defaultBoolean;
        private static char defaultChar;
        private static byte defaultByte;
        private static short defaultShort;
        private static int defaultInt;
        private static long defaultLong;
        private static float defaultFloat;
        private static double defaultDouble;

        private static Map<Class<?>, Object> primitiveValues = new HashMap<Class<?>, Object>();

        static {
            primitiveValues.put(boolean.class, defaultBoolean);
            primitiveValues.put(char.class, defaultChar);
            primitiveValues.put(byte.class, defaultByte);
            primitiveValues.put(short.class, defaultShort);
            primitiveValues.put(int.class, defaultInt);
            primitiveValues.put(long.class, defaultLong);
            primitiveValues.put(float.class, defaultFloat);
            primitiveValues.put(double.class, defaultDouble);
        }

        public static Object getDefaultReturnValue(Class<?> returnType) {
            return primitiveValues.get(returnType);
        }

    }
}
