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

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.LoadBalance;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.protocol.example.IWorld;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * Failover ha strategy test.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-18
 */

public class FailoverHaStrategyTest extends BaseTestCase {

    private FailoverHaStrategy<IWorld> failoverHaStrategy;
    private List<Referer<IWorld>> referers = null;
    private LoadBalance<IWorld> loadBalance = null;
    private int retries = 2;

    @Before
    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        loadBalance = mockery.mock(LoadBalance.class);
        final Referer<IWorld> referer1 = mockery.mock(Referer.class, "ref1");
        final Referer<IWorld> referer2 = mockery.mock(Referer.class, "ref2");
        referers = new ArrayList<Referer<IWorld>>();
        referers.add(referer1);
        referers.add(referer2);
        failoverHaStrategy = new FailoverHaStrategy<IWorld>() {
            @Override
            protected List<Referer<IWorld>> selectReferers(Request request, LoadBalance<IWorld> loadBalance) {
                return referers;
            }
        };
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.LOCALHOST, 0, IWorld.class.getName());
        url.addParameter(URLParamType.retries.getName(), String.valueOf(retries));
        failoverHaStrategy.setUrl(url);
    }


    public void testCall() {
        final DefaultRequest request = new DefaultRequest();
        request.setMethodName(IWorld.class.getMethods()[0].getName());
        request.setArguments(new Object[] {});
        request.setInterfaceName(IHello.class.getSimpleName());
        request.setParamtersDesc("void");
        final Response response = mockery.mock(Response.class);
        final URL url =
                URL.valueOf("motan%3A%2F%2F10.209.128.244%3A8000%2Fcom.weibo.api.motan.protocol.example.IWorld%3Fprotocol%3Dmotan%26export%3Dmotan%3A8000%26application%3Dapi%26module%3Dtest%26check%3Dtrue%26refreshTimestamp%3D1373275099717%26methodconfig.world%28void%29.retries%3D1%26id%3Dmotan%26methodconfig.world%28java.lang.String%29.retries%3D1%26methodconfig.world%28java.lang.String%2Cboolean%29.retries%3D1%26nodeType%3Dservice%26group%3Dwangzhe-test-yf%26shareChannel%3Dtrue%26&");

        mockery.checking(new Expectations() {
            {
                // one(loadBalance).selectToHolder(request,
                // failoverHaStrategy.referersHolder.get());
                for (Referer<IWorld> ref : referers) {
                    atLeast(0).of(ref).call(request);
                    will(returnValue(response));
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                    atLeast(0).of(ref).getUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                }

                one(referers.get(0)).call(request);
                will(returnValue(response));
            }
        });
        failoverHaStrategy.call(request, loadBalance);
    }

    public void testCallWithOneError() {
        final DefaultRequest request = new DefaultRequest();
        request.setMethodName(IWorld.class.getMethods()[0].getName());
        request.setArguments(new Object[] {});
        request.setInterfaceName(IHello.class.getSimpleName());
        request.setParamtersDesc("void");
        final Response response = mockery.mock(Response.class);
        final URL url =
                URL.valueOf("motan%3A%2F%2F10.209.128.244%3A8000%2Fcom.weibo.api.motan.protocol.example.IWorld%3Fprotocol%3Dmotan%26export%3Dmotan%3A8000%26application%3Dapi%26module%3Dtest%26check%3Dtrue%26refreshTimestamp%3D1373275099717%26methodconfig.world%28void%29.retries%3D1%26id%3Dmotan%26methodconfig.world%28java.lang.String%29.retries%3D1%26methodconfig.world%28java.lang.String%2Cboolean%29.retries%3D1%26nodeType%3Dservice%26group%3Dwangzhe-test-yf%26shareChannel%3Dtrue%26&");

        mockery.checking(new Expectations() {
            {
                for (Referer<IWorld> ref : referers) {
                    atLeast(0).of(ref).call(request);
                    will(returnValue(response));
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                    atLeast(0).of(ref).getUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                }
                one(referers.get(0)).call(request);
                will(throwException(new MotanServiceException("mock throw exception when call")));
                one(referers.get(1)).call(request);
                will(returnValue(response));
            }
        });

        failoverHaStrategy.call(request, loadBalance);
    }

    public void testCallWithFalse() {
        final DefaultRequest request = new DefaultRequest();
        request.setMethodName(IWorld.class.getMethods()[0].getName());
        request.setArguments(new Object[] {});
        request.setInterfaceName(IHello.class.getSimpleName());
        request.setParamtersDesc("void");
        final Response response = mockery.mock(Response.class);
        final URL url =
                URL.valueOf("motan%3A%2F%2F10.209.128.244%3A8000%2Fcom.weibo.api.motan.protocol.example.IWorld%3Fprotocol%3Dmotan%26export%3Dmotan%3A8000%26application%3Dapi%26module%3Dtest%26check%3Dtrue%26refreshTimestamp%3D1373275099717%26methodconfig.world%28void%29.retries%3D1%26id%3Dmotan%26methodconfig.world%28java.lang.String%29.retries%3D1%26methodconfig.world%28java.lang.String%2Cboolean%29.retries%3D1%26nodeType%3Dservice%26group%3Dwangzhe-test-yf%26shareChannel%3Dtrue%26&");

        mockery.checking(new Expectations() {
            {
                one(loadBalance).selectToHolder(request, failoverHaStrategy.referersHolder.get());
                for (Referer<IWorld> ref : referers) {
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                    atLeast(0).of(ref).getUrl();
                    will(returnValue(url));
                    atLeast(0).of(ref).destroy();
                }
                atLeast(2).of(referers.get(0)).call(request);
                will(throwException(new MotanServiceException("mock throw exception when 1th call")));
                oneOf(referers.get(1)).call(request);
                will(throwException(new MotanServiceException("mock throw exception when 2th call")));
            }
        });

        try {
            failoverHaStrategy.call(request, loadBalance);
            fail("Should throw exception before!");
            Assert.assertTrue(false); // should not run to here
        } catch (Exception e) {}
    }
}
