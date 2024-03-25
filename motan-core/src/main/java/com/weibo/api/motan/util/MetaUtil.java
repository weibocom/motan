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

package com.weibo.api.motan.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.runtime.meta.MetaService;
import com.weibo.api.motan.serialize.DeserializableObject;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanglei28
 * @date 2024/3/13.
 */
public class MetaUtil {
    public static final String defaultEnvMetaPrefix = "META_";
    // Check whether the default prefix is configured from the global configuration, if not, use the default prefix
    public static final String ENV_META_PREFIX = MotanGlobalConfigUtil.getConfig(MotanConstants.ENV_META_PREFIX_KEY, defaultEnvMetaPrefix);
    public static final String SERVICE_NAME = MetaService.class.getName();
    public static final String METHOD_NAME = "getDynamicMeta";
    private static final Class<?> RETURN_TYPE = Map.class;
    private static int cacheExpireSecond = 3; // default cache expire second
    private static final int notSupportExpireSecond = 30; // not support expire second
    private static final Cache<String, Map<String, String>> metaCache;
    private static final Cache<String, Boolean> notSupportCache;
    private static final Set<String> notSupportSerializer = Sets.newHashSet("protobuf", "grpc-pb", "grpc-pb-json");

    static {
        String expireSecond = MotanGlobalConfigUtil.getConfig(MotanConstants.META_CACHE_EXPIRE_SECOND_KEY);
        if (StringUtils.isNotBlank(expireSecond)) {
            try {
                int tempCacheExpireSecond = Integer.parseInt(expireSecond);
                if (tempCacheExpireSecond > 0) {
                    cacheExpireSecond = tempCacheExpireSecond;
                }
            } catch (Exception ignore) {
            }
        }
        // init caches
        metaCache = CacheBuilder.newBuilder().expireAfterWrite(cacheExpireSecond, TimeUnit.SECONDS).build();
        notSupportCache = CacheBuilder.newBuilder().expireAfterWrite(notSupportExpireSecond, TimeUnit.SECONDS).build();
    }

    // just for GlobalRuntime init envMeta and unit test.
    // to get runtime meta info, use GlobalRuntime.getEnvMeta(), GlobalRuntime.getDynamicMeta(), GlobalRuntime.getMergedMeta()(equivalent to MetaUtil.getLocalMeta)
    public static HashMap<String, String> _getOriginMetaInfoFromEnv() {
        HashMap<String, String> metas = new HashMap<>();
        for (String key : System.getenv().keySet()) {
            if (key.startsWith(MetaUtil.ENV_META_PREFIX)) { // add all the env variables that start with the prefix to the envMeta
                metas.put(key, System.getenv(key));
            }
        }
        return metas;
    }

    // get local meta information
    public static Map<String, String> getLocalMeta() {
        return GlobalRuntime.getMergedMeta();
    }

    // get remote meta information by referer.
    // host level, only contains dynamic meta info.
    // it's a remote RPC call, an exception will be thrown if it fails.
    // if a SERVICE_NOT_SUPPORT_ERROR exception is thrown, should not call this method again.
    public static Map<String, String> getRefererDynamicMeta(Referer<?> referer) throws ExecutionException {
        return metaCache.get(getCacheKey(referer.getUrl()), () -> getRemoteDynamicMeta(referer));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> getRemoteDynamicMeta(Referer<?> referer) throws MotanServiceException, IOException {
        String key = getCacheKey(referer.getUrl());
        // if not support meta service, throws the specified exception.
        if (notSupportCache.getIfPresent(key) != null
                || !isSupport(referer.getUrl())) {
            throw new MotanServiceException(MotanErrorMsgConstant.SERVICE_NOT_SUPPORT_ERROR);
        }
        if (!referer.isAvailable()) {
            throw new MotanServiceException("referer unavailable");
        }
        try {
            Object value = referer.call(buildMetaServiceRequest()).getValue();
            if (value instanceof DeserializableObject) {
                value = ((DeserializableObject) value).deserialize(RETURN_TYPE);
            }
            return (Map<String, String>) value;
        } catch (Exception e) {
            if (e instanceof MotanServiceException) {
                MotanServiceException mse = (MotanServiceException) e;
                if (mse.getStatus() == MotanErrorMsgConstant.SERVICE_NOT_SUPPORT_ERROR.getStatus()
                        || mse.getOriginMessage().contains("provider")) {
                    // provider-related exceptions are considered unsupported
                    notSupportCache.put(key, Boolean.TRUE);
                    throw new MotanServiceException(MotanErrorMsgConstant.SERVICE_NOT_SUPPORT_ERROR);
                }
            }
            throw e;
        }
    }

    private static boolean isSupport(URL url) {
        // check dynamicMeta config, protocol and serializer
        if (url.getBooleanParameter(URLParamType.dynamicMeta.getName(), URLParamType.dynamicMeta.getBooleanValue())
                && !notSupportSerializer.contains(url.getParameter(URLParamType.serialize.getName(), ""))
                && (MotanConstants.PROTOCOL_MOTAN.equals(url.getProtocol()) || MotanConstants.PROTOCOL_MOTAN2.equals(url.getProtocol()))) {
            return true;
        }
        notSupportCache.put(getCacheKey(url), true);
        return false;
    }

    private static String getCacheKey(URL url) {
        return url.getHost() + ":" + url.getPort();
    }

    // get remote static meta information from referer url attachments.
    // the static meta is init at server start from env.
    public static Map<String, String> getRefererStaticMeta(Referer<?> referer) {
        Map<String, String> meta = new HashMap<>();
        referer.getUrl().getParameters().forEach((k, v) -> {
            if (k.startsWith(defaultEnvMetaPrefix)
                    || k.startsWith(ENV_META_PREFIX)) {
                meta.put(k, v);
            }
        });
        return meta;
    }

    public static Request buildMetaServiceRequest() {
        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(SERVICE_NAME);
        request.setMethodName(METHOD_NAME);
        request.setAttachment(MotanConstants.FRAMEWORK_SERVICE, "y");
        return request;
    }

    // get meta value from meta map by keySuffix.
    // Try the environment variable prefix search first, and then use the default prefix search.
    // if not found, return null.
    public static String getMetaValue(Map<String, String> meta, String keySuffix) {
        String value = null;
        if (meta != null) {
            value = meta.get(ENV_META_PREFIX + keySuffix);
            if (value == null) {
                value = meta.get(defaultEnvMetaPrefix + keySuffix);
            }
        }
        return value;
    }

    // only for server end to add meta info to url.
    public static void addStaticMeta(URL url) {
        if (url != null) {
            url.getParameters().putAll(GlobalRuntime.getEnvMeta()); // only add static meta
        }
    }

    public static void clearCache() {
        metaCache.invalidateAll();
        notSupportCache.invalidateAll();
    }
}
