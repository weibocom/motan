/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.api.motan.registry.weibomesh;

import com.weibo.api.motan.exception.MotanFrameworkException;

import java.util.Map;

/**
 * @author zhanglei28
 * @date 2021/6/28.
 * 向mesh发送管理指令
 */
public interface MeshTransport {

    ManageResponse getManageRequest(String url) throws MotanFrameworkException;

    ManageResponse postManageRequest(String url, Map<String, String> params) throws MotanFrameworkException;

    ManageResponse postManageRequest(String url, String content) throws MotanFrameworkException;


    class ManageResponse {
        private int statusCode;
        private String content;

        public ManageResponse(int statusCode, String content) {
            this.statusCode = statusCode;
            this.content = content;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

}
