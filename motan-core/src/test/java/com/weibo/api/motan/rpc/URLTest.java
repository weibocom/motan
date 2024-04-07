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

package com.weibo.api.motan.rpc;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import junit.framework.TestCase;

import java.lang.reflect.Method;

/**
 * URL test
 *
 * @author fishermen
 * @version V1.0 created at: 2013-7-19
 */

public class URLTest extends TestCase {

    public void testCheckGetMethod() {
        Method[] methods = URL.class.getDeclaredMethods();
        for (Method m : methods) {
            // 对于带参数的get方法，必须返回对象，不能返回原始类型，防止某些不经意的修改，引发错误
            if (m.getName().startsWith("get") && m.getParameterTypes().length > 0) {
                if (m.getReturnType().isPrimitive()) {
                    fail(String.format("URL.%s should not return primitive type", m.getName()));
                }
            }
        }
    }

    public void testProtocolCompatible() {
        URL serverUrl = new URL(MotanConstants.PROTOCOL_MOTAN2, "127.0.0.1", 8002, "com.weibo.api.motan.protocol.example.IHello");
        URL clientUrl = serverUrl.createCopy();
        URL clientUrl2 = serverUrl.createCopy();
        clientUrl2.setProtocol(MotanConstants.PROTOCOL_MOTAN);
        serverUrl.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);

        assertTrue(serverUrl.canServe(clientUrl));
        assertTrue(serverUrl.canServe(clientUrl2)); // motan2 server can serve motan client

        serverUrl.setProtocol(MotanConstants.PROTOCOL_MOTAN);
        assertFalse(serverUrl.canServe(clientUrl)); // motan server can not serve motan2 client
        assertTrue(serverUrl.canServe(clientUrl2));
    }
}
