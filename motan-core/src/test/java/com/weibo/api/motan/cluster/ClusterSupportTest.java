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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Protocol;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StringTools;

/**
 * 
 * ClusterSupport test.
 *
 * @author fishermen
 * @version V1.0 created at: 2013-6-22
 */

public class ClusterSupportTest {

    private static JUnit4Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static ClusterSupport<IHello> clusterSupport;
    private static Protocol protocol = mockery.mock(Protocol.class);
    private static Map<String, Registry> registries = new HashMap<String, Registry>();
    private static String regProtocol1 = "reg_1";
    private static String regProtocol2 = "reg_2";
    private static String localAddress = NetUtils.getLocalAddress().getHostAddress();
    private static Map<String, Referer<IHello>> portReferers = new HashMap<String, Referer<IHello>>();

    @Before
    public void initCluster() {
        clusterSupport = new ClusterSupportMask<IHello>(IHello.class, mockRegistryUrls());

        registries.put(regProtocol1, mockery.mock(Registry.class, regProtocol1));
        registries.put(regProtocol2, mockery.mock(Registry.class, regProtocol2));

        mockery.checking(new Expectations() {
            {
                allowing(any(Registry.class)).method("register").with(any(URL.class));
                allowing(any(Registry.class)).method("subscribe").with(any(URL.class), any(NotifyListener.class));
            }
        });

        clusterSupport.init();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testNotify() {

        // 先利用registry1 通知新增2个url
        final int urlsCount = 5;
        final List<URL> serviceUrls1 = new ArrayList<URL>();
        for (int i = 0; i < urlsCount; i++) {
            URL url = new URL(MotanConstants.PROTOCOL_MOTAN, localAddress, 1000 + i, IHello.class.getName());
            url.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
            serviceUrls1.add(url);
        }

        final URL reg1Url = new URL("reg_protocol_1", NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName());
        final URL reg2Url = new URL("reg_protocol_2", NetUtils.getLocalAddress().getHostAddress(), 0, RegistryService.class.getName());

        mockery.checking(new Expectations() {
            {
                for (int i = 0; i < urlsCount; i++) {
                    URL serviceUrl = serviceUrls1.get(i).createCopy();
                    URL refererUrl = serviceUrls1.get(i).createCopy();

                    String application = serviceUrl.getParameter(URLParamType.application.getName(), URLParamType.application.getValue());
                    String module = serviceUrl.getParameter(URLParamType.module.getName(), URLParamType.module.getValue());
                    refererUrl.addParameters(serviceUrl.getParameters());

                    refererUrl.addParameter(URLParamType.application.getName(), application);
                    refererUrl.addParameter(URLParamType.module.getName(), module);
                    refererUrl.addParameter(URLParamType.check.getName(), "false");

                    atLeast(1).of(protocol).refer(IHello.class, refererUrl, serviceUrl);
                    will(returnValue(mockReferer(refererUrl)));
                    atLeast(1).of(mockReferer(refererUrl)).getUrl();
                    will(returnValue(serviceUrls1.get(i)));
                    atLeast(1).of(mockReferer(refererUrl)).getServiceUrl();
                    will(returnValue(serviceUrls1.get(i)));
                }

                for (int i = 0; i < 3; i++) {
                    atLeast(1).of(mockReferer(serviceUrls1.get(i))).destroy();
                }

                atLeast(1).of(registries.get(regProtocol1)).getUrl();
                will(returnValue(reg1Url));
                atLeast(1).of(registries.get(regProtocol2)).getUrl();
                will(returnValue(reg2Url));
            }
        });
        List copy = new ArrayList<URL>();

        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(0, 2)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 2);

        // 再利用registry1 通知有三个
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(0, 3)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 3);

        // 再利用registr1 通知有0个
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(0, 0)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 3);

        // 再利用registr1 通知有2个，少了一个
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(1, 3)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 2);

        // 再利用registry1 通知有三个
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(0, 3)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 3);

        // 利用registry2，通知有2个
        clusterSupport.notify(registries.get(regProtocol2).getUrl(), copy(copy, serviceUrls1.subList(3, 5)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 5);

        // 再利用registr1 通知有2个，少了一个
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), copy(copy, serviceUrls1.subList(1, 3)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 4);

        // 再利用registr1 通知有registry1没有url了
        clusterSupport.notify(registries.get(regProtocol1).getUrl(), null);
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 2);

        List<Referer<IHello>> oldReferers = clusterSupport.getCluster().getReferers();

        // 利用registry2，通知有2个
        clusterSupport.notify(registries.get(regProtocol2).getUrl(), copy(copy, serviceUrls1.subList(3, 5)));
        Assert.assertEquals(clusterSupport.getCluster().getReferers().size(), 2);

        for (Referer<IHello> referer : clusterSupport.getCluster().getReferers()) {
            if (!oldReferers.contains(referer)) {
                Assert.assertTrue(false);
            }
        }

    }

    private static List<URL> mockRegistryUrls() {
        URL refUrl = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, IHello.class.getName());
        refUrl.addParameter(URLParamType.check.getName(), "false");

        URL url1 = new URL(regProtocol1, "192.168.1.1", 18081, RegistryService.class.getName());
        url1.addParameter(URLParamType.embed.getName(), StringTools.urlEncode(refUrl.toFullStr()));

        URL url2 = new URL(regProtocol2, "192.168.1.2", 8082, RegistryService.class.getName());
        url2.addParameter(URLParamType.embed.getName(), StringTools.urlEncode(refUrl.toFullStr()));

        List<URL> urls = new ArrayList<URL>();
        urls.add(url1);
        urls.add(url2);
        return urls;
    }

    private static class ClusterSupportMask<T> extends ClusterSupport<T> {
        public ClusterSupportMask(Class<T> interfaceClass, List<URL> registryUrls) {
            super(interfaceClass, registryUrls);
        }

        @Override
        protected Protocol getDecorateProtocol(String protocolName) {
            return protocol;
        }

        @Override
        protected Registry getRegistry(URL url) {
            return registries.get(url.getProtocol());
        }

    }

    @SuppressWarnings("unchecked")
    private synchronized Referer<IHello> mockReferer(URL url) {
        if (portReferers.get(url.getIdentity()) != null) {
            return portReferers.get(url.getIdentity());
        }
        portReferers.put(url.getIdentity(), mockery.mock(Referer.class, url.getIdentity()));
        return portReferers.get(url.getIdentity());

    }

    private List<URL> copy(List<URL> dest, List<URL> source) {
        dest.clear();
        for (URL url : source) {
            dest.add(url.createCopy());
        }
        return dest;
    }
}
