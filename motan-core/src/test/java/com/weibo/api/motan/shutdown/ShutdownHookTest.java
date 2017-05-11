package com.weibo.api.motan.shutdown;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.motan.demo.client.DemoRpcClient;
import com.weibo.motan.demo.server.DemoRpcServer;
import com.weibo.motan.demo.server.MotanDemoServiceImpl;
import com.weibo.motan.demo.service.MotanDemoService;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by voyager.
 * Date: 2017/5/11
 * Time: 下午4:26
 */
public class ShutdownHookTest extends TestCase{
    private RefererConfig<MotanDemoService> refererConfig;
    private ServiceConfig<MotanDemoService> serviceConfig;

    @Before
    protected void setUp(){
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setId("testMotan");
        protocolConfig.setName("motan");
        protocolConfig.setCodec("motan");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("127.0.0.1");
        registryConfig.setPort(8002);

        serviceConfig = new ServiceConfig<MotanDemoService>();
        serviceConfig.setRef(new MotanDemoServiceImpl());
        serviceConfig.setInterface(MotanDemoService.class);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setExport("testMotan:8002");
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setShareChannel(true);

        serviceConfig.export();

        refererConfig = new RefererConfig<MotanDemoService>();
        refererConfig.setDirectUrl("127.0.0.1:8002");
        refererConfig.setProtocol(protocolConfig);
        refererConfig.setInterface(MotanDemoService.class);

        refererConfig.getRef();
    }

    @Test
    protected void TestShutDown(){

    }

}
