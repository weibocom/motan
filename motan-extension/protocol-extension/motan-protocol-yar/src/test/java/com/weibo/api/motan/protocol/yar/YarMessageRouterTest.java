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

import com.weibo.api.motan.protocol.yar.annotation.YarConfig;
import com.weibo.api.motan.rpc.DefaultProvider;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.yar.YarRequest;
import com.weibo.yar.YarResponse;

/**
 * 
 * @Description YarMessageRouterTest
 * @author zhanglei
 * @date 2016年7月27日
 *
 */
public class YarMessageRouterTest {
    TestYarMessageRouter router = new TestYarMessageRouter();
    DefaultResponse response;
    String requestPath = "/test/anno_path";

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testHandle() {
        response = new DefaultResponse();
        response.setValue("test");
        response.setProcessTime(1);

        Provider provider = new DefaultProvider(null, null, AnnoService.class);
        router.addProvider(provider);

        YarRequest yarRequest = new YarRequest(1, "JSON", "hello", new Object[] {"params"});
        yarRequest.setRequestPath(requestPath);

        YarResponse yarResponse = (YarResponse) router.handle(null, yarRequest);
        assertEquals(YarProtocolUtil.convert(response, "JSON"), yarResponse);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testAddProvider() {
        Provider provider = new DefaultProvider(null, null, AnnoService.class);
        router.addProvider(provider);
        assertTrue(router.checkProvider(requestPath));

        router.removeProvider(provider);
        assertFalse(router.checkProvider(requestPath));

        URL url = new URL("motan", "localhost", 8002, "urlpath");
        provider = new DefaultProvider(null, url, normalService.class);
        router.addProvider(provider);
        assertTrue(router.checkProvider(YarProtocolUtil.getYarPath(normalService.class, url)));

        router.removeProvider(provider);
        assertFalse(router.checkProvider(YarProtocolUtil.getYarPath(normalService.class, url)));
    }

    class TestYarMessageRouter extends YarMessageRouter {
        public boolean checkProvider(String path) {
            return providerMap.containsKey(path);
        }

        @Override
        protected Response call(Request request, Provider<?> provider) {
            return response;
        }

    }

    @YarConfig(path = "/test/anno_path")
    interface AnnoService {
        String hello(String name);
    }

    interface normalService {
        String hello(String name);
    }
}
