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

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.ha.FailoverHaStrategy;
import com.weibo.api.motan.cluster.loadbalance.RandomLoadBalance;
import com.weibo.api.motan.cluster.support.ClusterSpi;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StringTools;
import junit.framework.Assert;
import org.jmock.Expectations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cluster test。
 *
 * @author fishermen
 * @version V1.0 created at: 2013-5-23
 */

public class ClusterTest extends BaseTestCase {

    private ClusterSpi<IHello> cluster = new ClusterSpi<>();
    private List<Referer<IHello>> referers;

    private static Map<String, Object> refererInfo;
    private URL url;


    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        HaStrategy<IHello> ha = new FailoverHaStrategy<>();
        LoadBalance<IHello> lb = new RandomLoadBalance<>();

        referers = new ArrayList<>();
        referers.add(mockery.mock(Referer.class, "ref1"));
        referers.add(mockery.mock(Referer.class, "ref2"));
        url = URL.valueOf(StringTools.urlDecode("motan%3A%2F%2F10.209.128.244%3A8000%2Fcom.weibo.api.motan.protocol.example.IWorld%3Fprotocol%3Dmotan%26export%3Dmotan%3A8000%26application%3Dapi%26module%3Dtest%26check%3Dtrue%26refreshTimestamp%3D1373275099717%26methodconfig.world%28void%29.retries%3D1%26id%3Dmotan%26methodconfig.world%28java.lang.String%29.retries%3D1%26methodconfig.world%28java.lang.String%2Cboolean%29.retries%3D1%26nodeType%3Dservice%26group%3Dwangzhe-test-yf%26shareChannel%3Dtrue%26&"));

        // add mock referer runtime info. it contains the common infos for cluster process.
        refererInfo = new HashMap<>();
        refererInfo.put(RuntimeInfoKeys.CODEC_KEY, "mockCodec");
        refererInfo.put(RuntimeInfoKeys.FUSING_THRESHOLD_KEY, 150);
        refererInfo.put("unKnownKey", "unknownValue");
        mockery.checking(new Expectations() {
            {
                for (Referer<IHello> ref : referers) {
                    atLeast(0).of(ref).getServiceUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                }
            }
        });

        cluster.setUrl(new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class
                .getName()));
        cluster.setHaStrategy(ha);
        cluster.setLoadBalance(lb);
        cluster.onRefresh(referers);
        cluster.init();
    }

    public void testCall() {
        final Request request = mockery.mock(Request.class);
        final Response rs = mockery.mock(Response.class);

        mockery.checking(new Expectations() {
            {
                allowing(any(Referer.class)).method("getUrl").withNoArguments();
                will(returnValue(new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 18080, Object.class
                        .getName())));
                allowing(any(Referer.class)).method("isAvailable").withNoArguments();
                will(returnValue(true));
                allowing(any(Referer.class)).method("call").with(same(request));
                will(returnValue(rs));
                allowing(any(Request.class)).method("getRequestId").withNoArguments();
                will(returnValue(0L));
                allowing(request).setAttachment(with(any(String.class)), with(any(String.class)));

                atLeast(0).of(request).setRetries(0);
                will(returnValue(null));
                atLeast(0).of(request).getRetries();
                will(returnValue(0));
                atLeast(0).of(request).getMethodName();
                will(returnValue("get"));
                atLeast(0).of(request).getParamtersDesc();
                will(returnValue("void"));
            }
        });

        Response callRs = cluster.call(request);
        Assert.assertEquals(rs, callRs);
    }

    @SuppressWarnings("unchecked")
    public void testRuntimeInfo() {

        int addSize = 15;
        for (int i = 0; i < addSize; i++) {
            referers.add(mockery.mock(Referer.class, "ref_runtime" + i));
        }

        mockery.checking(new Expectations() {
            {
                for (int i = 0; i < referers.size(); i++) {
                    Referer<?> ref = referers.get(i);
                    atLeast(0).of(ref).getServiceUrl();
                    will(returnValue(url));
                    atLeast(1).of(ref).destroy();
                    allowing(ref).getRuntimeInfo();
                    will(returnValue(new HashMap<>(refererInfo)));
                    allowing(ref).isAvailable();
                    will(returnValue(i % 2 == 0));
                    allowing(ref).getUrl();
                    will(returnValue(url));
                }
            }
        });

        Map<String, Object> info = cluster.getRuntimeInfo();
        assertEquals(cluster.getUrl().toFullStr(), info.get(RuntimeInfoKeys.URL_KEY));
        assertEquals(referers.size(), info.get(RuntimeInfoKeys.REFERER_SIZE_KEY));
        int refererSize = referers.size();
        int availableRefererSize = refererSize / 2 + (refererSize % 2 == 0 ? 0 : 1);
        // check available
        checkRefererInfo(info, availableRefererSize, RuntimeInfoKeys.AVAILABLE_KEY);
        // check unavailable
        checkRefererInfo(info, refererSize - availableRefererSize, RuntimeInfoKeys.UNAVAILABLE_KEY);

        // common info from referer
        assertEquals(refererInfo.get(RuntimeInfoKeys.CODEC_KEY), info.get(RuntimeInfoKeys.CODEC_KEY));
        assertEquals(refererInfo.get(RuntimeInfoKeys.FUSING_THRESHOLD_KEY), info.get(RuntimeInfoKeys.FUSING_THRESHOLD_KEY));
    }

    @SuppressWarnings("unchecked")
    private void checkRefererInfo(Map<String, Object> info, int expectSize, String refererKey) {
        Map<String, Map<String, String>> refererInfos = ((Map<String, Map<String, Map<String, String>>>) info.get(RuntimeInfoKeys.REFERERS_KEY)).get(refererKey);
        assertEquals(expectSize, refererInfos.size());
        for (Map<String, String> refererInfo : refererInfos.values()) {
            assertEquals(1, refererInfo.size()); // common infos are removed
        }
    }
}
