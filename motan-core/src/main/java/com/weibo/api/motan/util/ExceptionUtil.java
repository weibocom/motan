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

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.exception.*;
import org.apache.commons.lang3.StringUtils;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-14
 */
public class ExceptionUtil {

    public static final StackTraceElement[] REMOTE_MOCK_STACK = new StackTraceElement[]{new StackTraceElement("remoteClass",
            "remoteMethod", "remoteFile", 1)};

    /**
     * 判定是否是业务方的逻辑抛出的异常
     * <p>
     * <pre>
     * 		true: 来自业务方的异常
     * 		false: 来自框架本身的异常
     * </pre>
     *
     * @param e
     * @return
     */
    @Deprecated
    public static boolean isBizException(Exception e) {
        return e instanceof MotanBizException;
    }

    public static boolean isBizException(Throwable t) {
        return t instanceof MotanBizException;
    }


    /**
     * 是否框架包装过的异常
     *
     * @param e
     * @return
     */
    @Deprecated
    public static boolean isMotanException(Exception e) {
        return e instanceof MotanAbstractException;
    }

    public static boolean isMotanException(Throwable t) {
        return t instanceof MotanAbstractException;
    }

    public static String toMessage(Exception e) {
        JSONObject jsonObject = new JSONObject();
        int type = 1;
        int code = 500;
        String errmsg = null;

        if (e instanceof MotanFrameworkException) {
            MotanFrameworkException mfe = (MotanFrameworkException) e;
            type = 0;
            code = mfe.getErrorCode();
            errmsg = mfe.getOriginMessage();
        } else if (e instanceof MotanServiceException) {
            MotanServiceException mse = (MotanServiceException) e;
            type = 1;
            code = mse.getErrorCode();
            errmsg = mse.getOriginMessage();
        } else if (e instanceof MotanBizException) {
            MotanBizException mbe = (MotanBizException) e;
            type = 2;
            code = mbe.getErrorCode();
            errmsg = mbe.getOriginMessage();
            if(mbe.getCause() != null){
                errmsg = errmsg + ", org err:" + mbe.getCause().getMessage();
            }
        } else {
            errmsg = e.getMessage();
        }
        jsonObject.put("errcode", code);
        jsonObject.put("errmsg", errmsg);
        jsonObject.put("errtype", type);
        return jsonObject.toString();
    }

    public static MotanAbstractException fromMessage(String msg) {
        if (StringUtils.isNotBlank(msg)) {
            try {
                JSONObject jsonObject = JSONObject.parseObject(msg);
                int type = jsonObject.getIntValue("errtype");
                int errcode = jsonObject.getIntValue("errcode");
                String errmsg = jsonObject.getString("errmsg");
                MotanAbstractException e = null;
                switch (type) {
                    case 1:
                        e = new MotanServiceException(errmsg, new MotanErrorMsg(errcode, errcode, errmsg));
                        break;
                    case 2:
                        e = new MotanBizException(errmsg, new MotanErrorMsg(errcode, errcode, errmsg));
                        break;
                    default:
                        e = new MotanFrameworkException(errmsg, new MotanErrorMsg(errcode, errcode, errmsg));
                }
                return e;
            } catch (Exception e) {
                LoggerUtil.warn("build exception from msg fail. msg:" + msg);
            }
        }
        return null;
    }

    /**
     * 覆盖给定exception的stack信息，server端产生业务异常时调用此类屏蔽掉server端的异常栈。
     *
     * @param e
     */
    public static void setMockStackTrace(Throwable e) {
        if (e != null) {
            try {
                e.setStackTrace(REMOTE_MOCK_STACK);
            } catch (Exception e1) {
                LoggerUtil.warn("replace remote exception stack fail!" + e1.getMessage());
            }
        }
    }
}
