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

package com.weibo.api.motan.cluster.ha;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * Failfast ha strategy.
 * 
 * @author fishermen
 * @version V1.0 created at: 2013-6-18
 */

public class FailfastHaStrategyTest extends BaseTestCase {

    private FailfastHaStrategy<IWorld> failfastHaStrategy = new FailfastHaStrategy<IWorld>();

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.LOCALHOST, 0, IWorld.class.getName());
        failfastHaStrategy.setUrl(url);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCall() {
        final LoadBalance<IWorld> loadBalance = mockery.mock(LoadBalance.class);
        final Referer<IWorld> referer = mockery.mock(Referer.class);
        final Request request = mockery.mock(Request.class);
        final Response response = mockery.mock(Response.class);

        mockery.checking(new Expectations() {
            {
                one(loadBalance).select(request);
                will(returnValue(referer));
                one(referer).call(request);
                will(returnValue(response));
            }
        });

        assertEquals(response, failfastHaStrategy.call(request, loadBalance));
    }

    @Test(expected = MotanServiceException.class)
    @SuppressWarnings("unchecked")
    public void testCallError() {
        final LoadBalance<IWorld> loadBalance = mockery.mock(LoadBalance.class);
        final Referer<IWorld> referer = mockery.mock(Referer.class);
        final Request request = mockery.mock(Request.class);

        mockery.checking(new Expectations() {
            {
                one(loadBalance).select(request);
                will(returnValue(referer));
                one(referer).call(request);
                will(returnValue(null));
            }
        });

        assertEquals(null, failfastHaStrategy.call(request, loadBalance));
    }
}
