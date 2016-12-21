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
package com.weibo.api.motan.filter.opentracing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.opentracing.Tracer;
//import io.opentracing.impl.BraveTracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.rpc.AbstractReferer;
import com.weibo.api.motan.rpc.DefaultProvider;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * @Description UT
 * @author zhanglei
 * @date Dec 9, 2016
 *
 */
public class OpenTracingFilterTest {
    Filter OTFilter;
    Tracer tracer;
    Referer<HelloService> refer;
    Provider<HelloService> provider;
    DefaultRequest request;
    DefaultResponse response;


    @Before
    public void setUp() throws Exception {
        OTFilter = new OpenTracingFilter();
        tracer = new MockTracer();
        OpenTracingContext.tracerFactory = new TracerFactory() {
            @Override
            public Tracer getTracer() {
                return tracer;
            }
        };
        URL url = new URL("motan", "localhost", 8002, "HelloService");
        request = new DefaultRequest();
        request.setInterfaceName("HelloService");
        request.setAttachment(URLParamType.group.name(), "test");
        request.setMethodName("sayHello");
        request.setParamtersDesc("java.lang.String");
        response = new DefaultResponse();
        refer = new AbstractReferer<HelloService>(HelloService.class, url) {
            @Override
            public void destroy() {}

            @Override
            public boolean isAvailable() {
                return true;
            }

            @Override
            protected Response doCall(Request request) {
                return response;
            }

            @Override
            protected boolean doInit() {
                return true;
            }
        };

        provider = new DefaultProvider<HelloService>(new HelloServiceImpl(), url, HelloService.class) {

            @Override
            public Response call(Request request) {
                return response;
            }

        };
    }

    @After
    public void tearDown() throws Exception {
        OpenTracingContext.tracerFactory = TracerFactory.DEFAULT;
    }

    @Test
    public void testRefererFilter() {
        Response res = OTFilter.filter(refer, request);
        assertEquals(response, res);
        checkMockTracer();

        // brave test must run with jdk1.8 
//        tracer = new BraveTracer();// use bravetracer
//        res = OTFilter.filter(refer, request);
//        assertEquals(response, res);
//        checkBraveTrace();
    }

    @Test
    public void testProviderFilter() {
        Response res = OTFilter.filter(provider, request);
        assertEquals(response, res);
        checkMockTracer();
    }

    @Test
    public void testException() {
        response.setException(new RuntimeException("in test"));
        Response res = OTFilter.filter(refer, request);
        assertEquals(response, res);
        if (tracer instanceof MockTracer) {
            MockSpan span = ((MockTracer) tracer).finishedSpans().get(0);
            assertEquals(span.logEntries().size(), 1);
            assertTrue("request fail.in test".equals(span.logEntries().get(0).fields().get("event")));
        }
    }

    private void checkMockTracer() {
        if (tracer instanceof MockTracer) {
            MockTracer mt = (MockTracer) tracer;
            assertEquals(1, mt.finishedSpans().size());
            MockSpan span = mt.finishedSpans().get(0);
            assertEquals(span.operationName(), "Motan_test_HelloService.sayHello(java.lang.String)");
            assertEquals(span.parentId(), 0);
            assertEquals(span.logEntries().size(), 1);
            assertTrue("request success.".equals(span.logEntries().get(0).fields().get("event")));
            assertTrue(span.tags().containsKey("requestId"));
        }
    }

//    private void checkBraveTrace() {
//        if (tracer instanceof BraveTracer) {
//            assertTrue(request.getAttachments().containsKey("X-B3-TraceId"));
//            assertTrue(request.getAttachments().containsKey("X-B3-SpanId"));
//            assertTrue(request.getAttachments().containsKey("X-B3-Sampled"));
//        }
//    }

}
