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

import com.weibo.api.motan.TestConstants;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.ReflectUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by chenxl on 2020/6/10.
 */
public class DefaultProtectedStrategyTest extends TestCase {

    private static final int PUBLIC_METHOD_COUNT_A = 1;
    private static final int PUBLIC_METHOD_COUNT_B = 2;
    private static final int PUBLIC_METHOD_COUNT_C = 3;
    private static final int MAX_WORKER_THREAD = 200;

    @Test
    public void testIsAllowRequest() {

        DefaultProtectedStrategy defaultProtectedStrategy = new DefaultProtectedStrategy();
        defaultProtectedStrategy.setMethodCounter(new AtomicInteger(PUBLIC_METHOD_COUNT_A));

        for (int i = 1; i <= MAX_WORKER_THREAD; i++) {
            Assert.assertTrue(defaultProtectedStrategy.isAllowRequest(i, i, MAX_WORKER_THREAD));
        }

        defaultProtectedStrategy.setMethodCounter(new AtomicInteger(PUBLIC_METHOD_COUNT_A + PUBLIC_METHOD_COUNT_B));

        for (int i = 1; i <= MAX_WORKER_THREAD; i++) {
            if (i <= (MAX_WORKER_THREAD * 3 / 4)) {
                Assert.assertTrue(defaultProtectedStrategy.isAllowRequest(i, i, MAX_WORKER_THREAD));
            } else {
                Assert.assertFalse(defaultProtectedStrategy.isAllowRequest(i, i, MAX_WORKER_THREAD));
            }
        }

        Assert.assertTrue(defaultProtectedStrategy.isAllowRequest(MAX_WORKER_THREAD / 2, MAX_WORKER_THREAD * 3 / 4 + 1, MAX_WORKER_THREAD));
        Assert.assertFalse(defaultProtectedStrategy.isAllowRequest(MAX_WORKER_THREAD / 2 + 1, MAX_WORKER_THREAD * 3 / 4 + 1, MAX_WORKER_THREAD));

        defaultProtectedStrategy.setMethodCounter(new AtomicInteger(PUBLIC_METHOD_COUNT_A + PUBLIC_METHOD_COUNT_C));

        for (int i = 1; i <= MAX_WORKER_THREAD; i++) {
            if (i <= (MAX_WORKER_THREAD * 3 / 4)) {
                Assert.assertTrue(defaultProtectedStrategy.isAllowRequest(i, i, MAX_WORKER_THREAD));
            } else {
                Assert.assertFalse(defaultProtectedStrategy.isAllowRequest(i, i, MAX_WORKER_THREAD));
            }
        }

        Assert.assertTrue(defaultProtectedStrategy.isAllowRequest(MAX_WORKER_THREAD / 4, MAX_WORKER_THREAD * 3 / 4 + 1, MAX_WORKER_THREAD));
        Assert.assertFalse(defaultProtectedStrategy.isAllowRequest(MAX_WORKER_THREAD / 4 + 1, MAX_WORKER_THREAD * 3 / 4 + 1, MAX_WORKER_THREAD));
    }

    @Test
    public void testCall() {
        DefaultProtectedStrategy defaultProtectedStrategy = new DefaultProtectedStrategy();
        defaultProtectedStrategy.setMethodCounter(new AtomicInteger(1));

        Provider<ProviderA> providerA =
                new DefaultProvider<ProviderA>(new ProviderAImpl(), new URL("injvm", "localhost", 0, ProviderA.class.getName()), ProviderA.class);

        DefaultRequest requestA = new DefaultRequest();
        requestA.setInterfaceName(com.weibo.api.motan.transport.ProviderA.class.getName());
        requestA.setMethodName("providerA");
        requestA.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        Response response = defaultProtectedStrategy.call(requestA, providerA);
        Assert.assertEquals(providerA.getImpl().providerA(), response.getValue());
    }

    @Test
    public void testHandle() {
        ProviderMessageRouter providerMessageRouter = new ProviderMessageRouter();

        Provider<ProviderA> providerA =
                new DefaultProvider<ProviderA>(new ProviderAImpl(), new URL("injvm", "localhost", 0, ProviderA.class.getName()), ProviderA.class);

        providerMessageRouter.addProvider(providerA);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_A);

        DefaultRequest requestA = new DefaultRequest();
        requestA.setInterfaceName(ProviderA.class.getName());
        requestA.setMethodName("providerA");
        requestA.setParamtersDesc(ReflectUtil.EMPTY_PARAM);
        Response response;

        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestA);
        Assert.assertEquals(providerA.getImpl().providerA(), response.getValue());


        Provider<ProviderB> providerB =
                new DefaultProvider<ProviderB>(new ProviderBImpl(), new URL("injvm", "localhost", 0, ProviderB.class.getName()), ProviderB.class);

        providerMessageRouter.addProvider(providerB);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_A + PUBLIC_METHOD_COUNT_B);

        DefaultRequest requestB = new DefaultRequest();
        requestB.setInterfaceName(ProviderB.class.getName());
        requestB.setMethodName("providerB");
        requestB.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestB);
        Assert.assertEquals(providerB.getImpl().providerB(), response.getValue());

        Provider<ProviderC> providerC =
                new DefaultProvider<ProviderC>(new ProviderCImpl(), new URL("injvm", "localhost", 0, ProviderC.class.getName()), ProviderC.class);

        providerMessageRouter.addProvider(providerC);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_A + PUBLIC_METHOD_COUNT_B + PUBLIC_METHOD_COUNT_C);

        DefaultRequest requestC = new DefaultRequest();
        requestC.setInterfaceName(ProviderC.class.getName());
        requestC.setMethodName("providerC");
        requestC.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestC);
        Assert.assertEquals(providerC.getImpl().providerC(), response.getValue());
    }
}

class ProviderAImpl implements ProviderA {

    @Override
    public String providerA() {
        return "hello " + this.getClass().getSimpleName();
    }

}


class ProviderBImpl implements ProviderB {

    @Override
    public String providerB() {
        return "hello " + this.getClass().getSimpleName();
    }

    @Override
    public String providerA() {
        return "hello " + this.getClass().getSimpleName();
    }

}


class ProviderCImpl implements ProviderC {

    @Override
    public String providerA() {
        return "hello " + this.getClass().getSimpleName();
    }

    @Override
    public String providerB() {
        return "hello " + this.getClass().getSimpleName();
    }

    @Override
    public String providerC() {
        return "hello " + this.getClass().getSimpleName();
    }

}