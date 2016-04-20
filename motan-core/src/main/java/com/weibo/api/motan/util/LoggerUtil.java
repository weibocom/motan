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

import com.weibo.api.motan.log.DefaultLogService;
import com.weibo.api.motan.log.LogService;

/**
 * 
 * @author maijunsheng
 * @author zhanglei
 * 
 */
public class LoggerUtil {
    private static LogService logService = new DefaultLogService();// 可以通过设置为不同logservice控制log行为。

    public static boolean isTraceEnabled() {
        return logService.isTraceEnabled();
    }

    public static boolean isDebugEnabled() {
        return logService.isDebugEnabled();
    }

    public static boolean isWarnEnabled() {
        return logService.isWarnEnabled();
    }

    public static boolean isErrorEnabled() {
        return logService.isErrorEnabled();
    }

    public static boolean isStatsEnabled() {
        return logService.isStatsEnabled();
    }

    public static void trace(String msg) {
        logService.trace(msg);
    }

    public static void debug(String msg) {
        logService.debug(msg);
    }

    public static void debug(String format, Object... argArray) {
        logService.debug(format, argArray);
    }

    public static void debug(String msg, Throwable t) {
        logService.debug(msg, t);
    }

    public static void info(String msg) {
        logService.info(msg);
    }

    public static void info(String format, Object... argArray) {
        logService.info(format, argArray);
    }

    public static void info(String msg, Throwable t) {
        logService.info(msg, t);
    }

    public static void warn(String msg) {
        logService.warn(msg);
    }

    public static void warn(String format, Object... argArray) {
        logService.warn(format, argArray);
    }

    public static void warn(String msg, Throwable t) {
        logService.warn(msg, t);
    }

    public static void error(String msg) {
        logService.error(msg);
    }

    public static void error(String format, Object... argArray) {
        logService.error(format, argArray);
    }

    public static void error(String msg, Throwable t) {
        logService.error(msg, t);
    }

    public static void accessLog(String msg) {
        logService.accessLog(msg);
    }

    public static void accessStatsLog(String msg) {
        logService.accessStatsLog(msg);
    }

    public static void accessStatsLog(String format, Object... argArray) {
        logService.accessStatsLog(format, argArray);
    }

    public static void accessProfileLog(String format, Object... argArray) {
        logService.accessProfileLog(format, argArray);
    }

    public static LogService getLogService() {
        return logService;
    }

    public static void setLogService(LogService logService) {
        LoggerUtil.logService = logService;
    }

}
