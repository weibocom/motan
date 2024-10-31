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

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.common.MotanConstants;

import static com.weibo.api.motan.TestUtils.getModifiableEnvironment;

/**
 * @author bozheng
 */
public class NetUtilsTest extends BaseTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getModifiableEnvironment().remove(MotanConstants.ENV_MOTAN_LOCAL_IP);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        getModifiableEnvironment().remove(MotanConstants.ENV_MOTAN_LOCAL_IP);
    }

    public void testIsValidLocalHost() {
        String[] validIps = new String[]{
                "192.168.0.1",
                "10.0.0.0",
                "172.16.0.1",
                "255.255.255.255",
                "10.185.10.10"
        };
        String[] invalidIps = new String[]{
                null,
                "localhost",
                "127.0.0.1",
                "0.0.0.0",
                "256.255.255.1",
                "192.168.0",
                "192.168.0.1.2",
                "256.0.0.1",
                "192.168.0.256",
                "192.168.0.-1",
                "192.168.0.1.",
                ".192.168.0.1",
                "192.168.0.1a",
                "192.168.0",
                "192.168.0.",
                "192.168",
                "a.b.c.d",
                "192.168.0.1:8080",
                " 192.168.0.1 ",
                "192. 168.0.1"
        };
        for (String ip : validIps) {
            assertTrue(NetUtils.isValidLocalHost(ip));
        }
        for (String ip : invalidIps) {
            assertFalse(NetUtils.isValidLocalHost(ip));
        }
    }

    public void testGetLocalIpString() throws Exception {
        String ip = NetUtils.getLocalIpString();
        assertNotNull(ip);
        assertTrue(NetUtils.isValidLocalHost(ip));

        // test use env ip
        String expectIp = "255.255.255.255";
        NetUtils.clearCache();
        getModifiableEnvironment().put(MotanConstants.ENV_MOTAN_LOCAL_IP, expectIp);
        ip = NetUtils.getLocalIpString();
        assertEquals(expectIp, ip);
    }

}
