/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
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

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.runtime.RuntimeInfoKeys;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zhanglei28
 * @date 2024/3/7.
 */
public class CommandFailbackRegistryTest extends TestCase {
    static List<URL> notifyList;

    @SuppressWarnings("unchecked")
    public void testRuntimeInfo() throws InterruptedException {
        URL registryUrl = new URL("command", "localhost", 0, "test/registry");
        registryUrl.addParameter(URLParamType.registryRetryPeriod.getName(), "50");
        TestCommandFailbackRegistry registry = new TestCommandFailbackRegistry(registryUrl);
        List<URL> defaultUrls = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            defaultUrls.add(new URL("motan", "localhost", 1111 + i, "test.Service"));
        }
        registry.setDefaultUrls(defaultUrls);

        // register/subscribe will fail
        URL serverUrl = new URL("motan", "localhost", 1111, "test.Service");
        serverUrl.addParameter(URLParamType.check.getName(), "false");
        registry.register(serverUrl);

        URL clientUrl = new URL("motan", "localhost", 0, "test.Service");
        clientUrl.addParameter(URLParamType.check.getName(), "false");
        registry.subscribe(clientUrl, (url, urls) -> notifyList = urls);

        Map<String, Object> runtimeInfo = registry.getRuntimeInfo();
        assertFalse(runtimeInfo.containsKey(RuntimeInfoKeys.SUBSCRIBE_INFO_KEY));
        checkCount(runtimeInfo, 0, 0, 1, 1);

        // register/subscribe successful
        registry.setAvailable(true);
        Thread.sleep(200); // > 2 * registryRetryPeriod
        runtimeInfo = registry.getRuntimeInfo();
        checkCount(runtimeInfo, 1, 1, 0, 0);

        // notify with command
        RpcCommand command = new RpcCommand();
        RpcCommand.ClientCommand clientCommand = new RpcCommand.ClientCommand();
        clientCommand.setIndex(1);
        clientCommand.setPattern("test.Service");
        clientCommand.setMergeGroups(Arrays.asList("testGroup", "testGroup2"));
        command.setClientCommandList(Collections.singletonList(clientCommand));
        String commmandString = RpcCommandUtil.commandToString(command);
        registry.notifyCommand(clientUrl, commmandString);
        Thread.sleep(10);
        runtimeInfo = registry.getRuntimeInfo();
        Map<String, Map<String, Object>> subInfo = (Map<String, Map<String, Object>>) runtimeInfo.get(RuntimeInfoKeys.SUBSCRIBE_INFO_KEY);
        assertEquals(1, subInfo.size());
        // check notify history
        Map<String, List<Object>> notifyHistory = (Map<String, List<Object>>) subInfo.get(clientUrl.getIdentity()).get(RuntimeInfoKeys.NOTIFY_HISTORY_KEY);
        assertEquals(1, notifyHistory.size());
        assertEquals(20, notifyHistory.values().iterator().next().size());

        // check command history
        Map<String, String> commandHistory = (Map<String, String>) subInfo.get(clientUrl.getIdentity()).get(RuntimeInfoKeys.COMMAND_HISTORY_KEY);
        assertEquals(1, commandHistory.size());

        // check command weight
        Map<String, Integer> weight = (Map<String, Integer>) subInfo.get(clientUrl.getIdentity()).get(RuntimeInfoKeys.WEIGHT_KEY);
        assertEquals(2, weight.size());

        // check command
        assertEquals(commmandString, (String) subInfo.get(clientUrl.getIdentity()).get(RuntimeInfoKeys.COMMAND_KEY));
    }

    @SuppressWarnings("unchecked")
    private void checkCount(Map<String, Object> runtimeInfo, int registerCount, int subscribeCount, int failedRegisterCount, int failedSubscribeCount) {
        assertEquals(registerCount, ((List<Object>) runtimeInfo.get(RuntimeInfoKeys.REGISTERED_SERVICE_URLS_KEY)).size());
        assertEquals(subscribeCount, ((Map<String, List<String>>) runtimeInfo.get(RuntimeInfoKeys.SUBSCRIBED_SERVICE_URLS_KEY)).size());
        if (failedRegisterCount > 0) {
            assertEquals(failedRegisterCount, ((List<Object>) runtimeInfo.get(RuntimeInfoKeys.FAILED_REGISTER_URLS_KEY)).size());
        } else {
            assertNull(runtimeInfo.get(RuntimeInfoKeys.FAILED_REGISTER_URLS_KEY));
        }
        if (failedSubscribeCount > 0) {
            assertEquals(failedSubscribeCount, ((List<Object>) runtimeInfo.get(RuntimeInfoKeys.FAILED_SUBSCRIBE_URLS_KEY)).size());
        } else {
            assertNull(runtimeInfo.get(RuntimeInfoKeys.FAILED_SUBSCRIBE_URLS_KEY));
        }
    }

    static class TestCommandFailbackRegistry extends CommandFailbackRegistry {
        AtomicBoolean isAvailable = new AtomicBoolean(false);
        List<URL> defaultUrls;
        HashMap<String, List<URL>> urlsCache = new HashMap<>();
        HashMap<String, ServiceListener> serviceListeners = new HashMap<>();
        HashMap<String, CommandListener> commandListeners = new HashMap<>();

        public void setAvailable(boolean available) {
            isAvailable.set(available);
        }

        public void setDefaultUrls(List<URL> urls) {
            this.defaultUrls = urls;
        }

        public void notifyUrls(URL url, List<URL> urls) {
            urlsCache.put(url.getIdentity(), urls);
            serviceListeners.get(url.getIdentity()).notifyService(url, super.getUrl(), urls);
        }

        public void notifyCommand(URL url, String command) {
            commandListeners.get(url.getIdentity()).notifyCommand(url, command);
        }

        public TestCommandFailbackRegistry(URL url) {
            super(url);
        }

        @Override
        protected void subscribeService(URL url, ServiceListener listener) {
            if (!isAvailable.get()) {
                throw new RuntimeException("unavailable");
            }
            serviceListeners.put(url.getIdentity(), listener);
        }

        @Override
        protected void subscribeCommand(URL url, CommandListener listener) {
            if (!isAvailable.get()) {
                throw new RuntimeException("unavailable");
            }
            commandListeners.put(url.getIdentity(), listener);
        }

        @Override
        protected void unsubscribeService(URL url, ServiceListener listener) {
            serviceListeners.remove(url.getIdentity());
        }

        @Override
        protected void unsubscribeCommand(URL url, CommandListener listener) {
            commandListeners.remove(url.getIdentity());
        }

        @Override
        protected List<URL> discoverService(URL url) {
            if (!isAvailable.get()) {
                throw new RuntimeException("unavailable");
            }
            if (urlsCache.get(url.getIdentity()) != null) {
                return urlsCache.get(url.getIdentity());
            }
            return defaultUrls;
        }

        @Override
        protected String discoverCommand(URL url) {
            return "";
        }

        @Override
        protected void doRegister(URL url) {
            if (!isAvailable.get()) {
                throw new RuntimeException("unavailable");
            }
        }

        @Override
        protected void doUnregister(URL url) {
            if (!isAvailable.get()) {
                throw new RuntimeException("unavailable");
            }
        }

        @Override
        protected void doAvailable(URL url) {

        }

        @Override
        protected void doUnavailable(URL url) {

        }
    }

}