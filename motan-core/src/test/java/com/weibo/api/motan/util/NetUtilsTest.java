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

/**
 * 
 */
package com.weibo.api.motan.util;

import com.weibo.api.motan.BaseTestCase;
import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author bozheng
 * 
 */
public class NetUtilsTest extends BaseTestCase {
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetLocalAddress() {
        InetAddress address = NetUtils.getLocalAddress();
        assertNotNull(address);
        assertTrue(NetUtils.isValidAddress(address));
        try {
            if(NetUtils.isValidAddress(InetAddress.getLocalHost())){
                assertEquals(InetAddress.getLocalHost(), address);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

}
