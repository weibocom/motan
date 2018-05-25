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

package com.weibo.api.motan.util;

import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Response;
import org.apache.commons.lang3.StringUtils;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;

/**
 * 提供框架内部一些约定处理
 * 
 * @author maijunsheng
 * @version 创建时间：2013-6-4
 * 
 */
public class MotanFrameworkUtil {

    /**
     * 目前根据 group/interface/version 来唯一标示一个服务
     * 
     * @param request
     * @return
     */

    public static String getServiceKey(Request request) {
        String version = getVersionFromRequest(request);
        String group = getGroupFromRequest(request);

        return getServiceKey(group, request.getInterfaceName(), version);
    }

    public static String getGroupFromRequest(Request request) {
        return getValueFromRequest(request, URLParamType.group.name(), URLParamType.group.getValue());
    }

    public static String getVersionFromRequest(Request request) {
        return getValueFromRequest(request, URLParamType.version.name(), URLParamType.version.getValue());
    }

    public static String getValueFromRequest(Request request, String key, String defaultValue) {
        String value = defaultValue;
        if (request.getAttachments() != null && request.getAttachments().containsKey(key)) {
            value = request.getAttachments().get(key);
        }
        return value;
    }

    /**
     * 目前根据 group/interface/version 来唯一标示一个服务
     *
     * @param url
     * @return
     */
    public static String getServiceKey(URL url) {
        return getServiceKey(url.getGroup(), url.getPath(), url.getVersion());
    }

    /**
     * protocol key: protocol://host:port/group/interface/version
     * 
     * @param url
     * @return
     */
    public static String getProtocolKey(URL url) {
        return url.getProtocol() + MotanConstants.PROTOCOL_SEPARATOR + url.getServerPortStr() + MotanConstants.PATH_SEPARATOR
                + url.getGroup() + MotanConstants.PATH_SEPARATOR + url.getPath() + MotanConstants.PATH_SEPARATOR + url.getVersion();
    }

    /**
     * 输出请求的关键信息： requestId=** interface=** method=**(**)
     * 
     * @param request
     * @return
     */
    public static String toString(Request request) {
        return "requestId=" + request.getRequestId() + " interface=" + request.getInterfaceName() + " method=" + request.getMethodName()
                + "(" + request.getParamtersDesc() + ")";
    }

    /**
     * 根据Request得到 interface.method(paramDesc) 的 desc
     * 
     * <pre>
	 * 		比如：
	 * 			package com.weibo.api.motan;
	 * 
	 * 		 	interface A { public hello(int age); }
	 * 
	 * 			那么return "com.weibo.api.motan.A.hell(int)"
	 * </pre>
     * 
     * @param request
     * @return
     */
    public static String getFullMethodString(Request request) {
        return request.getInterfaceName() + "." + request.getMethodName() + "("
                + request.getParamtersDesc() + ")";
    }
    
    public static String getGroupMethodString(Request request){
        return getGroupFromRequest(request) + "_" + getFullMethodString(request);
    }


    /**
     * 判断url:source和url:target是否可以使用共享的service channel(port) 对外提供服务
     * 
     * <pre>
	 * 		1） protocol
	 * 		2） codec 
	 * 		3） serialize
	 * 		4） maxContentLength
	 * 		5） maxServerConnection
	 * 		6） maxWorkerThread
	 * 		7） workerQueueSize
	 * 		8） heartbeatFactory
	 * </pre>
     * 
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShareServiceChannel(URL source, URL target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.codec.getName()), target.getParameter(URLParamType.codec.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.serialize.getName()),
                target.getParameter(URLParamType.serialize.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.maxContentLength.getName()),
                target.getParameter(URLParamType.maxContentLength.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.maxServerConnection.getName()),
                target.getParameter(URLParamType.maxServerConnection.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.maxWorkerThread.getName()),
                target.getParameter(URLParamType.maxWorkerThread.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.workerQueueSize.getName()),
                target.getParameter(URLParamType.workerQueueSize.getName()))) {
            return false;
        }

        return StringUtils.equals(source.getParameter(URLParamType.heartbeatFactory.getName()),
                target.getParameter(URLParamType.heartbeatFactory.getName()));

    }

    /**
     * 判断url:source和url:target是否可以使用共享的client channel(port) 对外提供服务
     * 
     * <pre>
	 * 		1） protocol
	 * 		2） codec 
	 * 		3） serialize
	 * 		4） maxContentLength
	 * 		5） maxClientConnection
	 * 		6） heartbeatFactory
	 * </pre>
     * 
     * @param source
     * @param target
     * @return
     */
    public static boolean checkIfCanShallClientChannel(URL source, URL target) {
        if (!StringUtils.equals(source.getProtocol(), target.getProtocol())) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.codec.getName()), target.getParameter(URLParamType.codec.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.serialize.getName()),
                target.getParameter(URLParamType.serialize.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.maxContentLength.getName()),
                target.getParameter(URLParamType.maxContentLength.getName()))) {
            return false;
        }

        if (!StringUtils.equals(source.getParameter(URLParamType.maxClientConnection.getName()),
                target.getParameter(URLParamType.maxClientConnection.getName()))) {
            return false;
        }

        return StringUtils.equals(source.getParameter(URLParamType.heartbeatFactory.getName()),
                target.getParameter(URLParamType.heartbeatFactory.getName()));

    }

    /**
     * serviceKey: group/interface/version
     * 
     * @param group
     * @param interfaceName
     * @param version
     * @return
     */
    private static String getServiceKey(String group, String interfaceName, String version) {
        return group + MotanConstants.PATH_SEPARATOR + interfaceName + MotanConstants.PATH_SEPARATOR + version;
    }
    
    /**
     * 获取默认motan协议配置
     * @return motan协议配置
     */
    public static ProtocolConfig getDefaultProtocolConfig(){
        ProtocolConfig pc = new ProtocolConfig();
        pc.setId("motan");
        pc.setName("motan");
        return pc;
    }
    
    /**
     * 默认本地注册中心
     * @return local registry
     */
    public static RegistryConfig getDefaultRegistryConfig(){
        RegistryConfig local = new RegistryConfig();
        local.setRegProtocol("local");
        return local;
    }
    
    public static String removeAsyncSuffix(String path){
        if(path != null && path.endsWith(MotanConstants.ASYNC_SUFFIX)){
            return path.substring(0, path.length() - MotanConstants.ASYNC_SUFFIX.length());
        }
        return path;
    }

    public static Response buildErrorResponse(long requestId, Exception e) {
        DefaultResponse response = new DefaultResponse(requestId);
        response.setException(e);
        return response;
    }
}
