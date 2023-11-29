/*
 *
 *   Copyright 2009-2023 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.admin;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import junit.framework.TestCase;

/**
 * @author zhanglei28
 * @date 2023/11/29.
 */
public class DefaultPermissionCheckerTest extends TestCase {
    public void testDefaultPermissionChecker() {
        String rightToken = "rightToken";
        MotanGlobalConfigUtil.putConfig(MotanConstants.ADMIN_TOKEN, rightToken);
        // test ip
        checkIp("127.0.0.2", false);
        checkIp("10.0.10.0", false);
        checkIp("127.0.0.1", true);
        checkIp("0:0:0:0:0:0:0:1", true);

        // test token
        checkToken(rightToken, true);
        checkToken("failToken", false);
        checkToken("", false);
        checkToken(null, false);

        // test extension validate
        PermissionChecker checker = new DefaultPermissionChecker() {
            @Override
            protected boolean extendValidate(Request request) {
                return "extToken".equals(request.getAttachments().get("extension"));
            }
        };
        Request request = new DefaultRequest();
        request.setAttachment(URLParamType.host.getName(), "unknownHost");
        assertFalse(checker.check(request));
        request.setAttachment("extension", "extToken");
        assertTrue(checker.check(request));
    }

    private void checkToken(String token, boolean expectResult) {
        Request request = new DefaultRequest();
        request.setAttachment(URLParamType.host.getName(), "unknownHost");
        if (token != null) {
            request.setAttachment("token", token);
        }
        assertEquals(expectResult, new DefaultPermissionChecker().check(request));
    }

    private void checkIp(String host, boolean expectResult) {
        Request request = new DefaultRequest();
        request.setAttachment(URLParamType.host.getName(), host);
        assertEquals(expectResult, AdminUtil.getDefaultPermissionChecker().check(request));
    }

}