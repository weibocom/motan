package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.support.command.CommandListener;
import com.weibo.api.motan.registry.support.command.ServiceListener;
import com.weibo.api.motan.rpc.URL;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ConsulRegistryTest {
    private MockConsulClient client;
    private ConsulRegistry registry;
    private URL registerUrl;
    private URL serviceUrl, serviceUrl2, clientUrl, clientUrl2;
    private String serviceid, serviceid2;
    private long interval = 1000;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        registerUrl = new URL("motan", "localhost", 8500, "");
        registerUrl.addParameter(URLParamType.registrySessionTimeout.getName(), "" + interval);
        registry = new ConsulRegistry(registerUrl, client);

        serviceUrl = MockUtils.getMockUrl(8001);
        serviceUrl2 = MockUtils.getMockUrl(8002);
        serviceid = ConsulUtils.convertConsulSerivceId(serviceUrl);
        serviceid2 = ConsulUtils.convertConsulSerivceId(serviceUrl2);
        clientUrl = MockUtils.getMockUrl("127.0.0.1", 0);
        clientUrl2 = MockUtils.getMockUrl("127.0.0.2", 0);

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
        registry.doRegister(serviceUrl2);
        Assert.assertTrue(client.isRegistered(serviceid));
        Assert.assertFalse(client.isWorking(serviceid));
        Assert.assertTrue(client.isRegistered(serviceid2));
        Assert.assertFalse(client.isWorking(serviceid2));

        // available
        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        Assert.assertTrue(client.isWorking(serviceid));
        Assert.assertTrue(client.isWorking(serviceid2));

        // unavailable
        registry.doUnavailable(null);
        Thread.sleep(sleepTime);
        Assert.assertFalse(client.isWorking(serviceid));
        Assert.assertFalse(client.isWorking(serviceid2));

        // unregister
        registry.doUnregister(serviceUrl);
        Assert.assertFalse(client.isRegistered(serviceid));
        Assert.assertTrue(client.isRegistered(serviceid2));
        registry.doUnregister(serviceUrl2);
        Assert.assertFalse(client.isRegistered(serviceid2));
    }

    private ServiceListener createNewServiceListener(final URL serviceUrl) {
        return new ServiceListener() {
            @Override
            public void notifyService(URL refUrl, URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };
    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = createNewServiceListener(serviceUrl);
        ServiceListener serviceListener2 = createNewServiceListener(serviceUrl);

        registry.subscribeService(clientUrl, serviceListener);
        registry.subscribeService(clientUrl2, serviceListener2);
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl, serviceListener));
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

        registry.doRegister(serviceUrl);
        registry.doRegister(serviceUrl2);
        registry.doAvailable(null);
        Thread.sleep(sleepTime);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(serviceUrl, clientUrl, serviceListener));
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

        registry.unsubscribeService(clientUrl2, serviceListener2);
        Assert.assertFalse(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

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