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
 * wrapper client exception.
 * 
 * @author maijunsheng
 * 
 */
public class MotanFrameworkException extends MotanAbstractException {
    private static final long serialVersionUID = -1638857395789735293L;

    public MotanFrameworkException() {
        super(MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR);
    }

    public MotanFrameworkException(MotanErrorMsg motanErrorMsg) {
        super(motanErrorMsg);
    }

    public MotanFrameworkException(String message) {
        super(message, MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR);
    }

    public MotanFrameworkException(String message, MotanErrorMsg motanErrorMsg) {
        super(message, motanErrorMsg);
    }

    public MotanFrameworkException(String message, Throwable cause) {
        super(message, cause, MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR);
    }

    public MotanFrameworkException(String message, Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(message, cause, motanErrorMsg);
    }

    public MotanFrameworkException(Throwable cause) {
        super(cause, MotanErrorMsgConstant.FRAMEWORK_DEFAULT_ERROR);
    }

    public MotanFrameworkException(Throwable cause, MotanErrorMsg motanErrorMsg) {
        super(cause, motanErrorMsg);
    }

}
