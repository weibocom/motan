package com.weibo.api.motan.registry.consul;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.util.MotanSwitcherUtil;

/**
 * 
 * @Description ConsulHeartbeatManagerTest
 * @author zhanglei28
 * @date 2016年3月22日
 *
 */
public class ConsulHeartbeatManagerTest {
    private ConsulHeartbeatManager heartbeatManager;
    private MockConsulClient client;

    @Before
    public void setUp() throws Exception {
        client = new MockConsulClient("localhost", 8500);
        heartbeatManager = new ConsulHeartbeatManager(client);

        ConsulConstants.HEARTBEAT_CIRCLE = 200;
        ConsulConstants.SWITCHER_CHECK_CIRCLE = 20;
    }

    @After
    public void tearDown() throws Exception {
        heartbeatManager = null;
    }

    @Test
    public void testStart() throws InterruptedException {
        heartbeatManager.start();
        Map<String, Long> mockServices = new HashMap<String, Long>();
        int serviceNum = 5;

        for (int i = 0; i < serviceNum; i++) {
            String serviceid = "service" + i;
            mockServices.put(serviceid, 0L);
            heartbeatManager.addHeartbeatServcieId(serviceid);
        }

        // 打开心跳
        setHeartbeatSwitcher(true);
        checkHeartbeat(mockServices, true, serviceNum);

        // 关闭心跳
        setHeartbeatSwitcher(false);
        Thread.sleep(100);
        checkHeartbeat(mockServices, false, serviceNum);

    }

    private void checkHeartbeat(Map<String, Long> services, boolean start, int times) throws InterruptedException {
        // 检查times次心跳
        for (int i = 0; i < times; i++) {
            Thread.sleep(ConsulConstants.HEARTBEAT_CIRCLE + 500);
            for (Entry<String, Long> entry : services.entrySet()) {
                long heartbeatTimes = client.getCheckPassTimes(entry.getKey());
                long lastHeartbeatTimes = services.get(entry.getKey());
                services.put(entry.getKey(), heartbeatTimes);
                if (start) { // 心跳打开状态，心跳请求次数应该增加
                    assertTrue(heartbeatTimes > lastHeartbeatTimes);
                } else {// 心跳关闭时，心跳请求次数不应该在改变。
                    assertTrue(heartbeatTimes == lastHeartbeatTimes);
                }
            }
        }
    }

    public void setHeartbeatSwitcher(boolean value) {
        heartbeatManager.setHeartbeatOpen(value);

    }

}
