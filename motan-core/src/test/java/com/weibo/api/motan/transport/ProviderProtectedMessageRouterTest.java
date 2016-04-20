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

import com.weibo.api.motan.rpc.DefaultProvider;
import com.weibo.api.motan.rpc.Provider;
import com.weibo.api.motan.rpc.URL;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-18
 * 
 */
public class ProviderProtectedMessageRouterTest extends TestCase {

    @Test
    public void testIsAllowRequest() {
        ProviderProtectedMessageRouter providerMessageRouter = new ProviderProtectedMessageRouter();

        Provider<ProviderA> providerA =
                new DefaultProvider<ProviderA>(new A(), new URL("injvm", "localhost", 0, ProviderA.class.getName()), ProviderA.class);

        Provider<ProviderB> providerB =
                new DefaultProvider<ProviderB>(new B(), new URL("injvm", "localhost", 0, ProviderB.class.getName()), ProviderB.class);

        Provider<ProviderC> providerC =
                new DefaultProvider<ProviderC>(new C(), new URL("injvm", "localhost", 0, ProviderC.class.getName()), ProviderC.class);

        providerMessageRouter.addProvider(providerA);

        int maxThread = 40;

        for (int i = 1; i <= maxThread; i++) {
            Assert.assertTrue(providerMessageRouter.isAllowRequest(i, i, maxThread, null));
        }

        providerMessageRouter.addProvider(providerB);

        for (int i = 1; i <= (maxThread * 3 / 4); i++) {
            Assert.assertTrue(providerMessageRouter.isAllowRequest(i, i, maxThread, null));
        }

        Assert.assertTrue(providerMessageRouter.isAllowRequest(maxThread / 2, maxThread * 3 / 4 + 1, maxThread, null));
        Assert.assertFalse(providerMessageRouter.isAllowRequest(maxThread / 2 + 1, maxThread * 3 / 4 + 1, maxThread, null));

        providerMessageRouter.removeProvider(providerB);
        providerMessageRouter.addProvider(providerC);

        for (int i = 1; i <= (maxThread * 3 / 4); i++) {
            Assert.assertTrue(providerMessageRouter.isAllowRequest(i, i, maxThread, null));
        }

        Assert.assertTrue(providerMessageRouter.isAllowRequest(maxThread / 4, maxThread * 3 / 4 + 1, maxThread, null));
        Assert.assertFalse(providerMessageRouter.isAllowRequest(maxThread / 4 + 1, maxThread * 3 / 4 + 1, maxThread, null));
    }
}
