/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.yar;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.yar.annotation.YarConfig;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

/**
 * 
 * @Description yar protocol util.
 * @author zhanglei
 * @date 2016-6-8
 *
 */
public class YarProtocolUtil {

    public static String getYarPath(Class<?> interfaceClazz, URL url) {
        if (interfaceClazz != null) {
            YarConfig config = interfaceClazz.getAnnotation(YarConfig.class);
            if (config != null && StringUtils.isNotBlank(config.path())) {
                return config.path();
            }
        }
        // '/group/urlpath' as default
        return "/" + url.getGroup() + "/" + url.getPath();
    }

    /**
     * convert yar request to motan rpc request
     * 
     * @param yarRequest
     * @param interfaceClass
     * @return
     */
    public static Request convert(YarRequest yarRequest, Class<?> interfaceClass) {
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName(interfaceClass.getName());
        request.setMethodName(yarRequest.getMethodName());
        request.setRequestId(yarRequest.getId());
        addArguments(request, interfaceClass, yarRequest.getMethodName(), yarRequest.getParameters());
        if (yarRequest instanceof AttachmentRequest) {
            request.setAttachments(((AttachmentRequest) yarRequest).getAttachments());
        }
        return request;
    }

    public static YarRequest convert(Request request, String packagerName) {
        YarRequest yarRequest = new YarRequest();
        yarRequest.setId(request.getRequestId());
        yarRequest.setMethodName(request.getMethodName());
        yarRequest.setPackagerName(packagerName);
        yarRequest.setParameters(request.getArguments());
        return yarRequest;
    }

    public static Response convert(YarResponse yarResponse) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(yarResponse.getId());
        response.setValue(yarResponse.getRet());
        if (StringUtils.isNotBlank(yarResponse.getError())) {
            response.setException(new MotanBizException(yarResponse.getError()));
        }

        return response;
    }

    public static YarResponse convert(Response response, String packagerName) {
        YarResponse yarResponse = new YarResponse();
        yarResponse.setId(response.getRequestId());
        yarResponse.setPackagerName(packagerName);
        if (response.getException() != null) {
            if (response.getException() instanceof MotanBizException) {
                yarResponse.setError(response.getException().getCause().getMessage());
            } else {
                yarResponse.setError(response.getException().getMessage());
            }
        } else {
            yarResponse.setRet(response.getValue());
        }

        return yarResponse;
    }

    /**
     * add arguments
     * 
     * @param interfaceClass
     * @param methodName
     * @param arguments
     * @return
     */
    private static void addArguments(DefaultRequest request, Class<?> interfaceClass, String methodName, Object[] arguments) {
        Method targetMethod = null;
        Method[] methods = interfaceClass.getDeclaredMethods();
        for (Method m : methods) {
            // FIXME parameters may be ambiguous in weak type language, temporarily by limiting the
            // size of parameters with same method name to avoid.
            if (m.getName().equalsIgnoreCase(methodName) && m.getParameterTypes().length == arguments.length) {
                targetMethod = m;
                break;
            }
        }
        if (targetMethod == null) {
            throw new MotanServiceException("cann't find request method. method name " + methodName);
        }

        request.setParamtersDesc(ReflectUtil.getMethodParamDesc(targetMethod));

        if (arguments != null && arguments.length > 0) {
            Class<?>[] argumentClazz = targetMethod.getParameterTypes();
            request.setArguments(adaptParams(targetMethod, arguments, argumentClazz));
        }


    }

    public static YarResponse buildDefaultErrorResponse(String errMsg, String packagerName) {
        YarResponse yarResponse = new YarResponse();
        yarResponse.setPackagerName(packagerName);
        yarResponse.setError(errMsg);
        yarResponse.setStatus("500");
        return yarResponse;
    }


    // adapt parameters to java class type
    private static Object[] adaptParams(Method method, Object[] arguments, Class<?>[] argumentClazz) {
        // FIXME the real parameter type may not same with formal parameter, for instance, formal
        // parameter type is int, the real parameter maybe use String in php
        // any elegant way?
        for (int i = 0; i < argumentClazz.length; i++) {
            try {
                if ("int".equals(argumentClazz[i].getName()) || "java.lang.Integer".equals(argumentClazz[i].getName())) {
                    if (arguments[i] == null) {
                        arguments[i] = 0;// default
                    } else if (arguments[i] instanceof String) {
                        arguments[i] = Integer.parseInt((String) arguments[i]);
                    } else if (arguments[i] instanceof Number) {
                        arguments[i] = ((Number) arguments[i]).intValue();
                    } else {
                        throw new RuntimeException();
                    }
                } else if ("long".equals(argumentClazz[i].getName()) || "java.lang.Long".equals(argumentClazz[i].getName())) {
                    if (arguments[i] == null) {
                        arguments[i] = 0;// default
                    } else if (arguments[i] instanceof String) {
                        arguments[i] = Long.parseLong((String) arguments[i]);
                    } else if (arguments[i] instanceof Number) {
                        arguments[i] = ((Number) arguments[i]).longValue();
                    } else {
                        throw new RuntimeException();
                    }
                } else if ("float".equals(argumentClazz[i].getName()) || "java.lang.Float".equals(argumentClazz[i].getName())) {
                    if (arguments[i] == null) {
                        arguments[i] = 0.0f;// default
                    } else if (arguments[i] instanceof String) {
                        arguments[i] = Float.parseFloat((String) arguments[i]);
                    } else if (arguments[i] instanceof Number) {
                        arguments[i] = ((Number) arguments[i]).floatValue();
                    } else {
                        throw new RuntimeException();
                    }
                } else if ("double".equals(argumentClazz[i].getName()) || "java.lang.Double".equals(argumentClazz[i].getName())) {
                    if (arguments[i] == null) {
                        arguments[i] = 0.0f;// default
                    } else if (arguments[i] instanceof String) {
                        arguments[i] = Double.parseDouble((String) arguments[i]);
                    } else if (arguments[i] instanceof Number) {
                        arguments[i] = ((Number) arguments[i]).doubleValue();
                    } else {
                        throw new RuntimeException();
                    }
                } else if ("boolean".equals(argumentClazz[i].getName()) || "java.lang.Boolean".equals(argumentClazz[i].getName())) {
                    if (arguments[i] instanceof Boolean) {
                        continue;
                    }
                    if (arguments[i] instanceof String) {
                        arguments[i] = Boolean.valueOf(((String) arguments[i]));
                    } else {
                        throw new RuntimeException();
                    }
                }
            } catch (Exception e) {
                throw new MotanServiceException("adapt param fail! method:" + method.toString() + ", require param:"
                        + argumentClazz[i].getName() + ", actual param:"
                        + (arguments[i] == null ? null : arguments[i].getClass().getName() + "-" + arguments[i]));
            }
            
        }
        return arguments;
    }


}
