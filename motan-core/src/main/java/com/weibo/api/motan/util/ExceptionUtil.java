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

import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanBizException;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 * 
 */
public class ExceptionUtil {

    /**
     * 判定是否是业务方的逻辑抛出的异常
     * 
     * <pre>
	 * 		true: 来自业务方的异常
	 * 		false: 来自框架本身的异常
	 * </pre>
     * 
     * @param e
     * @return
     */
    public static boolean isBizException(Exception e) {
        return e instanceof MotanBizException;
    }


    /**
     * 是否框架包装过的异常
     * 
     * @param e
     * @return
     */
    public static boolean isMotanException(Exception e) {
        return e instanceof MotanAbstractException;
    }
}
