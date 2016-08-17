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

/**
 * 
 * @Description rpc log服务。方便适配不同的log方式和配置。
 * @author zhanglei28
 * @date 2016年3月25日
 *
 */
public interface LogService {

    void trace(String msg);

    void trace(String format, Object... argArray);

    void debug(String msg);

    void debug(String format, Object... argArray);

    void debug(String msg, Throwable t);

    void info(String msg);

    void info(String format, Object... argArray);

    void info(String msg, Throwable t);

    void warn(String msg);

    void warn(String format, Object... argArray);

    void warn(String msg, Throwable t);

    void error(String msg);

    void error(String format, Object... argArray);

    void error(String msg, Throwable t);

    void accessLog(String msg);

    void accessProfileLog(String format, Object... argArray);

    void accessStatsLog(String msg);

    void accessStatsLog(String format, Object... argArray);

    boolean isTraceEnabled();

    boolean isDebugEnabled();
    
    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isErrorEnabled();

    boolean isStatsEnabled();

}
