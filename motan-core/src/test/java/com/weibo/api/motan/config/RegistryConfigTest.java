/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
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

package com.weibo.api.motan.config;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.UrlUtils;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2021/7/13.
 */
public class RegistryConfigTest extends BaseTestCase {

    public void testGetAddressParams() throws Exception {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("127.0.0.1:9981?mport=8002&proxyRegistry=testVintage&meshRegistryName=meshVintage");
        Map<String, String> map = registryConfig.getAddressParams();
        assertEquals(map.get("proxyRegistry"), "testVintage");
        assertEquals(map.get("meshRegistryName"), "meshVintage");
        assertEquals(map.get("mport"), "8002");
        assertEquals(map.size(), 3);

        registryConfig.setAddress("127.0.0.1:9981?mport=8002&proxyRegistry=testVintage&meshRegistryName=meshVintage , 127.0.0.1:8003");
        map = registryConfig.getAddressParams();
        assertEquals(map.get("proxyRegistry"), "testVintage");
        assertEquals(map.get("meshRegistryName"), "meshVintage");
        assertEquals(map.get("mport"), "8002");
        assertEquals(map.size(), 3);
    }

    @Test
    public void testToURLs() throws Exception {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("test");
        registryConfig.setRegProtocol("vintage");
        registryConfig.setAddress("weibomesh://127.0.0.1:9981?mport=8002&proxyRegistry=testVintage");
        registryConfig.setCheck("true");
        registryConfig.setRequestTimeout(1000);

        RegistryConfig proxyRegistry = new RegistryConfig();
        proxyRegistry.setName("testVintage");
        proxyRegistry.setRegProtocol("vintage");
        proxyRegistry.setAddress("vintage.test.weibo.com");
        proxyRegistry.setRequestTimeout(1000);

        registryConfig.setProxyRegistry(proxyRegistry);
        List<URL> registryUrls = registryConfig.toURLs();
        assertEquals(registryUrls.size(), 1);
        String expectURLString = "weibomesh://127.0.0.1:9981/com.weibo.api.motan.registry.RegistryService?path=com.weibo.api.motan.registry.RegistryService&proxyRegistryUrlString=vintage%253A%252F%252Fvintage.test.weibo.com%253A0%252Fcom.weibo.api.motan.registry.RegistryService%253Fpath%253Dcom.weibo.api.motan.registry.RegistryService%2526protocol%253Dvintage%2526address%253Dvintage.test.weibo.com%2526name%253DtestVintage%2526requestTimeout%253D1000%2526&protocol=vintage&address=weibomesh%3A%2F%2F127.0.0.1%3A9981%3Fmport%3D8002%26proxyRegistry%3DtestVintage&name=test&mport=8002&check=true&requestTimeout=1000&proxyRegistry=testVintage&\n";
        registryUrls.get(0).removeParameter("refreshTimestamp");

        // test urls to string
        String urlString = UrlUtils.urlsToString(registryUrls);
        List<URL> parsedUrls = UrlUtils.stringToURLs(urlString);
        assertEquals(registryUrls, parsedUrls);

        URL resultURL = URL.valueOf(expectURLString);
        assertEquals(registryUrls.get(0), resultURL);
    }

}