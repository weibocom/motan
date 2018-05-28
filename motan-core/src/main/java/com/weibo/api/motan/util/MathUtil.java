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

/**
 * @author fishermen
 * @version V1.0 created at: 2013-6-20
 */

public class MathUtil {


    /**
     * 针对int类型字符串进行解析，如果存在格式错误，则返回默认值（defaultValue）
     * Parse intStr, return defaultValue when numberFormatException occurs
     * @param intStr
     * @param defaultValue
     * @return
     */
    public static int parseInt(String intStr, int defaultValue) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            LoggerUtil.debug("ParseInt false, for malformed intStr:" + intStr);
            return defaultValue;
        }
    }

    /**
     * 针对long类型字符串进行解析，如果存在格式错误，则返回默认值（defaultValue）
     * Parse longStr, return defaultValue when numberFormatException occurs
     * @param longStr
     * @param defaultValue
     * @return
     */
    public static long parseLong(String longStr, long defaultValue){
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 通过二进制位操作将originValue转化为非负数:
     *      0和正数返回本身
     *      负数通过二进制首位取反转化为正数或0（Integer.MIN_VALUE将转换为0）
     * return non-negative int value of originValue
     * @param originValue
     * @return positive int
     */
    public static int getNonNegative(int originValue){
        return 0x7fffffff & originValue;
    }
}
