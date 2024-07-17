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

package com.weibo.api.motan.protocol.support;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.weibo.api.motan.TestUtils.getModifiableEnvironment;
import static com.weibo.api.motan.common.MotanConstants.ENV_GLOBAL_FILTERS;
import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2022/6/24.
 */
public class ProtocolFilterDecoratorTest {
    ProtocolFilterDecorator protocolFilterDecorator;
    Protocol protocol;

    private int defaultFilterSize = 2;

    @Before
    public void setUp() throws Exception {
        protocol = new MockProtocol();
        protocolFilterDecorator = new ProtocolFilterDecorator(protocol);
    }

    @After
    public void tearDown() throws Exception {
        getModifiableEnvironment().remove(ENV_GLOBAL_FILTERS);
        MotanGlobalConfigUtil.remove(ENV_GLOBAL_FILTERS);
    }

    @Test
    public void testGetFilters() throws Exception {
        URL url = new URL("mock", "localhost", 7777, IWorld.class.getName());
        url.addParameter(URLParamType.filter.getName(), "statistic,switcher");
        List<Filter> filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_REFERER);
        assertEquals(2 + defaultFilterSize, filters.size());

        // test disable referer filter
        url.addParameter(URLParamType.filter.getName(), "statistic,-access");
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_REFERER);
        checkFilter(filters, 1 + defaultFilterSize - 1, "statistic", "access");

        // test disable service filter
        url.addParameter(URLParamType.filter.getName(), "statistic,switcher,-access,-notExist");
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        checkFilter(filters, 2 + defaultFilterSize - 1, "switcher", "access");

        // test env global filters
        resetUrlFilter(url);
        getModifiableEnvironment().put(ENV_GLOBAL_FILTERS, "statistic,-access, switcher,,,"); // test add, remove, empty
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        checkFilter(filters, 2 + defaultFilterSize - 1, "switcher", "access");

        // test global config
        resetUrlFilter(url);
        MotanGlobalConfigUtil.putConfig(ENV_GLOBAL_FILTERS, "statistic,-access, switcher,,,"); // test add, remove, empty
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        checkFilter(filters, 2 + defaultFilterSize - 1, "switcher", "access");

        // filter and env and global config
        resetUrlFilter(url);
        url.addParameter(URLParamType.filter.getName(), "statistic");
        getModifiableEnvironment().put(ENV_GLOBAL_FILTERS, "switcher");
        MotanGlobalConfigUtil.putConfig(ENV_GLOBAL_FILTERS, ",-access,,,");
        checkFilter(filters, 2 + defaultFilterSize - 1, "switcher", "access");
    }

    private void resetUrlFilter(URL url) throws Exception {
        url.removeParameter(URLParamType.filter.getName());
        getModifiableEnvironment().remove(ENV_GLOBAL_FILTERS);
        MotanGlobalConfigUtil.remove(ENV_GLOBAL_FILTERS);
        List<Filter> filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        assertEquals(defaultFilterSize, filters.size()); // default filters
    }

    private boolean checkFilter(List<Filter> filters, int size, String contains, String notContains) {
        assertEquals(size, filters.size());
        boolean isContains = false;
        for (Filter filter : filters) {
            SpiMeta meta = filter.getClass().getAnnotation(SpiMeta.class);
            assertFalse(notContains.equals(meta.name()));
            if (contains.equals(meta.name())) {
                isContains = true;
            }
        }
        assertTrue(isContains);
        return true;
    }

    public class MockProtocol implements Protocol {
        Provider<?> provider;

        @Override
        public <T> Exporter<T> export(Provider<T> provider, URL url) {
            this.provider = provider;
            return null;
        }

        @Override
        public <T> Referer<T> refer(Class<T> clz, URL url, URL serviceUrl) {
            return new MockReferer<>(url);
        }

        @Override
        public void destroy() {
        }
    }
}