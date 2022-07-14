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
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2022/6/24.
 */
public class ProtocolFilterDecoratorTest {
    ProtocolFilterDecorator protocolFilterDecorator;
    Protocol protocol;

    @Before
    public void setUp() throws Exception {
        protocol = new MockProtocol();
        protocolFilterDecorator = new ProtocolFilterDecorator(protocol);
    }

    @Test
    public void testGetFilters() {
        URL url = new URL("mock", "localhost", 7777, IWorld.class.getName());
        url.addParameter(URLParamType.filter.getName(), "statistic,switcher");
        List<Filter> filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_REFERER);
        assertEquals(3, filters.size());

        // test disable referer filter
        url.addParameter(URLParamType.filter.getName(), "statistic,-access");
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_REFERER);
        checkFilter(filters, 1, "statistic", "access");

        // test disable service filter
        url.addParameter(URLParamType.filter.getName(), "statistic,switcher,-access,-notExist");
        filters = protocolFilterDecorator.getFilters(url, MotanConstants.NODE_TYPE_SERVICE);
        checkFilter(filters, 2, "switcher", "access");
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