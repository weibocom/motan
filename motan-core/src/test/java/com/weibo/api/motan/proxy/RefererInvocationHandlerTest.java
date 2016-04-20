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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.*;

import org.jmock.Expectations;
import org.junit.Test;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;

public class RefererInvocationHandlerTest extends BaseTestCase {


    @Test
    public void testInvokeException() throws Throwable {
        final Cluster cluster = mockery.mock(Cluster.class);
        final Request request = mockery.mock(Request.class);
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



}
