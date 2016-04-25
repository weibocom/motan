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

package com.weibo.api.motan.filter;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
@SpiMeta(name = "switcher")
public class SwitcherFilter implements Filter {
    @Override
    public Response filter(Caller<?> caller, Request request) {
        // 检查接口或方法降级开关状态
        if (MotanSwitcherUtil.isOpen(request.getInterfaceName())
                || MotanSwitcherUtil.isOpen(MotanFrameworkUtil.getFullMethodString(request))) {
            // 返回的reponse需要设置exception，这样invocationhandler会在throwException为false时，构建默认值返回
            return mockDefaultResponse(request);
        }
        return caller.call(request);
    }

    /**
     * 返回的reponse需要设置exception，这样invocationhandler会在throwException为false时，构建默认值返回
     * 
     * @param request
     * @return
     */
    private Response mockDefaultResponse(Request request) {
        DefaultResponse response = new DefaultResponse(null, request.getRequestId());
        response.setException(new MotanServiceException("Request false for switcher is on"));
        return response;
    }

}
