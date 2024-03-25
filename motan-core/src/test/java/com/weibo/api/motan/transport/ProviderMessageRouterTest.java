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
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.mock.MockChannel;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.runtime.GlobalRuntime;
import com.weibo.api.motan.util.MetaUtil;
import com.weibo.api.motan.util.ReflectUtil;
import com.weibo.api.motan.util.RequestIdGenerator;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Map;

/**
 * @author maijunsheng
 * @version 创建时间：2013-6-18
 */
public class ProviderMessageRouterTest extends TestCase {
    private static final int PUBLIC_METHOD_COUNT_ALL = 3;
    private static final int PUBLIC_METHOD_COUNT_B = 2;

    public void testPublicMethodCount() {
        Assert.assertEquals(ReflectUtil.getPublicMethod(A.class).size(), 1);
        Assert.assertEquals(ReflectUtil.getPublicMethod(B.class).size(), 2);
    }

    public void testCall() {
        ProviderMessageRouter providerMessageRouter = new ProviderMessageRouter();

        Provider<ProviderA> providerA =
                new DefaultProvider<>(new A(), new URL("injvm", "localhost", 0, ProviderA.class.getName()), ProviderA.class);

        Provider<ProviderB> providerB =
                new DefaultProvider<>(new B(), new URL("injvm", "localhost", 0, ProviderB.class.getName()), ProviderB.class);

        providerMessageRouter.addProvider(providerA);
        providerMessageRouter.addProvider(providerB);

        Assert.assertEquals(providerMessageRouter.getPublicMethodCount(), PUBLIC_METHOD_COUNT_ALL);

        DefaultRequest requestA = new DefaultRequest();
        requestA.setInterfaceName(com.weibo.api.motan.transport.ProviderA.class.getName());
        requestA.setMethodName("providerA");
        requestA.setParamtersDesc(ReflectUtil.EMPTY_PARAM);

        Response response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestA);
        Assert.assertEquals("A", response.getValue());

        // group not match
        requestA.setAttachment(URLParamType.group.getName(), "not_match");
        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), requestA);
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
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @SuppressWarnings("unchecked")
    public void testFrameworkProviders() {
        // test meta service
        GlobalRuntime.getDynamicMeta().clear();
        ProviderMessageRouter providerMessageRouter = new ProviderMessageRouter();
        Request request = MetaUtil.buildMetaServiceRequest();
        Response response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), request);
        assertEquals(GlobalRuntime.getDynamicMeta(), response.getValue());
        assertTrue(((Map<String, String>) response.getValue()).isEmpty());

        GlobalRuntime.putDynamicMeta("xyz", "zyx");
        response = (Response) providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), request);
        assertEquals(GlobalRuntime.getDynamicMeta(), response.getValue());
        assertEquals("zyx", ((Map<String, String>) response.getValue()).get("xyz"));

        // test unknown framework service
        DefaultRequest request2 = new DefaultRequest();
        request2.setRequestId(RequestIdGenerator.getRequestId());
        request2.setInterfaceName("unknownService");
        request2.setMethodName("unknownMethod");
        request2.setAttachment(MotanConstants.FRAMEWORK_SERVICE, "y"); // a framework service request
        try {
            providerMessageRouter.handle(new MockChannel(TestConstants.EMPTY_URL), request2);
            fail();
        } catch (MotanServiceException mse) {
            assertEquals(MotanErrorMsgConstant.SERVICE_NOT_SUPPORT_ERROR.getErrorCode(), mse.getErrorCode());
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
