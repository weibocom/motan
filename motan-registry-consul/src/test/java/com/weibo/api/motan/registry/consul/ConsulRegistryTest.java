package com.weibo.api.motan.registry.consul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.rpc.URL;

/**
 * 
 * @Description ConsulRegistryTest
 * @author zhanglei28
 * @date 2016年3月22日
 *
 */
public class ConsulRegistryTest {
    private MockConsulClient client;
    private ConsulRegistry consulRegistry;
    private URL registerUrl;
    private long interval = 1000; // 设置查询间隔

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        registerUrl = new URL("motan", "localhost", 8500, "");
        registerUrl.addParameter(URLParamType.registrySessionTimeout.getName(), "" + interval);
        consulRegistry = new ConsulRegistry(registerUrl, client);

    }

    @After
    public void tearDown() throws Exception {
        consulRegistry = null;
    }

    @Test
    public void testdoDiscover() {
        URL referUrl = MockUtils.getMockUrl(0);
        List<URL> serviceUrls = consulRegistry.doDiscover(referUrl);
        assertEquals(serviceUrls.size(), client.getMockServiceNum());
    }

    @Test
    public void testdoRegisterAndUnregister() {
        // register
        URL serviceUrl1 = MockUtils.getMockUrl(123);
        consulRegistry.doRegister(serviceUrl1);
        String serviceid1 = ConsulUtils.convertConsulSerivceId(serviceUrl1);
        assertTrue(client.isServiceRegister(serviceid1));

        URL serviceUrl2 = MockUtils.getMockUrl(456);
        String serviceid2 = ConsulUtils.convertConsulSerivceId(serviceUrl2);
        consulRegistry.doRegister(serviceUrl2);
        assertTrue(client.isServiceRegister(serviceid2));

        // unregister
        consulRegistry.doUnregister(serviceUrl1);
        assertFalse(client.isServiceRegister(serviceid1));
        assertTrue(client.isServiceRegister(serviceid2));

        consulRegistry.doUnregister(serviceUrl2);
        assertFalse(client.isServiceRegister(serviceid2));
    }



    @Test
    public void testdoSubscribeAndUnsubscribe() throws InterruptedException {

        URL referUrl = MockUtils.getMockUrl(0);
        NotifyListener listener = new NotifyListener() {

            @Override
            public void notify(URL registryUrl, List<URL> urls) {
                System.out.println("process notify。service size: " + urls.size());
                assertEquals(client.getMockServiceNum(), urls.size());
            }
        };

        consulRegistry.doSubscribe(referUrl, listener);
        Thread.sleep(200);

        long lastIndex = client.getMockIndex();
        long curIndex = 0;
        for (int i = 0; i < 5; i++) {
            client.setMockServiceNum(client.getMockServiceNum() + 1);
            Thread.sleep(interval + 100);
            curIndex = client.getMockIndex();
            assertTrue(curIndex > lastIndex);
            lastIndex = curIndex;
        }

    }

    @Test
    public void testReRegister() {
        URL serviceUrl1 = MockUtils.getMockUrl(123);
        consulRegistry.doRegister(serviceUrl1);
        String serviceid1 = ConsulUtils.convertConsulSerivceId(serviceUrl1);
        assertTrue(client.isServiceRegister(serviceid1));

        URL serviceUrl2 = MockUtils.getMockUrl(456);
        String serviceid2 = ConsulUtils.convertConsulSerivceId(serviceUrl2);
        consulRegistry.doRegister(serviceUrl2);
        assertTrue(client.isServiceRegister(serviceid2));

        client.removeService(serviceid1);
        client.removeService(serviceid2);
        assertFalse(client.isServiceRegister(serviceid1));
        assertFalse(client.isServiceRegister(serviceid2));
        consulRegistry.reRegister();

        assertTrue(client.isServiceRegister(serviceid1));
        assertTrue(client.isServiceRegister(serviceid2));
    }

}
