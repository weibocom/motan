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

package com.weibo.api.motan.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLogService implements LogService {
    private static Logger trace = LoggerFactory.getLogger("trace");
    private static Logger debug = LoggerFactory.getLogger("debug");
    private static Logger info = LoggerFactory.getLogger("info");
    private static Logger warn = LoggerFactory.getLogger("warn");
    private static Logger error = LoggerFactory.getLogger("error");
    private static Logger access = LoggerFactory.getLogger("accessLog");
    private static Logger serviceStats = LoggerFactory.getLogger("serviceStatsLog");
    private static Logger profileLogger = LoggerFactory.getLogger("profile");


    public void trace(String msg) {
        trace.trace(msg);
    }

    @Override
    public void trace(String format, Object... argArray) {
        trace.trace(format, argArray);
    }

    public void debug(String msg) {
        debug.debug(msg);
    }

    public void debug(String format, Object... argArray) {
        debug.debug(format, argArray);
    }

    public void debug(String msg, Throwable t) {
        debug.debug(msg, t);
    }

    public void info(String msg) {
        info.info(msg);
    }

    public void info(String format, Object... argArray) {
        info.info(format, argArray);
    }

    public void info(String msg, Throwable t) {
        info.info(msg, t);
    }

    public void warn(String msg) {
        warn.warn(msg);
    }

    public void warn(String format, Object... argArray) {
        warn.warn(format, argArray);
    }

    public void warn(String msg, Throwable t) {
        warn.warn(msg, t);
    }

    public void error(String msg) {
        error.error(msg);
    }

    public void error(String format, Object... argArray) {
        error.error(format, argArray);
    }

    public void error(String msg, Throwable t) {
        error.error(msg, t);
    }

    public void accessLog(String msg) {
        access.info(msg);
    }

    public void accessStatsLog(String msg) {
        serviceStats.info(msg);
    }

    public void accessStatsLog(String format, Object... argArray) {
        serviceStats.info(format, argArray);
    }

    public void accessProfileLog(String format, Object... argArray) {
        profileLogger.info(format, argArray);
    }

    @Override
    public boolean isTraceEnabled() {
        return trace.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return debug.isDebugEnabled();
    }
    
    @Override
    public boolean isInfoEnabled() {
        return info.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return warn.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return error.isErrorEnabled();
    }

    @Override
    public boolean isStatsEnabled() {
        return serviceStats.isInfoEnabled();
    }

    
}
