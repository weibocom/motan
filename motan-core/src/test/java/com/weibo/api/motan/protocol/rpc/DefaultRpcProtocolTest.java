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

package com.weibo.api.motan.protocol.rpc;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.example.Hello;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.rpc.*;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

/**
 * @author maijunsheng
 * @version 创建时间：2013-5-23
 */
public class DefaultRpcProtocolTest {

    private DefaultRpcProtocol defaultRpcProtocol;
    private URL url;

    @Before
    public void setUp() {
        defaultRpcProtocol = new DefaultRpcProtocol();
    }

    @Test
    public void testProtocol() {
        url = new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
        url.getParameters().put(URLParamType.endpointFactory.getName(), "mockEndpoint");
        try {
            defaultRpcProtocol.export(null, null);
        } catch (Exception e) {
            if (e instanceof MotanFrameworkException) {
                Assert.assertTrue(e.getMessage().contains("url is null"));
            } else {
                Assert.assertTrue(false);
            }
        }
        try {
            defaultRpcProtocol.export(null, url);
        } catch (Exception e) {
            if (e instanceof MotanFrameworkException) {
                Assert.assertTrue(e.getMessage().contains("provider is null"));
            } else {
                Assert.assertTrue(false);
            }
        }

        defaultRpcProtocol.export(new Provider<IHello>() {
        	private IHello hello = new Hello();
            @Override
            public Response call(Request request) {
                hello.hello();
                return new DefaultResponse("hello");
            }

            @Override
            public void init() {
            }

            @Override
            public void destroy() {
            }

            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public String desc() {
                return null;
            }

            @Override
            public URL getUrl() {
                return new URL("motan", "localhost", 18080, "com.weibo.api.motan.procotol.example.IHello");
            }

            @Override
            public Class<IHello> getInterface() {
                return IHello.class;
            }

            @Override
            public Method lookupMethod(String methodName, String methodDesc) {
                return null;
            }

			@Override
			public IHello getImpl() {
				return hello;
			}

        }, url);

        Referer<IHello> referer = defaultRpcProtocol.refer(IHello.class, url);

        DefaultRequest request = new DefaultRequest();
        request.setMethodName("hello");
        request.setInterfaceName(IHello.class.getName());

        Response response = referer.call(request);

        System.out.println("client: " + response.getValue());

        defaultRpcProtocol.destroy();
    }


}
