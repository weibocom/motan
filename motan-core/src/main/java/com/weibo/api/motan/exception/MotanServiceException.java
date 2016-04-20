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
 * wrapper service exception.
 * 
 * @author maijunsheng
 * 
 */
public class MotanServiceException extends MotanAbstractException {
    private static final long serialVersionUID = -3491276058323309898L;

    public MotanServiceException() {
        super(MotanErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public MotanServiceException(MotanErrorMsg motanErrorMsg) {
        super(motanErrorMsg);
    }

    public MotanServiceException(String message) {
        super(message, MotanErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public MotanServiceException(String message, MotanErrorMsg motanErrorMsg) {
        super(message, motanErrorMsg);
    }

    public MotanServiceException(String message, Throwable cause) {
        super(message, cause, MotanErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public MotanServiceException(String message, Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(message, cause, motanErrorMsg);
    }

    public MotanServiceException(Throwable cause) {
        super(cause, MotanErrorMsgConstant.SERVICE_DEFAULT_ERROR);
    }

    public MotanServiceException(Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(cause, motanErrorMsg);
    }
}
