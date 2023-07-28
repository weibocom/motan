/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
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

import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @since 1.2.1
 * @author zhanglei28
 * @date 2023/2/9.
 */
public class MotanClientUtil {

    public static Request buildRequest(String interfaceName, String methodName, Object[] arguments) {
        return buildRequest(interfaceName, methodName, arguments, null);
    }

    public static Request buildRequestV1(String interfaceName, String methodName, Object[] arguments,String parametersDesc) {
        return buildRequest(interfaceName, methodName, parametersDesc,arguments, null);
    }

    public static Request buildRequest(String interfaceName, String methodName, Object[] arguments, Map<String, String> attachments) {
        return buildRequest(interfaceName, methodName, null, arguments, attachments);
    }

    public static Request buildRequest(String interfaceName, String methodName, String paramtersDesc, Object[] arguments, Map<String, String> attachments) {
        DefaultRequest request = new DefaultRequest();
        request.setRequestId(RequestIdGenerator.getRequestId());
        request.setInterfaceName(interfaceName);
        request.setMethodName(methodName);
        request.setArguments(arguments);
        if (StringUtils.isNotEmpty(paramtersDesc)) {
            request.setParamtersDesc(paramtersDesc);
        }
        if (attachments != null) {
            request.setAttachments(attachments);
        }
        return request;
    }
}
