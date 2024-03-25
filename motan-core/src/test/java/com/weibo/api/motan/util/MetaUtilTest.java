/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

import com.weibo.api.motan.TestUtils;
import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.GlobalRuntime;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2024/3/25.
 */
public class MetaUtilTest extends TestCase {

    public void testGetLocalMeta() throws Exception {
        // test env meta load
        Map<String, String> expectMetaMap = buildDefaultMetaMap();
        Map<String, String> envMap = TestUtils.getModifiableEnvironment();
        envMap.putAll(expectMetaMap);
        assertEquals(expectMetaMap, MetaUtil._getOriginMetaInfoFromEnv());
    }

    public void testGetRefererDynamicMeta() {
        GlobalRuntime.putDynamicMeta("tk1", "tv1");
        assertEquals(MetaUtil.getLocalMeta().get("tk1"), "tv1");
        GlobalRuntime.removeDynamicMeta("tk1");
        assertNull(MetaUtil.getLocalMeta().get("tk1"));
    }

    public void testGetRefererStaticMeta() {
        Map<String, String> expectMetaMap = buildDefaultMetaMap();
        URL url = new URL("motan", "127.0.0.1", 8000, "com.weibo.api.motan.test.TestService");
        url.addParameter("otherKey1", "v1");
        url.addParameter("otherKey2", "v2");
        url.getParameters().putAll(expectMetaMap);
        Referer<IWorld> referer = new MockReferer<>(url);
        Map<String, String> result = MetaUtil.getRefererStaticMeta(referer);
        assertEquals(expectMetaMap, result);
    }

    private Map<String, String> buildDefaultMetaMap() {
        Map<String, String> metaMap = new HashMap<>();
        metaMap.put(MetaUtil.ENV_META_PREFIX + "tk1", "tv1");
        metaMap.put(MetaUtil.ENV_META_PREFIX + "WEIGHT", "100");
        metaMap.put(MetaUtil.ENV_META_PREFIX + "CPU_CORE", "16");
        metaMap.put(MetaUtil.ENV_META_PREFIX + "IDC", "xxIDC");
        return metaMap;
    }
}