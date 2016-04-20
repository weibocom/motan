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

package com.weibo.api.motan.cluster;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.ha.FailoverHaStrategy;
import com.weibo.api.motan.cluster.loadbalance.RandomLoadBalance;
import com.weibo.api.motan.cluster.support.ClusterSpi;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;

/**
 * 
 * cluster spi test.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-28
 */

public class ClusterSpiTest extends BaseTestCase {

    private ClusterSpi<IHello> clusterSpi = new ClusterSpi<IHello>();
    private List<Referer<IHello>> referers;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testCall() {
        initCluster(true);
        final Request request = mockRequest();
        final Response response = mockery.mock(Response.class);
        final URL url = mockURL();
        mockery.checking(new Expectations() {
            {
                for (Referer<IHello> ref : referers) {
                    atLeast(0).of(ref).call(request);
                    will(returnValue(response));
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                    atLeast(0).of(ref).getUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                }
            }
        });
        clusterSpi.call(request);

        clusterSpi.destroy();

        try {
            clusterSpi.call(request);
            fail("Should not run to here!");
        } catch (Exception e) {
            assertTrue(e instanceof MotanServiceException);
        }
    }

    public void testSilentCall() {
        initCluster(false);
        final Request request = mockRequest();
        final URL url = mockURL();

        mockery.checking(new Expectations() {
            {
                for (Referer<IHello> ref : referers) {
                    atLeast(0).of(ref).call(request);
                    will(throwException(new IllegalStateException("Throw exception for test")));
                    atLeast(0).of(ref).isAvailable();
                    will(returnValue(true));
                    atLeast(0).of(ref).getUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                }
            }
        });

        clusterSpi.call(request);
        clusterSpi.destroy();

    }

    @SuppressWarnings("unchecked")
    private void initCluster(boolean throwException) {
        referers = new ArrayList<Referer<IHello>>();
        for (int i = 0; i < 10; i++) {
            referers.add(mockery.mock(Referer.class, "ref_" + i));
        }

        clusterSpi.setHaStrategy(new FailoverHaStrategy<IHello>());
        clusterSpi.setLoadBalance(new RandomLoadBalance<IHello>());
        URL url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName());
        url.addParameter(URLParamType.throwException.getName(), String.valueOf(throwException));
        url.addParameter(URLParamType.retries.getName(), "2");

        clusterSpi.setUrl(url);
        clusterSpi.onRefresh(referers);
        clusterSpi.init();
    }

    private Request mockRequest() {
        final DefaultRequest request = new DefaultRequest();
        request.setMethodName(IHello.class.getMethods()[0].getName());
        request.setArguments(new Object[] {});
        request.setInterfaceName(IHello.class.getSimpleName());
        request.setParamtersDesc("void");
        return request;
    }

    private URL mockURL() {
        return URL
                .valueOf("motan%3A%2F%2F10.209.128.244%3A8000%2Fcom.weibo.api.motan.protocol.example.IWorld%3Fprotocol%3Dmotan%26export%3Dmotan%3A8000%26application%3Dapi%26module%3Dtest%26check%3Dtrue%26refreshTimestamp%3D1373275099717%26methodconfig.world%28void%29.retries%3D1%26id%3Dmotan%26methodconfig.world%28java.lang.String%29.retries%3D1%26methodconfig.world%28java.lang.String%2Cboolean%29.retries%3D1%26nodeType%3Dservice%26group%3Dwangzhe-test-yf%26shareChannel%3Dtrue%26&");
    }

}
