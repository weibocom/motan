package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author zhanglei28
 * @Description ConsulRegistryTest
 * @date 2016年3月22日
 */
public class ConsulRegistryTest {
    private MockConsulClient client;
    private ConsulRegistry registry;
    private URL registerUrl;
    private URL serviceUrl, clientUrl;
    private String serviceid;
    private long interval = 1000; // 设置查询间隔
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        registerUrl = new URL("motan", "localhost", 8500, "");
        registerUrl.addParameter(URLParamType.registrySessionTimeout.getName(), "" + interval);
        registry = new ConsulRegistry(registerUrl, client);

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
    public void doDiscover() throws InterruptedException {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.doDiscover(clientUrl);
        Assert.assertTrue(urls.isEmpty());

        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        urls = registry.doDiscover(clientUrl);
        Assert.assertTrue(urls.contains(serviceUrl));
    }

    @Test
    public void doRegisterAndAvailable() throws InterruptedException {
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
    public void doSubscribeAndUnsubscribe() throws InterruptedException {
        NotifyListener notifyListener = new NotifyListener() {
            @Override
            public void notify(URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };

        registry.doSubscribe(clientUrl, notifyListener);
        Assert.assertTrue(containsNotifyListener(clientUrl, notifyListener));

        registry.doRegister(serviceUrl);
        registry.doAvailable(null);
        Thread.sleep(sleepTime);

        registry.doUnsubscribe(clientUrl, notifyListener);
        Assert.assertFalse(containsNotifyListener(clientUrl, notifyListener));
    }

    private boolean containsNotifyListener(URL clientUrl, NotifyListener notifyListener) {
        String service = ConsulUtils.getUrlClusterInfo(clientUrl);
        return registry.getSubscribeListeners().get(service).get(clientUrl) == notifyListener;
    }

    @Test
    public void reRegister() {
        URL serviceUrl1 = MockUtils.getMockUrl(123);
        registry.doRegister(serviceUrl1);
        String serviceid1 = ConsulUtils.convertConsulSerivceId(serviceUrl1);
        assertTrue(client.isRegistered(serviceid1));

        URL serviceUrl2 = MockUtils.getMockUrl(456);
        String serviceid2 = ConsulUtils.convertConsulSerivceId(serviceUrl2);
        registry.doRegister(serviceUrl2);
        assertTrue(client.isRegistered(serviceid2));

        client.removeService(serviceid1);
        client.removeService(serviceid2);
        assertFalse(client.isRegistered(serviceid1));
        assertFalse(client.isRegistered(serviceid2));
        registry.reRegister();

        assertTrue(client.isRegistered(serviceid1));
        assertTrue(client.isRegistered(serviceid2));
    }

}
