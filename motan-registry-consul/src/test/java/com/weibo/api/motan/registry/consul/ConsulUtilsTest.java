package com.weibo.api.motan.registry.consul;

import com.weibo.api.motan.rpc.URL;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author zhanglei28
 * @Description ConsulUtilsTest
 * @date 2016年3月22日
 */
public class ConsulUtilsTest {
    String testGroup;
    String testPath;
    String testHost;
    String testProtocol;
    int testPort;
    URL url;


    String testServiceName;
    String testServiceId;
    String testServiceTag;

    @Before
    public void setUp() throws Exception {
        testGroup = "yf-rpc-core";
        testServiceName = "motanrpc_yf-rpc-core";
        testPath = "com.weibo.motan.test.junit.TestService";
        testHost = "127.0.0.1";
        testPort = 8888;
        testProtocol = "motan";
        url = new URL(testProtocol, testHost, testPort, testPath);
        testServiceId = testHost + ":" + testPort + "-" + testPath;
        testServiceTag = ConsulConstants.CONSUL_TAG_MOTAN_PROTOCOL + ":" + testProtocol;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testConvertGroupToServiceName() {
        String tempServiceName = ConsulUtils.convertGroupToServiceName(testGroup);
        assertTrue(testServiceName.equals(tempServiceName));
    }

    @Test
    public void testGetGroupFromServiceName() {
        String tempGroup = ConsulUtils.getGroupFromServiceName(testServiceName);
        assertEquals(testGroup, tempGroup);
    }

    @Test
    public void testConvertConsulSerivceId() {
        String tempServiceId = ConsulUtils.convertConsulSerivceId(url);
        assertEquals(testServiceId, tempServiceId);
    }

    @Test
    public void testGetPathFromServiceId() {
        String tempPath = ConsulUtils.getPathFromServiceId(testServiceId);
        assertEquals(testPath, tempPath);
    }

}
