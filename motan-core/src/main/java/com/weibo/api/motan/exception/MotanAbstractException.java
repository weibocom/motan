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

package com.weibo.api.motan.exception;

import com.weibo.api.motan.rpc.RpcContext;


/**
 * @author maijunsheng
 * @version 创建时间：2013-5-30
 * 
 */
public abstract class MotanAbstractException extends RuntimeException {
    private static final long serialVersionUID = -8742311167276890503L;

    protected MotanErrorMsg motanErrorMsg = MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR;
    protected String errorMsg = null;

    public MotanAbstractException() {
        super();
    }

    public MotanAbstractException(MotanErrorMsg motanErrorMsg) {
        super();
        this.motanErrorMsg = motanErrorMsg;
    }

    public MotanAbstractException(String message) {
        super(message);
        this.errorMsg = message;
    }

    public MotanAbstractException(String message, MotanErrorMsg motanErrorMsg) {
        super(message);
        this.motanErrorMsg = motanErrorMsg;
        this.errorMsg = message;
    }

    public MotanAbstractException(String message, Throwable cause) {
        super(message, cause);
        this.errorMsg = message;
    }

    public MotanAbstractException(String message, Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(message, cause);
        this.motanErrorMsg = motanErrorMsg;
        this.errorMsg = message;
    }

    public MotanAbstractException(Throwable cause) {
        super(cause);
    }

    public MotanAbstractException(Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(cause);
        this.motanErrorMsg = motanErrorMsg;
    }

    @Override
    public String getMessage() {
        String message = getOriginMessage();

        return "error_message: " + message + ", status: " + motanErrorMsg.getStatus() + ", error_code: " + motanErrorMsg.getErrorCode()
                + ",r=" + RpcContext.getContext().getRequestId();
    }

    public String getOriginMessage(){
        if (motanErrorMsg == null) {
            return super.getMessage();
        }

        String message;

        if (errorMsg != null && !"".equals(errorMsg)) {
            message = errorMsg;
        } else {
            message = motanErrorMsg.getMessage();
        }
        return message;
    }

    public int getStatus() {
        return motanErrorMsg != null ? motanErrorMsg.getStatus() : 0;
    }

    public int getErrorCode() {
        return motanErrorMsg != null ? motanErrorMsg.getErrorCode() : 0;
    }

    public MotanErrorMsg getMotanErrorMsg() {
        return motanErrorMsg;
    }
}
