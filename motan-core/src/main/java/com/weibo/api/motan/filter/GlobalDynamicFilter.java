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

package com.weibo.api.motan.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * @author zhanglei28
 */
@SpiMeta(name = "globalDynamicFilter")
@Activation(sequence = 25, key = {MotanConstants.NODE_TYPE_SERVICE, MotanConstants.NODE_TYPE_REFERER})
public class GlobalDynamicFilter implements Filter {
    private static volatile ConditionFilter dynamicFilter; // only supports globally unique dynamic filter。

    @Override
    public Response filter(Caller<?> caller, Request request) {
        Filter temp = dynamicFilter;
        if (temp != null) {
            return temp.filter(caller, request);
        }
        return caller.call(request);
    }

    // Allow to set null to remove the filter.
    public static synchronized ConditionFilter setDynamicFilter(ConditionFilter conditionFilter) {
        ConditionFilter old = dynamicFilter;
        dynamicFilter = conditionFilter;
        LoggerUtil.info("[GlobalDynamicFilter] setDynamicFilter: " + (conditionFilter == null ? "null" : conditionFilter.toString()) + ", old: " + (old == null ? "null" : old.toString()));
        return old;
    }

    public static ConditionFilter getDynamicFilter() {
        return dynamicFilter;
    }

    public static class ConditionFilter implements Filter {
        private final Filter innerFilter;
        private final Condition condition;

        private final String filterName;

        private final String conditionString;

        public ConditionFilter(String filterName, String conditionString) {
            if (StringUtils.isBlank(filterName)) {
                throw new MotanServiceException("filter is null");
            }
            Filter filter = ExtensionLoader.getExtensionLoader(Filter.class).getExtension(filterName);
            this.filterName = filterName;
            this.conditionString = conditionString;
            this.innerFilter = filter;
            if (StringUtils.isNotBlank(conditionString)) {
                this.condition = GlobalDynamicFilter.Condition.build(conditionString);
            } else {
                this.condition = null;
            }
        }

        @Override
        public Response filter(Caller<?> caller, Request request) {
            if (condition == null || condition.match(caller, request)) {
                return innerFilter.filter(caller, request);
            }
            return caller.call(request);
        }

        public JSONObject toJson() {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("filterName", filterName);
            jsonObject.put("conditionString", conditionString);
            return jsonObject;
        }

        public String toString() {
            return "[filterName:" + filterName + ", conditionString:" + conditionString + "]";
        }
    }

    public static class Condition {
        public static final String SIDE_SERVER = "server";
        public static final String SIDE_CLIENT = "client";
        private String side; // server/client. when the field is empty, it means no limit
        private String service; // regular expression matching
        private String method; // regular expression matching
        private String ip; // prefix matching

        private Pattern servicePattern;
        private Pattern methodPattern;

        // build condition from json string
        public static Condition build(String jsonString) {
            if (StringUtils.isBlank(jsonString)) {
                return new Condition();
            }
            JSONObject jsonObject = JSON.parseObject(jsonString);
            return new Condition(jsonObject.getString("side"), jsonObject.getString("service"), jsonObject.getString("method"), jsonObject.getString("ip"));
        }

        public Condition() {
        }

        public Condition(String side, String service, String method, String ip) {
            this.side = side;
            this.service = service;
            this.method = method;
            this.ip = ip;
            servicePattern = StringUtils.isBlank(service) ? null : Pattern.compile(service);
            methodPattern = StringUtils.isBlank(method) ? null : Pattern.compile(method);
        }

        // Returns true if the condition is matched, otherwise returns false
        public boolean match(Caller<?> caller, Request request) {
            boolean isServerSide = caller instanceof Provider;
            // Determine whether the conditions are matched based on the four attributes of side, service, method, and ip.
            // If the attribute is empty, the condition is matched by default.
            if (SIDE_SERVER.equals(side) && !isServerSide
                    || SIDE_CLIENT.equals(side) && isServerSide) {
                // The caller does not match the specified side.
                return false;
            }

            // Determine whether the service matches the regular expression
            if (servicePattern != null && !servicePattern.matcher(request.getInterfaceName()).matches()) {
                return false;
            }

            // Determine whether the method matches the regular expression
            if (methodPattern != null && !methodPattern.matcher(request.getMethodName()).matches()) {
                return false;
            }

            if (StringUtils.isNotBlank(ip)) {
                String remoteIp = isServerSide ? MotanFrameworkUtil.getRemoteIpFromRequest(request) : caller.getUrl().getHost();
                if (remoteIp != null && !remoteIp.startsWith(ip)) {
                    return false;
                }
            }

            return true;
        }

        public String getSide() {
            return side;
        }

        public void setSide(String side) {
            this.side = side;
        }

        public String getService() {
            return service;
        }

        public void setService(String service) {
            this.service = service;
            servicePattern = StringUtils.isBlank(service) ? null : Pattern.compile(service);
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
            methodPattern = StringUtils.isBlank(method) ? null : Pattern.compile(method);
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }
    }
}
