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

package com.weibo.api.motan.transport;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

import com.weibo.api.motan.TestConstants;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.rpc.DefaultProvider;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.ReflectUtil;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-18
 * 
 */
public class ProviderMessageRouterTest extends TestCase {
    private static final int PUBLIC_METHOD_COUNT_ALL = 3;
    private static final int PUBLIC_METHOD_COUNT_B = 2;

    @Test
    public void testPublicMethodCount() {
        Assert.assertEquals(ReflectUtil.getPublicMethod(A.class).size(), 1);
        Assert.assertEquals(ReflectUtil.getPublicMethod(B.class).size(), 2);
    }

    @Test
    public void testCall() {
        ProviderMessageRouter providerMessageRouter = new ProviderMessageRouter();

        Provider<ProviderA> providerA =
                new DefaultProvider<ProviderA>(new A(), new URL("injvm", "localhost", 0, ProviderA.class.getName()), ProviderA.class);

        Provider<ProviderB> providerB =
                new DefaultProvider<ProviderB>(new B(), new URL("injvm", "localhost", 0, ProviderB.class.getName()), ProviderB.class);

        providerMessageRouter.addProvider(providerA);
        providerMessageRouter.addProvider(providerB);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_ALL);

        DefaultRequest requestA = new DefaultRequest();
        requestA.setInterfaceName(com.weibo.api.motan.transport.ProviderA.class.getName());
        requestA.setMethodName("providerA");
        requestA.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        Response response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestA);
        Assert.assertEquals("A", response.getValue());

        DefaultRequest requestB = new DefaultRequest();
        requestB.setInterfaceName(com.weibo.api.motan.transport.ProviderB.class.getName());
        requestB.setMethodName("providerB");
        requestB.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestB);
        Assert.assertEquals("B", response.getValue());

        providerMessageRouter.removeProvider(providerA);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_B);

        try {
            Response result = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestA);
            result.getValue();
            Assert.assertTrue(false);
        } catch (Exception e) {
            Assert.assertTrue(true);
        }

    }
}


class A implements ProviderA {
    @Override
    public String providerA() {
        return this.getClass().getSimpleName();
    }
}


class B implements ProviderB, ProviderA {
    @Override
    public String providerB() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String providerA() {
        return this.getClass().getSimpleName();
    }
}


class C implements ProviderC {

    @Override
    public String providerA() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String providerB() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String providerC() {
        return this.getClass().getSimpleName();
    }

}
