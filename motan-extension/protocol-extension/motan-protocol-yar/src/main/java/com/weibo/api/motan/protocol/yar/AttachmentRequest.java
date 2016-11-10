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

import java.util.Map;

import com.weibo.yar.YarRequest;

/**
 * 
 * @Description YarRequest with attachments. rpc attachments such as auth，application can pass with
 *              this class.
 * @author zhanglei
 * @date 2016-7-26
 *
 */
public class AttachmentRequest extends YarRequest {
    private Map<String, String> attachments;

    public AttachmentRequest(String packagerName, String methodName, Object[] parameters) {
        super(packagerName, methodName, parameters);
    }

    public AttachmentRequest(long id, String packagerName, String methodName, Object[] parameters) {
        super(id, packagerName, methodName, parameters);
    }

    public AttachmentRequest() {
        super();
    }

    public AttachmentRequest(YarRequest yarRequest, Map<String, String> attachments) {
        this(yarRequest.getId(), yarRequest.getPackagerName(), yarRequest.getMethodName(), yarRequest.getParameters());
        this.attachments = attachments;
    }

    public Map<String, String> getAttachments() {
        return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
        this.attachments = attachments;
    }

}
