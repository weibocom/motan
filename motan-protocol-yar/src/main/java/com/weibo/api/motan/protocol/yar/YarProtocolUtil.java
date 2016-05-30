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
package com.weibo.api.motan.protocol.yar;

import java.lang.reflect.Method;

import org.apache.commons.lang3.StringUtils;

import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

public class YarProtocolUtil {
    public static String getYarPath(URL url){
        //TODO 支持path定制，例如"/health" 包名中的点替换
        
        return "/" + url.getGroup() + "/" + url.getPath();
    }
    
    /**
     * 转换yar请求为motan rpc请求。
     * 由于php类型不敏感，故转换请求时只判断方法名和参数个数是否相等。相等时尝试转换为对应类型。
     * @param yarRequest
     * @param interfaceClass
     * @return
     */
    public static Request convert(YarRequest yarRequest, Class<?> interfaceClass){
        //TODO 根据接口兼容
        DefaultRequest request = new DefaultRequest();
        request.setInterfaceName(interfaceClass.getName());
        request.setMethodName(yarRequest.getMethodName());
        request.setRequestId(yarRequest.getId());
        addArguments(request, interfaceClass, yarRequest.getMethodName(), yarRequest.getParameters());
        
        return request;
    }
    
    public static YarRequest convert(Request request, Class<?> interfaceClass, String packagerName){
        YarRequest yarRequest = new YarRequest();
        yarRequest.setId(request.getRequestId());
        yarRequest.setMethodName(request.getMethodName());
        yarRequest.setPackagerName(packagerName);
        yarRequest.setParameters(request.getArguments());
        return yarRequest;
    }
    
    public static Response convert(YarResponse yarResponse){
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(yarResponse.getId());
        //TODO 如果不能明确返回对象，就统一使用jsonobject
//        response.setValue(yarResponse.getValue(???));
        response.setValue(yarResponse.getRet());
        if(StringUtils.isNotBlank(yarResponse.getError())){
            response.setException(new MotanBizException(yarResponse.getError()));
        }
        
        return response;
    }
    
    public static YarResponse convert(Response response, String packagerName){
        YarResponse yarResponse = new YarResponse();
        yarResponse.setId(response.getRequestId());
        yarResponse.setPackagerName(packagerName);
        yarResponse.setRet(response.getValue());
        if(response.getException() != null){
            yarResponse.setError(response.getException().getMessage());
        }
        
        return yarResponse;
    }
    
    /**
     * 给Request添加请求参数相关信息。
     * 由于php类型不敏感，所以只对方法名和参数个数做匹配，然后对参数做兼容处理
     * @param interfaceClass
     * @param methodName
     * @param arguments
     * @return
     */
    private static void addArguments(DefaultRequest request, Class<?> interfaceClass, String methodName, Object[] arguments){
        Method targetMethod = null;
        //TODO 需要考虑每次反射代价，考虑如何缓存
        Method[] methods = interfaceClass.getDeclaredMethods();
        for (Method m : methods) {
            if(m.getName().equalsIgnoreCase(methodName) && m.getParameterTypes().length == arguments.length){
                targetMethod = m;
                break;
            }
        }
        if(targetMethod == null){
            throw new MotanServiceException("cann't find request method. method name " + methodName);
        }
        
        request.setParamtersDesc(ReflectUtil.getMethodParamDesc(targetMethod));
        //TODO 需要特别适配空类型
        //参数适配为java对应类型
        if(arguments != null && arguments.length > 0){
            Object[] adapterArguments = new Object[arguments.length];
            Class<?>[] argumentClazz = targetMethod.getParameterTypes();
//            for (int i = 0; i < argumentClazz.length; i++) {
//                
//            }
//            request.setArguments(adapterArguments);
            request.setArguments(arguments);
        }
        
        
    }
    

}
