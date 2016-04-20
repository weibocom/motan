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

/**
 * wrapper biz exception.
 * 
 * @author maijunsheng
 * 
 */
public class MotanBizException extends MotanAbstractException {
    private static final long serialVersionUID = -3491276058323309898L;

    public MotanBizException() {
        super(MotanErrorMsgConstant.BIZ_DEFAULT_EXCEPTION);
    }

    public MotanBizException(MotanErrorMsg motanErrorMsg) {
        super(motanErrorMsg);
    }

    public MotanBizException(String message) {
        super(message, MotanErrorMsgConstant.BIZ_DEFAULT_EXCEPTION);
    }

    public MotanBizException(String message, MotanErrorMsg motanErrorMsg) {
        super(message, motanErrorMsg);
    }

    public MotanBizException(String message, Throwable cause) {
        super(message, cause, MotanErrorMsgConstant.BIZ_DEFAULT_EXCEPTION);
    }

    public MotanBizException(String message, Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(message, cause, motanErrorMsg);
    }

    public MotanBizException(Throwable cause) {
        super(cause, MotanErrorMsgConstant.BIZ_DEFAULT_EXCEPTION);
    }

    public MotanBizException(Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(cause, motanErrorMsg);
    }
}
