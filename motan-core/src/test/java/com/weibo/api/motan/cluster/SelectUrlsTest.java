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

import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.protocol.example.IHello;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.Protocol;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StringTools;
import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SelectUrlsTest {

    private static JUnit4Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static ClusterSupportMask<IHello> clusterSupport;
    private static Protocol protocol = mockery.mock(Protocol.class);
    private static Map<String, Registry> registries = new HashMap<>();
    private static String regProtocol1 = "reg_1";
    private static String regProtocol2 = "reg_2";
    private static int count = 10;

    private static List<URL> mockRegistryUrls() {
        URL refUrl = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, IHello.class.getName());
        refUrl.addParameter(URLParamType.check.getName(), "false");
        refUrl.addParameter(URLParamType.clientConnectionCount.getName(), String.valueOf(count * 2));

        URL url1 = new URL(regProtocol1, "192.168.1.1", 18081, RegistryService.class.getName());
        url1.addParameter(URLParamType.embed.getName(), StringTools.urlEncode(refUrl.toFullStr()));

        URL url2 = new URL(regProtocol2, "192.168.1.2", 8082, RegistryService.class.getName());
        url2.addParameter(URLParamType.embed.getName(), StringTools.urlEncode(refUrl.toFullStr()));

        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        urls.add(url2);
        return urls;
    }

    @BeforeClass
    public static void initCluster() {
        clusterSupport = new ClusterSupportMask<>(IHello.class, mockRegistryUrls());

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

    @Test
    public void testSelectUrlsAvgCount() {
        List<URL> urls = new ArrayList<>();
        int notifyCount = 60;
        urls.addAll(mockUrls(notifyCount, "group1"));
        urls.addAll(mockUrls(notifyCount, "group2"));
        int clientCount = 1000;
        List<URL> finalUrls = new ArrayList<>();
        for (int i = 0; i < clientCount; i++) {
            finalUrls.addAll(clusterSupport.selectUrls(clusterSupport.getUrl(), urls));
        }
        Map<String, List<URL>> result = finalUrls.stream().collect(Collectors.groupingBy(URL::toSimpleString));
        Assert.assertEquals(notifyCount * 2, result.size());
        int avgCount = clientCount * count / notifyCount;
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            Assert.assertTrue(entry.getValue().size() > avgCount - 50);
            Assert.assertTrue(entry.getValue().size() < avgCount + 50);
        }
    }

    @Test
    public void testSelectUrls() {
        List<URL> urls = mockUrls(count + 100, "group1");
        URL mockRegistryUrl = mockRegistryUrls().get(0);
        boolean success = false;
        Set<URL> result = new HashSet<>();
        int[] arr = new int[]{1, 10, 100};
        for (int cnt : arr) {
            clusterSupport.activeUrlsMap.clear();
            for (int i = 0; i < 1000; i++) {
                int expect = count + cnt;
                result.addAll(clusterSupport.selectUrls(mockRegistryUrl, urls.subList(0, expect)));
                if (result.size() == expect) {
//                    System.out.println(i);
                    success = true;
                    break;
                }
            }
            Assert.assertTrue(success);
        }
    }

    private List<URL> mockUrls(int size, String group) {
        List<URL> urls = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            URL url = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), i, IHello.class.getName());
            url.addParameter(URLParamType.nodeType.getName(), MotanConstants.NODE_TYPE_SERVICE);
            url.addParameter(URLParamType.group.getName(), group);
            urls.add(url);
        }
        return urls;
    }

    private static class ClusterSupportMask<T> extends ClusterSupport<T> {
        public ConcurrentHashMap<URL, List<URL>> activeUrlsMap = super.registryActiveUrlsMap;

        public ClusterSupportMask(Class<T> interfaceClass, List<URL> registryUrls) {
            super(interfaceClass, registryUrls);
        }

        @Override
        public List<URL> selectUrls(URL registryUrl, List<URL> urls) {
            return super.selectUrls(registryUrl, urls);
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
}
