/*
 *
 *   Copyright 2009-2016 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.registry.support.command;

import com.google.common.collect.Lists;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author zhanglei28
 * @date 2021/10/22.
 */
public class CommandServiceManagerTest {
    private static JUnit4Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    @Test
    public void testStaticCommandParse() {
        checkStaticCommand(null, false, 0);
        checkStaticCommand("group0", false, 0);
        checkStaticCommand("group1", true, 2);
        checkStaticCommand("group1 , group0", true, 2);
        checkStaticCommand("group1 , group2 ,group3", true, 4);
        checkStaticCommand("group1 , group2 ,group0, group3", true, 4);
    }

    private void checkStaticCommand(String mixGroups, boolean hasStaticCommand, int expectMergeGroupsSize) {
        URL refUrl = getDefaultUrl("group0", mixGroups);
        CommandServiceManager manager = new CommandServiceManager(refUrl);
        RpcCommand command = manager.getStaticCommand();
        if (hasStaticCommand) {
            assertNotNull("should has static command", command);
            assertEquals("merge group size", expectMergeGroupsSize, command.getClientCommandList().get(0).getMergeGroups().size());
        } else {
            assertNull("should not has static command", command);
        }
    }

    @Test
    public void notifyService() throws Exception {
        // without command
        checkNotifyService("testNotifyService-1", null, null, 2);
        // with static command
        checkNotifyService("testNotifyService-2", "group1", null, 5);
        // with dynamic command
        checkNotifyService("testNotifyService-3", null, RpcCommandUtil.commandToString(getDefaultSingleCommand(Lists.newArrayList("group0", "group1"), null)), 5);
    }

    private void checkNotifyService(String name, String mixGroups, String dynamicCommand, int expectSize) {
        URL refUrl = getDefaultUrl("group0", mixGroups);
        CommandServiceManager manager = getCommandServiceManagerForNotify(name, refUrl, expectSize, true);
        if (dynamicCommand != null) {
            manager.setCommandCache(dynamicCommand);
        }
        List<URL> notifyList = Lists.newArrayList(refUrl, refUrl);
        manager.notifyService(refUrl, refUrl, notifyList);
        Map<String, List<URL>> groupCache = manager.getGroupServiceCache();
        assertEquals("group result cache size", notifyList.size(), groupCache.get(refUrl.getGroup()).size());
        mockery.assertIsSatisfied();
    }

    private CommandServiceManager getCommandServiceManagerForNotify(String name, URL refUrl, int expectSize, boolean notify) {
        CommandFailbackRegistry registry = getDefaultRegistry(name, 3, refUrl, "group0", "group1", "group2");
        CommandServiceManager manager = new CommandServiceManager(refUrl);
        manager.setRegistry(registry);
        NotifyListener listener = mockery.mock(NotifyListener.class, name);
        if (notify) {
            mockery.checking(new Expectations() {
                {
                    oneOf(listener).notify(with(any(URL.class)), with(new BaseMatcher<List<URL>>() {
                                @Override
                                public boolean matches(Object o) {
                                    List<URL> list = (List<URL>) o;
                                    return list.size() == expectSize;
                                }

                                @Override
                                public void describeTo(Description description) {
                                }
                            })
                    );
                }
            });
        }
        manager.addNotifyListener(listener);
        return manager;
    }

    @Test
    public void notifyCommand() throws Exception {
        // 空指令
        checkNotifyCommand("checkNotifyCommand-1", null, 0, null, null, false, 0, false);
        // 空指令 + 静态指令
        checkNotifyCommand("checkNotifyCommand-2", "group1", 0, null, null, false, 0, false);

        List<String> groups = Lists.newArrayList("group0", "group1");
        checkNotifyCommand("checkNotifyCommand-3", null, groups.size(), null, getDefaultSingleCommand(groups, null), true, 0, false);
        checkNotifyCommand("checkNotifyCommand-4", "group1,group2", groups.size(), null, getDefaultSingleCommand(groups, null), true, 0, false);

        // 指令变更
        List<String> oldGroups = Lists.newArrayList("group0:2", "group1:3");
        List<String> newGroups = Lists.newArrayList("group2");
        checkNotifyCommand("checkNotifyCommand-5", null, newGroups.size(), getDefaultSingleCommand(oldGroups, null), getDefaultSingleCommand(newGroups, null), true, 2, false);
        checkNotifyCommand("checkNotifyCommand-6", "group1,group2", newGroups.size(), getDefaultSingleCommand(oldGroups, null), getDefaultSingleCommand(newGroups, null), true, 2, false);

        // 重复指令
        oldGroups = Lists.newArrayList("group0:2", "group1:3");
        newGroups = Lists.newArrayList("group0:2", "group1:3");
        checkNotifyCommand("checkNotifyCommand-7", null, newGroups.size(), getDefaultSingleCommand(oldGroups, null), getDefaultSingleCommand(newGroups, null), false, 0, false);
        checkNotifyCommand("checkNotifyCommand-8", "group1,group2", newGroups.size(), getDefaultSingleCommand(oldGroups, null), getDefaultSingleCommand(newGroups, null), false, 0, false);

        // 指令清空
        oldGroups = Lists.newArrayList("group1:2", "group2:3");
        checkNotifyCommand("checkNotifyCommand-9", null, 1, getDefaultSingleCommand(oldGroups, null), null, true, 3, true);
        checkNotifyCommand("checkNotifyCommand-10", "group1,group2", 3, getDefaultSingleCommand(oldGroups, null), null, true, 0, false);
    }

    /**
     * @param name        mock对象名
     * @param mixGroup    混打分组配置
     * @param groupSize   结果包含分组数
     * @param oldCommand  原指令
     * @param newCommand  新通知指令
     * @param notify      是否触发回调通知
     * @param unSubscribe 解订分组的调用次数
     * @param reSub       是否触发重新订阅自身分组
     */
    private void checkNotifyCommand(String name, String mixGroup, int groupSize, RpcCommand oldCommand, RpcCommand newCommand, boolean notify, int unSubscribe, boolean reSub) {
        URL refUrl = getDefaultUrl("group0", mixGroup);
        CommandServiceManager manager = getCommandServiceManagerForNotify(name, refUrl, groupSize * 3, notify);
        if (oldCommand != null) {
            manager.setCommandCache(RpcCommandUtil.commandToString(oldCommand));
            manager.discoverServiceWithCommand(new HashMap<>(), manager.getCommandCache());

        }
        mockery.checking(new Expectations() {
            {
                if (unSubscribe > 0) {
                    exactly(unSubscribe).of(manager.getRegistry()).unsubscribeService(with(any(URL.class)), with(any(ServiceListener.class)));
                }
            }
        });
        manager.notifyCommand(refUrl, RpcCommandUtil.commandToString(newCommand));
        assertEquals("dynamic command cache", RpcCommandUtil.commandToString(newCommand), RpcCommandUtil.commandToString(manager.getCommandCache()));
        assertEquals("group size", groupSize, manager.getGroupServiceCache().size());
        if (reSub) {
            assertEquals("group service cache size", 1, manager.getGroupServiceCache().size());
            assertNotNull(manager.getGroupServiceCache().get(refUrl.getGroup()));
        }
        mockery.assertIsSatisfied();
    }

    @Test
    public void discoverServiceWithCommand() throws Exception {
        checkWithMixGroups(false, false);
        checkWithMixGroups(true, false);
        checkWithMixGroups(false, true);
        checkWithMixGroups(true, true);
    }

    private void checkWithMixGroups(boolean withDynamicWeight, boolean withStaticCommand) {
        CommandFailbackRegistry registry = mockery.mock(CommandFailbackRegistry.class, "mockRegistry" + withDynamicWeight + withStaticCommand);
        URL refUrl = getDefaultUrl("group0", withStaticCommand ? "group1" : null);
        CommandServiceManager manager = new CommandServiceManager(refUrl);
        manager.setRegistry(registry);
        Map<String, Integer> weights = new HashMap<>();
        List<String> mergeGroups = new ArrayList<>();
        mergeGroups.add("group0" + (withDynamicWeight ? ":3" : ""));
        mergeGroups.add("group1" + (withDynamicWeight ? ":5" : ""));
        mergeGroups.add("group2" + (withDynamicWeight ? ":2" : ""));
        RpcCommand command = getDefaultSingleCommand(mergeGroups, null);

        mockery.checking(new Expectations() {
            {
                URL url1 = refUrl.createCopy();
                url1.addParameter(URLParamType.group.getName(), "group1");
                URL url2 = refUrl.createCopy();
                url2.addParameter(URLParamType.group.getName(), "group2");
                oneOf(registry).discoverService(refUrl);
                will(returnValue(Lists.newArrayList(refUrl, refUrl)));
                oneOf(registry).discoverService(url1);
                will(returnValue(Lists.newArrayList(url1, url1)));
                oneOf(registry).discoverService(url2);
                will(returnValue(Lists.newArrayList(url2, url2)));
                oneOf(registry).subscribeService(refUrl, manager);
                oneOf(registry).subscribeService(url1, manager);
                oneOf(registry).subscribeService(url2, manager);
            }
        });

        // with dynamic command
        int groupSize = mergeGroups.size();
        List<URL> result = manager.discoverServiceWithCommand(weights, command);
        assertEquals("result size with dynamic command", withDynamicWeight ? 2 * groupSize + 1 : 2 * groupSize, result.size());
        assertEquals("weight with dynamic command", groupSize, weights.size());
        if (withDynamicWeight) {
            assertEquals("weight rule url check", "rule", result.get(0).getProtocol());
            assertEquals("weight rule url check", "group0:3,group2:2,group1:5", result.get(0).getParameter(URLParamType.weights.getName()));
        } else {
            assertFalse("weight rule url check", "rule".equals(result.get(0).getProtocol()));
        }

        // non dynamic command
        weights = new HashMap<>();
        result = manager.discoverServiceWithCommand(weights, null);
        if (withStaticCommand) {
            assertEquals("result size with static command", 4, result.size());
            assertEquals("weight with static command", 2, weights.size());
        } else {
            assertEquals("result size without static command", 2, result.size());
            assertEquals("weight without static command", 0, weights.size());
        }
        mockery.assertIsSatisfied();
    }

    @Test
    public void testRouteRules() {
        URL refUrl = getDefaultUrl("group0", null);
        CommandFailbackRegistry registry = mockery.mock(CommandFailbackRegistry.class, "testRouteRules");
        String[] hosts = {"10.71.1.1", "10.71.2.1", "10.71.1.2", "10.72.1.1", "10.73.1.1", "10.73.2.2", "10.75.1.1"};
        String[] groups = {"group0", "group1", "group2"};
        mockery.checking(new Expectations() {
            {
                for (String group : groups) {
                    URL groupUrl = refUrl.createCopy();
                    groupUrl.addParameter(URLParamType.group.getName(), group);
                    List<URL> result = new ArrayList<>();
                    for (String host : hosts) {
                        URL url = groupUrl.createCopy();
                        url.setHost(host);
                        result.add(url);
                    }
                    allowing(registry).discoverService(groupUrl);
                    will(returnValue(result));
                }
                allowing(registry).subscribeService(with(any(URL.class)), with(any(ServiceListener.class)));
            }
        });
        CommandServiceManager manager = new CommandServiceManager(refUrl);
        manager.setRegistry(registry);

        checkRouteRules(manager, hosts, Lists.newArrayList("* to 10.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to 10.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.77.* to 10.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.77.1.* to 10.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.77.1.* to !10.*"), "10.77.1.1", 0, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.77.1.1 to 10.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.77.1.1 to 10.71.*"), "10.77.1.1", 3, new int[]{3, 4, 5, 6});
        checkRouteRules(manager, hosts, Lists.newArrayList("!10.77.1.1 to 10.71.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.88.1.1 to 10.71.*"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to 10.71.*"), "10.77.1.1", 3, new int[]{3, 4, 5, 6});
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to 10.71.1.*"), "10.77.1.1", 2, new int[]{1, 3, 4, 5, 6});
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to !10.71.1.*"), "10.77.1.1", 5, new int[]{0, 2});
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to 10.73.1.1"), "10.77.1.1", 1, new int[]{0, 1, 2, 3, 5, 6});
        checkRouteRules(manager, hosts, Lists.newArrayList("!10.* to 10.73.1.1"), "10.77.1.1", hosts.length, null);
        checkRouteRules(manager, hosts, Lists.newArrayList("10.* to !10.73.1.1"), "10.77.1.1", 6, new int[]{4});
        checkRouteRules(manager, hosts, Lists.newArrayList("* to !10.71.*"), "10.78.1.1", 4, new int[]{0, 1, 2});
    }

    private void checkRouteRules(CommandServiceManager manager, String[] hosts, List<String> routeRules, String localIp, int expectSize, int[] excludeIndex) {
        List<URL> result = manager.discoverServiceWithCommand(new HashMap<>(), getDefaultSingleCommand(null, routeRules), localIp);
        assertEquals("check route rules result size", expectSize, result.size());
        if (excludeIndex != null) {
            for (int index : excludeIndex) {
                for (URL url : result) {
                    assertFalse("should exclude url" + hosts[index], url.getHost().equals(hosts[index]));
                }
            }
        }
    }

    private URL getDefaultUrl(String group, String mixGroups) {
        URL url = new URL("motan", "127.0.0.1", 0, "testService");
        url.addParameter(URLParamType.group.getName(), group);
        url.addParameter(URLParamType.mixGroups.getName(), mixGroups);
        return url;
    }

    private RpcCommand getDefaultSingleCommand(List<String> mergeGroups, List<String> routeRules) {
        RpcCommand command = RpcCommandUtil.stringToCommand(RpcCommandUtilTest.TEST_COMMAND_STRING_1);
        command.getClientCommandList().get(0).setPattern("*");
        command.getClientCommandList().get(0).setMergeGroups(mergeGroups);
        command.getClientCommandList().get(0).setRouteRules(routeRules);
        return command;
    }

    private CommandFailbackRegistry getDefaultRegistry(String name, int notifySize, URL baseUrl, String... groups) {
        CommandFailbackRegistry registry = mockery.mock(CommandFailbackRegistry.class, "mockRegistry-" + name);
        mockery.checking(new Expectations() {
            {
                for (String group : groups) {
                    URL url = baseUrl.createCopy();
                    url.addParameter(URLParamType.group.getName(), group);
                    List<URL> result = new ArrayList<>();
                    for (int i = 0; i < notifySize; i++) {
                        result.add(url);
                    }
                    allowing(registry).discoverService(url);
                    will(returnValue(result));
                }
                allowing(registry).subscribeService(with(any(URL.class)), with(any(ServiceListener.class)));
                allowing(registry).getUrl();
                will(returnValue(new URL("testRegistry", "127.0.0.1", 0, "testRegistry")));
            }
        });
        return registry;
    }

}