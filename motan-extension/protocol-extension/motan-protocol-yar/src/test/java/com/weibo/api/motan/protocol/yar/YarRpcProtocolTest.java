/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.api.motan.protocol.yar;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.rpc.DefaultProvider;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.transport.MessageHandler;
import com.weibo.api.motan.transport.ProviderMessageRouter;

/**
 * 
 * @Description YarRpcProtocolTest
 * @author zhanglei
 * @date 2016年7月27日
 *
 */
public class YarRpcProtocolTest {

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testInitRequestRouter() {
        YarRpcProtocol protocol = new YarRpcProtocol();
        URL url = new URL("motan", "localhost", 8002, "urlpath");
        Provider provider = new DefaultProvider(null, url, MessageHandler.class);
        ProviderMessageRouter router = protocol.initRequestRouter(url, provider);
        assertNotNull(router);

        URL url2 = new URL("motan", "localhost", 8003, "urlpath2");
        Provider provider2 = new DefaultProvider(null, url2, MessageHandler.class);
        ProviderMessageRouter router2 = protocol.initRequestRouter(url2, provider2);
        assertNotNull(router2);
        assertFalse(router2.equals(router));

        URL url3 = new URL("motan", "localhost", 8002, "urlpath3");
        Provider provider3 = new DefaultProvider(null, url3, MessageHandler.class);
        ProviderMessageRouter router3 = protocol.initRequestRouter(url3, provider3);
        assertNotNull(router3);
        assertTrue(router3.equals(router));

        try {
            protocol.initRequestRouter(url, provider);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("duplicate yar provider"));
        }
    }

}
