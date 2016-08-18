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

package com.weibo.api.motan.rpc;

import java.util.HashMap;
import java.util.Map;

import com.weibo.api.motan.common.URLParamType;

/**
 * rpc session context
 * 
 * @author maijunsheng
 * 
 */
public class RpcContext {
    private Map<Object, Object> attribute = new HashMap<Object, Object>();
    private Request request;
    private Response response;
    private String clientRequestId = null;

    private static final ThreadLocal<RpcContext> localContext = new ThreadLocal<RpcContext>() {
        protected RpcContext initialValue() {
            return new RpcContext();
        }
    };

    public static RpcContext getContext() {
        return localContext.get();
    }
    
    /**
     * init new rpcContext with request
     * @param request
     * @return
     */
    public static RpcContext init(Request request){
        RpcContext context = new RpcContext();
        if(request != null){
            context.setRequest(request);
            context.setClientRequestId(request.getAttachments().get(URLParamType.requestIdFromClient.getName()));
        }
        localContext.set(context);
        return context;
    }

    public static void destroy() {
        localContext.remove();
    }

    /**
     * clientRequestId > request.id 
     * @return
     */
    public String getRequestId(){
        if(clientRequestId != null){
            return clientRequestId;
        } else{
            return request == null ? null : String.valueOf(request.getRequestId());
        }
    }
    
    public void putAttribute(Object key, Object value){
        attribute.put(key, value);
    }
    
    public Object getAttribute(Object key) {
        return attribute.get(key);
    }
    
    public void revomeAttribute(Object key){
        attribute.remove(key);
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }
    
}
