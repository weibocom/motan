/*
 *
 *   Copyright 2009-2022 Weibo, Inc.
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

package com.weibo.api.motan.util;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2022/7/19.
 */
public class MeshProxyUtilTest {
    String mode = "server";
    String mport = "8803"; // not default value
    String port = "9983"; // not default value
    String agentFilters = "  af_accessLog%2Caf_metrics  "; // with space
    String encodedValue = "2132%3Aer93%3A3e987%3Adfje%2F%2Fjdife"; // a URLEncoded value for known key

    @After
    public void tearDown() throws Exception {
        getModifiableEnvironment().remove(MeshProxyUtil.MESH_PROXY_ENV_NAME);
    }

    @Test
    public void processMeshProxy() throws Exception {
        // not proxy if env not set
        List<URL> originRegistryUrls = new ArrayList<>();
        originRegistryUrls.add(new URL("zookeeper", "localhost", 2181, "testZkRegistry"));
        originRegistryUrls.add(new URL("local", "localhost", 0, "testLocalRegistry"));
        originRegistryUrls.add(new URL("direct", "localhost", 8802, "testDirectRegistry"));
        originRegistryUrls.add(new URL("weibomesh", "localhost", 9988, "testMeshRegistry"));
        URL serviceUrl = new URL("motan2", "localhost", 8802, "testService");
        List<URL> resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);

        assertNull(MeshProxyUtil.getProxyConfig()); // not init
        check(originRegistryUrls, resultUrl, false, null);

        // env with minimal param
        getModifiableEnvironment().put(MeshProxyUtil.MESH_PROXY_ENV_NAME, "mode:" + mode); // minimal key as default
        MeshProxyUtil.reset();
        assertEquals(mode, MeshProxyUtil.getProxyConfig().get("mode")); // check init proxy config
        assertFalse(MeshProxyUtil.setInitChecked(true)); // initChecked is false because not have MeshRegistry extension. so set initChecked true for unit test

        // check default port
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        check(originRegistryUrls, resultUrl, true, null); // proxy server url
        assertEquals(0, resultUrl.get(0).getPort().intValue()); // default port

        // check mode
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        Map<String, String> proxiedParams = new HashMap<>();
        proxiedParams.put("mode", mode);
        proxiedParams.put(URLParamType.dynamic.getName(), "true");
        proxiedParams.put(URLParamType.proxyRegistryUrlString.getName(), StringTools.urlEncode(originRegistryUrls.get(0).toFullStr()));

        check(originRegistryUrls, resultUrl, true, proxiedParams); // proxy server url
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, false);
        check(originRegistryUrls, resultUrl, false, null); // not proxy client url in server mode

        mode = "client";
        getModifiableEnvironment().put(MeshProxyUtil.MESH_PROXY_ENV_NAME, "mode:" + mode); // minimal key as default
        MeshProxyUtil.reset();
        MeshProxyUtil.setInitChecked(true);
        assertEquals(mode, MeshProxyUtil.getProxyConfig().get("mode"));
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, false);
        proxiedParams.put("mode", mode);
        check(originRegistryUrls, resultUrl, true, proxiedParams); // proxy client url
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        check(originRegistryUrls, resultUrl, false, null); // not proxy server url in client mode

        mode = "all";
        getModifiableEnvironment().put(MeshProxyUtil.MESH_PROXY_ENV_NAME, "mode:" + mode); // minimal key as default
        MeshProxyUtil.reset();
        MeshProxyUtil.setInitChecked(true);
        assertEquals(mode, MeshProxyUtil.getProxyConfig().get("mode"));
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, false);
        proxiedParams.put("mode", mode);
        check(originRegistryUrls, resultUrl, true, proxiedParams); // proxy client url
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        check(originRegistryUrls, resultUrl, true, proxiedParams); // proxy server url

        // env with more params
        getModifiableEnvironment().put(MeshProxyUtil.MESH_PROXY_ENV_NAME, "mode:" + mode + ",mport:" + mport + ",port:" + port + ",test:" + encodedValue + ", filter:" + agentFilters); // minimal key as default
        MeshProxyUtil.reset();
        MeshProxyUtil.setInitChecked(true);
        assertEquals(mode, MeshProxyUtil.getProxyConfig().get("mode"));
        assertEquals(mport, MeshProxyUtil.getProxyConfig().get("mport"));
        assertEquals(port, MeshProxyUtil.getProxyConfig().get("port"));
        assertEquals(StringTools.urlDecode(agentFilters.trim()), MeshProxyUtil.getProxyConfig().get("filter")); // with trim
        assertEquals(StringTools.urlDecode(encodedValue), MeshProxyUtil.getProxyConfig().get("test")); // url decode with value

        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        proxiedParams.put("filter", MeshProxyUtil.getProxyConfig().get("filter"));
        proxiedParams.put("test", MeshProxyUtil.getProxyConfig().get("test"));
        proxiedParams.put("mport", MeshProxyUtil.getProxyConfig().get("mport"));
        check(originRegistryUrls, resultUrl, true, proxiedParams);

        // check not motan2 protocol. //TODO remove this test if motan protocol is supported
        serviceUrl.setProtocol("motan");
        resultUrl = MeshProxyUtil.processMeshProxy(originRegistryUrls, serviceUrl, true);
        check(originRegistryUrls, resultUrl, false, null);
    }

    private void check(List<URL> originRegistryUrls, List<URL> resultRegistryUrls, boolean isProxied, Map<String, String> proxiedParams) {
        assertEquals(originRegistryUrls.size(), resultRegistryUrls.size()); // registry size is same
        for (int i = 0; i < resultRegistryUrls.size(); i++) {
            if (isProxied) {
                String orgProtocol = originRegistryUrls.get(i).getProtocol();
                String newProtocol = resultRegistryUrls.get(i).getProtocol();
                if ("local".equals(orgProtocol) || "direct".equals(orgProtocol) || "weibomesh".equals(orgProtocol)) {
                    // not proxy if registry protocol is local or direct or weibomesh
                    assertEquals(originRegistryUrls.get(i), resultRegistryUrls.get(i));
                } else {
                    assertEquals("weibomesh", newProtocol);
                    if (proxiedParams != null) {
                        for (Map.Entry<String, String> entry : proxiedParams.entrySet()) {
                            assertEquals(entry.getValue(), resultRegistryUrls.get(i).getParameter(entry.getKey()));
                        }
                    }
                }
            } else { // registry will not modify if not proxy
                assertEquals(originRegistryUrls.get(i), resultRegistryUrls.get(i));
            }
        }
    }

    private static Map<String, String> getModifiableEnvironment() throws Exception {
        Class<?> pe = Class.forName("java.lang.ProcessEnvironment");
        Method getenv = pe.getDeclaredMethod("getenv");
        getenv.setAccessible(true);
        Object unmodifiableEnvironment = getenv.invoke(null);
        Class<?> map = Class.forName("java.util.Collections$UnmodifiableMap");
        Field m = map.getDeclaredField("m");
        m.setAccessible(true);
        return (Map<String, String>) m.get(unmodifiableEnvironment);
    }

}