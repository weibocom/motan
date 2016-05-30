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

import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.util.ReflectUtil;

public class Test {

    public static void main(String[] args) {
        String methodName = "testVoid";
        
        Object[] arguments = new Object[]{};
        
        
        Class interfaceClass = Model.class;
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
        
        System.out.println(ReflectUtil.getMethodDesc(targetMethod));
        //TODO 需要特别适配空类型
        //参数适配为java对应类型
        if(arguments != null && arguments.length > 0){
            Object[] adapterArguments = new Object[arguments.length];
           
        }
        
    }

}
