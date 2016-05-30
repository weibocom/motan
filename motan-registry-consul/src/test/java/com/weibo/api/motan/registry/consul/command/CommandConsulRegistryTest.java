package com.weibo.api.motan.registry.consul.command;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.consul.ConsulConstants;
import com.weibo.api.motan.registry.consul.ConsulUtils;
import com.weibo.api.motan.registry.consul.MockConsulClient;
import com.weibo.api.motan.registry.consul.MockUtils;
import com.weibo.api.motan.registry.support.command.CommandListener;
import com.weibo.api.motan.registry.support.command.ServiceListener;
import com.weibo.api.motan.rpc.URL;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class CommandConsulRegistryTest {
    private MockConsulClient client;
    private CommandConsulRegistry registry;
    private URL registerUrl;
    private URL serviceUrl, clientUrl;
    private String serviceid;
    private long interval = 1000;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        registerUrl = new URL("motan", "localhost", 8500, "");
        registerUrl.addParameter(URLParamType.registrySessionTimeout.getName(), "" + interval);
        registry = new CommandConsulRegistry(registerUrl, client);

        serviceUrl = MockUtils.getMockUrl(8001);
        serviceid = ConsulUtils.convertConsulSerivceId(serviceUrl);
        clientUrl = MockUtils.getMockUrl(0);

        sleepTime = ConsulConstants.SWITCHER_CHECK_CIRCLE + 500;
    }

    @After
    public void tearDown() throws Exception {
        registry = null;
    }

    @Test
    public void doRegisterAndAvailable() throws Exception {
        // register
        registry.doRegister(serviceUrl);
        Assert.assertTrue(client.isRegistered(serviceid));
        Assert.assertFalse(client.isWorking(serviceid));

        // available
        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        Assert.assertTrue(client.isWorking(serviceid));

        // unavailable
        registry.doUnavailable(null);
        Thread.sleep(sleepTime);
        Assert.assertFalse(client.isWorking(serviceid));

        // unregister
        registry.doUnregister(serviceUrl);
        Assert.assertFalse(client.isRegistered(serviceid));
    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = new ServiceListener() {
            @Override
            public void notifyService(URL refUrl, URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };
        registry.subscribeService(clientUrl, serviceListener);
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl, serviceListener));
        registry.doRegister(serviceUrl);
        registry.doAvailable(null);
        Thread.sleep(sleepTime);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(serviceUrl, clientUrl, serviceListener));
    }

    @Test
    public void subAndUnsubCommand() throws Exception {
        final String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        CommandListener commandListener = new CommandListener() {
            @Override
            public void notifyCommand(URL refUrl, String commandString) {
                if (commandString != null) {
                    Assert.assertTrue(commandString.equals(command));
                }
            }
        };
        registry.subscribeCommand(clientUrl, commandListener);
        Assert.assertTrue(containsCommandListener(serviceUrl, clientUrl, commandListener));

        client.setKVValue(clientUrl.getGroup(), command);
        Thread.sleep(2000);

        client.removeKVValue(clientUrl.getGroup());

        registry.unsubscribeCommand(clientUrl, commandListener);
        Assert.assertFalse(containsCommandListener(serviceUrl, clientUrl, commandListener));
    }

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.discoverService(serviceUrl);
        Assert.assertFalse(urls.contains(serviceUrl));

        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        urls = registry.discoverService(serviceUrl);
        Assert.assertTrue(urls.contains(serviceUrl));
    }

    @Test
    public void discoverCommand() throws Exception {
        String result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(""));

        String command = "{\"index\":0,\"mergeGroups\":[\"aaa:1\",\"bbb:1\"],\"pattern\":\"*\",\"routeRules\":[]}\n";
        client.setKVValue(clientUrl.getGroup(), command);

        result = registry.discoverCommand(clientUrl);
        Assert.assertTrue(result.equals(command));
    }

    private Boolean containsServiceListener(URL serviceUrl, URL clientUrl, ServiceListener serviceListener) {
        String service = ConsulUtils.getUrlClusterInfo(serviceUrl);
        return registry.getServiceListeners().get(service).get(clientUrl) == serviceListener;
    }

    private Boolean containsCommandListener(URL serviceUrl, URL clientUrl, CommandListener commandListener) {
        String group = serviceUrl.getGroup();
        return registry.getCommandListeners().get(group).get(clientUrl) == commandListener;
    }

}