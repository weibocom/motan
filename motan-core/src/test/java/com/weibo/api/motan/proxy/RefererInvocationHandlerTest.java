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

package com.weibo.api.motan.proxy;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.*;
import org.jmock.Expectations;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class RefererInvocationHandlerTest extends BaseTestCase {


    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testInvokeException() throws Throwable {
        final Cluster cluster = mockery.mock(Cluster.class);
        final URL u = new URL("motan", "local", 80, "test");
        u.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_REFERER);
        mockery.checking(new Expectations() {
            {
                one(cluster).call(with(any(Request.class)));
                will(throwException(new MotanBizException("just test", new StackOverflowError())));
                allowing(cluster).getUrl();
                will(returnValue(u));
            }
        });

        List<Cluster> clus = new ArrayList<Cluster>();
        clus.add(cluster);
        RefererInvocationHandler handler = new RefererInvocationHandler(String.class, clus);
        Method[] methods = String.class.getMethods();
        try {
            handler.invoke(null, methods[1], null);
        } catch (Exception e) {
            assertTrue(e instanceof MotanServiceException);
            assertTrue(e.getMessage().contains("StackOverflowError"));
        }

    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testLocalMehtod() throws Exception{
        final Cluster cluster = mockery.mock(Cluster.class);
        final URL u = new URL("motan", "local", 80, "test");
        final List<Referer> referers = new ArrayList<Referer>();
        final Referer referer = mockery.mock(Referer.class);
        referers.add(referer);
        mockery.checking(new Expectations() {
            {
                allowing(cluster).getUrl();
                will(returnValue(u));
                allowing(referer).getUrl();
                will(returnValue(u));
                allowing(referer).isAvailable();
                will(returnValue(true));
                allowing(cluster).getReferers();
                will(returnValue(referers));
            }
        });
        List<Cluster> clusters = new ArrayList<>();
        clusters.add(cluster);
        RefererInvocationHandler handler = new RefererInvocationHandler(TestService.class, clusters);
        //local method
        Method method = TestServiceImpl.class.getMethod("toString");
        assertTrue(handler.isLocalMethod(method));
        try {
            String result = (String)handler.invoke(null, method, null);
            assertEquals("{protocol:motan[motan://local:80/test?group=default_rpc, available:true]}", result);
        } catch (Throwable e) {
            assertTrue(false);
        }

        method = TestServiceImpl.class.getMethod("hashCode");
        assertTrue(handler.isLocalMethod(method));

        //remote method
        method = TestServiceImpl.class.getMethod("hello");
        assertFalse(handler.isLocalMethod(method));
        method = TestServiceImpl.class.getMethod("equals", Object.class);
        assertFalse(handler.isLocalMethod(method));


    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void testAsync() {
        final Cluster cluster = mockery.mock(Cluster.class);
        final URL u = new URL("motan", "local", 80, "test");
        u.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_REFERER);
        final ResponseFuture response = mockery.mock(ResponseFuture.class);
        mockery.checking(new Expectations() {
            {
                one(cluster).call(with(any(Request.class)));
                will(returnValue(response));
                allowing(cluster).getUrl();
                will(returnValue(u));
                allowing(response).setReturnType(with(any(Class.class)));
            }
        });

        List<Cluster> clus = new ArrayList<Cluster>();
        clus.add(cluster);
        RefererInvocationHandler handler = new RefererInvocationHandler(String.class, clus);
        Method method;
        try {
            method = TestService.class.getMethod("helloAsync", new Class<?>[] {});
            ResponseFuture res = (ResponseFuture) handler.invoke(null, method, null);
            assertEquals(response, res);
            assertTrue((Boolean) RpcContext.getContext().getAttribute(MotanConstants.ASYNC_SUFFIX));
        } catch (Throwable e) {
            e.printStackTrace();
            assertTrue(false);
        }

    }

    interface TestService {
        String hello();

        ResponseFuture helloAsync();

        boolean equals(Object o);
    }

    class TestServiceImpl implements TestService {
        @Override
        public String hello() {
            return "hello";
        }

        @Override
        public ResponseFuture helloAsync() {
            return null;
        }

    }

}
