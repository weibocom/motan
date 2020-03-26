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
import com.weibo.api.motan.registry.RegistryService;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.NetUtils;
import com.weibo.api.motan.util.StringTools;
import junit.framework.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SelectUrlsTest {
    private static int count = 20;

    private static List<URL> mockRegistryUrls() {
        URL refUrl = new URL(MotanConstants.PROTOCOL_MOTAN, NetUtils.getLocalAddress().getHostAddress(), 0, IHello.class.getName());
        refUrl.addParameter(URLParamType.check.getName(), "false");
        refUrl.addParameter(URLParamType.maxConnectionPerGroup.getName(), String.valueOf(count * URLParamType.maxClientConnection.getIntValue()));

        URL url1 = new URL("reg_1", "192.168.1.1", 18081, RegistryService.class.getName());
        url1.addParameter(URLParamType.embed.getName(), StringTools.urlEncode(refUrl.toFullStr()));

        List<URL> urls = new ArrayList<>();
        urls.add(url1);
        return urls;
    }

    /**
     * 节点不变时，每次更新应返回固定的20个url
     */
    @Test
    public void testSelectUrls() {
        ClusterSupportMask<IHello> clusterSupport = new ClusterSupportMask<>(IHello.class, mockRegistryUrls());
        List<URL> urls = new ArrayList<>();
        int notifyCount = 60;
        urls.addAll(mockUrls(notifyCount, "group1"));
        urls.addAll(mockUrls(notifyCount, "group2"));
        List<URL> finalUrls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            finalUrls.addAll(clusterSupport.selectUrls(clusterSupport.getUrl(), urls));
        }
        Map<String, List<URL>> result = finalUrls.stream().collect(Collectors.groupingBy(URL::toSimpleString));
        Assert.assertEquals(count * 2, result.size());
    }

    /**
     * 新增节点时，验证每次更新后来自新增节点的数量是否符合预期
     */
    @Test
    public void testSelectUrls2() {
        int notifyCount = 100;
        checkCount(notifyCount, 60);
    }

    public void checkCount(int notifyCount, int split) {
        ClusterSupportMask<IHello> clusterSupport = new ClusterSupportMask<>(IHello.class, mockRegistryUrls());
        List<URL> urls = mockUrls(notifyCount, "group1");

        clusterSupport.selectUrls(clusterSupport.getUrl(), urls.subList(0, split));
        List<URL> selectUrls = new ArrayList<>();
        selectUrls.addAll(clusterSupport.selectUrls(clusterSupport.getUrl(), urls));
        List<URL> addedUrls = new ArrayList<>(selectUrls);
        addedUrls.retainAll(urls.subList(split, notifyCount));

        double p = (double)(notifyCount - split) / notifyCount;
        //假设认定新加入的url都有p的可能性被选中，即每个url是否被选中独立服从0-1分布
        //进而，根据中心极限定理可知下式 近似 服从标准正态分布
        double z = (addedUrls.size() - p * count) / (Math.sqrt((notifyCount - split) * p * (1 - p)));

        //查表知在n=40的情况下，如满足条件则|z| < 2.7045的可能性为99%。这样的话此用例将有<1%的概率失败
        Assert.assertTrue(Math.abs(z) < 2.7045);
    }

    /**
     * 删除节点时，每次更新应返回固定的20个url
     */
    @Test
    public void testSelectUrls3() {
        int notifyCount = 60;
        ClusterSupportMask<IHello> clusterSupport = new ClusterSupportMask<>(IHello.class, mockRegistryUrls());
        List<URL> urls = mockUrls(notifyCount, "group1");

        clusterSupport.selectUrls(clusterSupport.getUrl(), urls);
        List<URL> selectUrls = new ArrayList<>();
        selectUrls.addAll(clusterSupport.selectUrls(clusterSupport.getUrl(), urls.subList(30, notifyCount)));
        Assert.assertEquals(count, selectUrls.size());
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

        public ClusterSupportMask(Class<T> interfaceClass, List<URL> registryUrls) {
            super(interfaceClass, registryUrls);
        }

        @Override
        public List<URL> selectUrls(URL registryUrl, List<URL> urls) {
            return super.selectUrls(registryUrl, urls);
        }

    }
}
