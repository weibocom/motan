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

package com.weibo.api.motan.common;

import com.weibo.api.motan.util.ReflectUtil;

import java.util.regex.Pattern;

/**
 * 类说明
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-28
 */

public class MotanConstants {

    public static final String SEPERATOR_ACCESS_LOG = "|";
    public static final String COMMA_SEPARATOR = ",";
    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");
    public static final String PROTOCOL_SEPARATOR = "://";
    public static final String PATH_SEPARATOR = "/";
    public static final String REGISTRY_SEPARATOR = "|";
    public static final Pattern REGISTRY_SPLIT_PATTERN = Pattern.compile("\\s*[|;]+\\s*");
    public static final String SEMICOLON_SEPARATOR = ";";
    public static final Pattern SEMICOLON_SPLIT_PATTERN = Pattern.compile("\\s*[;]+\\s*");
    public static final String QUERY_PARAM_SEPARATOR = "&";
    public static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("\\s*[&]+\\s*");
    public static final String EQUAL_SIGN_SEPERATOR = "=";
    public static final Pattern EQUAL_SIGN_PATTERN = Pattern.compile("\\s*[=]\\s*");
    public static final String NODE_TYPE_SERVICE = "service";
    public static final String NODE_TYPE_REFERER = "referer";
    public static final String SCOPE_NONE = "none";
    public static final String SCOPE_LOCAL = "local";
    public static final String SCOPE_REMOTE = "remote";
    public static final String REGISTRY_PROTOCOL_LOCAL = "local";
    public static final String REGISTRY_PROTOCOL_DIRECT = "direct";
    public static final String REGISTRY_PROTOCOL_ZOOKEEPER = "zookeeper";
    public static final String REGISTRY_PROTOCOL_PLAIN_ZOOKEEPER = "zk"; // use utf8 string serializer
    public static final String REGISTRY_PROTOCOL_WEIBOMESH = "weibomesh";
    public static final String PROTOCOL_INJVM = "injvm";
    public static final String PROTOCOL_MOTAN = "motan";
    public static final String PROTOCOL_MOTAN2 = "motan2";
    public static final String PROXY_JDK = "jdk";
    public static final String PROXY_COMMON = "common";
    public static final String PROXY_JAVASSIST = "javassist";
    public static final String FRAMEWORK_NAME = "motan";
    public static final String PROTOCOL_SWITCHER_PREFIX = "protocol:";
    public static final String METHOD_CONFIG_PREFIX = "methodconfig.";
    public static final int MILLS = 1;
    public static final int SECOND_MILLS = 1000;
    public static final int MINUTE_MILLS = 60 * SECOND_MILLS;
    public static final String DEFAULT_VALUE = "default";
    public static final int DEFAULT_INT_VALUE = 0;
    public static final String DEFAULT_VERSION = "1.0";
    public static final boolean DEFAULT_THROWS_EXCEPTION = true;
    public static final String DEFAULT_CHARACTER = "utf-8";
    public static final int SLOW_COST = 50; // 50ms
    public static final int STATISTIC_PEROID = 30; // 30 seconds
    public static final int REFRESH_PERIOD = 60;
    public static final String ASYNC_SUFFIX = "Async";// suffix for async call.
    public static final String APPLICATION_STATISTIC = "statisitic";
    public static final String REQUEST_REMOTE_ADDR = "requestRemoteAddress";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String DISABLE_FILTER_PREFIX = "-";
    public static final String SUFFIX_STRING = "suffix:";

    /**
     * netty channel constants start
     **/

    // netty codec
    public static final short NETTY_MAGIC_TYPE = (short) 0xF1F1;
    // netty header length
    public static final int NETTY_HEADER = 16;
    // netty server max excutor thread
    public static final int NETTY_EXECUTOR_MAX_SIZE = 800;
    // netty thread idle time: 1 mintue
    public static final int NETTY_THREAD_KEEPALIVE_TIME = 60 * 1000;
    // netty client max concurrent request
    public static final int NETTY_CLIENT_MAX_REQUEST = 50000;
    // share channel max worker thread
    public static final int NETTY_SHARECHANNEL_MAX_WORKDER = 800;
    // share channel min worker thread
    public static final int NETTY_SHARECHANNEL_MIN_WORKDER = 40;
    // don't share channel max worker thread
    public static final int NETTY_NOT_SHARECHANNEL_MAX_WORKDER = 200;
    // don't share channel min worker thread
    public static final int NETTY_NOT_SHARECHANNEL_MIN_WORKDER = 20;
    public static final int NETTY_TIMEOUT_TIMER_PERIOD = 100;
    public static final byte NETTY_REQUEST_TYPE = 1;
    public static final byte FLAG_REQUEST = 0x00;
    public static final byte FLAG_RESPONSE = 0x01;
    public static final byte FLAG_RESPONSE_VOID = 0x03;
    public static final byte FLAG_RESPONSE_EXCEPTION = 0x05;
    public static final byte FLAG_RESPONSE_ATTACHMENT = 0x07;
    public static final byte FLAG_OTHER = (byte) 0xFF;
    /**
     * heartbeat constants start
     */
    public static final int HEARTBEAT_PERIOD = 500;
    public static final String HEARTBEAT_INTERFACE_NAME = "com.weibo.api.motan.rpc.heartbeat";
    public static final String HEARTBEAT_METHOD_NAME = "heartbeat";
    public static final String HHEARTBEAT_PARAM = ReflectUtil.EMPTY_PARAM;
    /**
     * heartbeat constants end
     */

    public static final String ZOOKEEPER_REGISTRY_NAMESPACE = "/motan";
    public static final String ZOOKEEPER_REGISTRY_COMMAND = "/command";

    public static final String REGISTRY_HEARTBEAT_SWITCHER = "feature.configserver.heartbeat";
    public static final String MOTAN_TRACE_INFO_SWITCHER = "feature.motan.trace.info";

    /**
     * 默认的consistent的hash的数量
     */
    public static final int DEFAULT_CONSISTENT_HASH_BASE_LOOP = 1000;

    // ------------------ motan 2 protocol constants -----------------
    public static final String M2_GROUP = "M_g";
    public static final String M2_VERSION = "M_v";
    public static final String M2_PATH = "M_p";
    public static final String M2_METHOD = "M_m";
    public static final String M2_METHOD_DESC = "M_md";
    public static final String M2_AUTH = "M_a";
    public static final String M2_SOURCE = "M_s";// 调用方来源标识,等同与application
    public static final String M2_MODULE = "M_mdu";
    public static final String M2_PROXY_PROTOCOL = "M_pp";
    public static final String M2_INFO_SIGN = "M_is";
    public static final String M2_ERROR = "M_e";
    public static final String M2_PROCESS_TIME = "M_pt";
    public static final String M2_TIMEOUT = "M_tmo";

    // ------------------ request trace point constants -----------------
    public static final String TRACE_INVOKE = "TRACE_INVOKE"; //client 发起请求
    public static final String TRACE_CONNECTION = "TRACE_CONNECTION"; // client获取链接
    public static final String TRACE_CENCODE = "TRACE_CENCODE"; //client编码
    public static final String TRACE_CSEND = "TRACE_CSEND"; //client发送请求
    public static final String TRACE_SRECEIVE = "TRACE_SRECEIVE"; //server端接收请求
    public static final String TRACE_SDECODE = "TRACE_SDECODE"; //server端解码
    public static final String TRACE_SEXECUTOR_START = "TRACE_SEXECUTOR_START"; //server端线程池开始执行
    public static final String TRACE_BEFORE_BIZ = "TRACE_BEFORE_BIZ"; //server端业务逻辑执行前
    public static final String TRACE_AFTER_BIZ = "TRACE_AFTER_BIZ"; //server端业务逻辑执行后
    public static final String TRACE_PROCESS = "TRACE_PROCESS"; //server端处理完成
    public static final String TRACE_SENCODE = "TRACE_SENCODE"; // server端编码
    public static final String TRACE_SSEND = "TRACE_SSEND";// server端发送response
    public static final String TRACE_CRECEIVE = "TRACE_CRECEIVE";// client端接收response
    public static final String TRACE_CDECODE = "TRACE_CDECODE"; // client端解码response

    // ------------------ attachment constants -----------------
    public static final String ATT_PRINT_TRACE_LOG = "print_trace_log"; // 针对单请求是否打印（access）日志
    public static final String X_FORWARDED_FOR = "x-forwarded-for"; // 经过mesh代理的远端host
    public static final String FRAMEWORK_SERVICE = "M_fws"; // motan framework service attachment key
    public static final String ROUTE_GROUP_KEY = "motan-route-group"; // 请求中控制分组流量的key

    // ------------------ common env name -----------------
    public static final String ENV_ADDITIONAL_GROUP = "MOTAN_SERVICE_ADDITIONAL_GROUP"; //motan service 追加导出分组。例如可以自动追加云平台上的分组
    public static final String ENV_GLOBAL_FILTERS = "MOTAN_GLOBAL_FILTERS"; // add global filters from env
    public static final String ENV_MESH_PROXY = "MOTAN_MESH_PROXY"; //使用mesh代理motan请求的环境变量名
    public static final String ENV_MOTAN_IP_PREFIX = "MOTAN_IP_PREFIX";
    public static final String ENV_MOTAN_LOCAL_IP = "MOTAN_LOCAL_IP"; // specify the local IP address used in motan
    public static final String ENV_MOTAN_ADMIN_TOKEN = "MOTAN_ADMIN_TOKEN";
    public static final String ENV_MOTAN_ADMIN_PORT = "MOTAN_ADMIN_PORT";
    public static final String ENV_MOTAN_ADMIN_EXT_HANDLERS = "MOTAN_ADMIN_EXT_HANDLERS";
    public static final String ENV_RPC_REG_GROUP_SUFFIX = "RPC_REG_GROUP_SUFFIX"; // Group suffix automatically appended during RPC registration
    public static final String ENV_MOTAN_CHANGE_REG_GROUPS = "MOTAN_CHANGE_REG_GROUPS"; // Change registed groups of service by service name.
    public static final String ENV_MOTAN_SERVER_MODE = "MOTAN_SERVER_MODE"; // Set the startup mode of motan server.

    // ------------------ motan server mode -----------------
    public static final String MOTAN_SERVER_MODE_SANDBOX = "sandbox";

    // ------------------ motan mesh default value -----------------
    public static final String MESH_CLIENT = "meshClient";
    public static final int MESH_DEFAULT_PORT = 9981;
    public static final int MESH_DEFAULT_MPORT = 8002;
    public static final String MESH_DEFAULT_HOST = "localhost";

    // admin server
    public static final int DEFAULT_ADMIN_PORT = 9002;
    // admin server config keys
    public static final String ADMIN_DISABLE = "admin.disable";
    public static final String ADMIN_PORT = "admin.port";
    public static final String ADMIN_SERVER = "admin.server";
    public static final String ADMIN_PROTOCOL = "admin.protocol";
    public static final String ADMIN_TOKEN = "admin.token";
    public static final String ADMIN_EXT_HANDLERS = "admin.extHandlers";

    // ===== global config keys =====
    public static final String ENV_META_PREFIX_KEY = "envMetaPrefix";
    public static final String SERVER_END_STRICT_CHECK_GROUP_KEY = "serverEndStrictCheckGroup";
    public static final String META_CACHE_EXPIRE_SECOND_KEY = "metaCacheExpireSecond";
    public static final String WEIGHT_REFRESH_PERIOD_SECOND_KEY = "weightRefreshPeriodSecond";
    public static final String WEIGHT_REFRESH_MAX_THREAD_KEY = "weightRefreshMaxThread";

    private MotanConstants() {
    }

}
