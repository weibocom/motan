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

package com.weibo.api.motan.runtime;

/**
 * @author zhanglei28
 * @date 2024/2/29.
 */
public class RuntimeInfoKeys {
    // top level keys
    public static final String INSTANCE_TYPE_KEY = "instanceType";
    public static final String REGISTRIES_KEY = "registries";
    public static final String EXPORTERS_KEY = "exporters";
    public static final String CLUSTERS_KEY = "clusters";
    public static final String MESH_CLIENTS_KEY = "meshClients";
    public static final String SERVERS_KEY = "servers";
    public static final String FILTER_KEY = "filters";
    public static final String GLOBAL_CONFIG_KEY = "globalConfigs";

    // common keys
    public static final String URL_KEY = "url";
    public static final String STATE_KEY = "state";

    // registry keys
    public static final String REGISTERED_SERVICE_URLS_KEY = "registeredServiceUrls";
    public static final String SUBSCRIBED_SERVICE_URLS_KEY = "subscribedServiceUrls";
    public static final String FAILED_REGISTER_URLS_KEY = "failedRegisterUrls";
    public static final String FAILED_UNREGISTER_URLS_KEY = "failedUnregisterUrls";
    public static final String FAILED_SUBSCRIBE_URLS_KEY = "failedSubscribeUrls";
    public static final String FAILED_UNSUBSCRIBE_URLS_KEY = "failedUnsubscribeUrls";
    public static final String SUBSCRIBE_INFO_KEY = "subscribeInfo";
    public static final String COMMAND_KEY = "command";
    public static final String STATIC_COMMAND_KEY = "staticCommand";
    public static final String WEIGHT_KEY = "weight";
    public static final String COMMAND_HISTORY_KEY = "commandHistory";
    public static final String NOTIFY_HISTORY_KEY = "notifyHistory";

    // exporter keys
    public static final String PROVIDER_KEY = "provider";
    public static final String SERVER_KEY = "server";
    public static final String IS_ASYNC_KEY = "isAsync";
    public static final String SERVICE_KEY = "service";
    public static final String IMPL_CLASS_KEY = "implClass";

    // server keys
    public static final String CONNECTION_COUNT_KEY = "connectionCount";
    public static final String TASK_COUNT_KEY = "taskCount";
    public static final String METHOD_COUNT_KEY = "methodCount";
    public static final String PROVIDER_SIZE_KEY = "providerSize";
    public static final String PROTECT_STRATEGY_KEY = "protectStrategy";


    // cluster keys
    public static final String REFERERS_KEY = "referers";
    public static final String REFERER_SIZE_KEY = "refererSize";
    public static final String CURRENT_CALL_COUNT_KEY = "currentCallCount";
    public static final String AVAILABLE_KEY = "available";
    public static final String UNAVAILABLE_KEY = "unavailable";

    // client keys
    public static final String CODEC_KEY = "codec";
    public static final String FUSING_THRESHOLD_KEY = "fusingThreshold";
    public static final String ERROR_COUNT_KEY = "errorCount";
    public static final String FORCE_CLOSED_KEY = "forceClosed";

}
