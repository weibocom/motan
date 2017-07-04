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
package com.weibo.api.motan.protocol.restful;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.protocol.restful.HelloResource.User;

public class TestRestful {
    private ServiceConfig<HelloResource> serviceConfig;
    private RefererConfig<HelloResource> refererConfig;
    private HelloResource resource;

    @Before
    public void setUp() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setId("testRpc");
        protocolConfig.setName("restful");
        protocolConfig.setEndpointFactory("netty");

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("local");
        registryConfig.setAddress("127.0.0.1");
        registryConfig.setPort(0);

        serviceConfig = new ServiceConfig<HelloResource>();
        serviceConfig.setRef(new RestHelloResource());
        serviceConfig.setInterface(HelloResource.class);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setExport("testRpc:8002");
        serviceConfig.setFilter("serverf");
        serviceConfig.setGroup("test-group");
        serviceConfig.setVersion("0.0.3");
        serviceConfig.setRegistry(registryConfig);

        serviceConfig.export();

        refererConfig = new RefererConfig<HelloResource>();
        refererConfig.setDirectUrl("127.0.0.1:8002");
        refererConfig.setGroup("test-group");
        refererConfig.setVersion("0.0.3");
        refererConfig.setFilter("clientf");
        refererConfig.setProtocol(protocolConfig);
        refererConfig.setInterface(HelloResource.class);

        resource = refererConfig.getRef();
    }

    @Test
    public void testPrimitiveType() {
        Assert.assertEquals("helloworld", resource.testPrimitiveType());
    }

    @Test
    public void testCookie() {
        List<User> users = resource.hello(23);
        Assert.assertEquals(users.size(), 1);
        Assert.assertEquals(users.get(0).getId(), 23);
        Assert.assertEquals(users.get(0).getName(), "de");
    }

    @Test
    public void testReturnResponse() {
        Response resp = resource.add(2, "de");
        Assert.assertEquals(resp.getStatus(), Status.OK.getStatusCode());
        Assert.assertEquals(resp.getCookies().size(), 1);
        Assert.assertEquals(resp.getCookies().get("ck").getName(), "ck");
        Assert.assertEquals(resp.getCookies().get("ck").getValue(), "2");

        User user = resp.readEntity(User.class);
        resp.close();

        Assert.assertEquals(user.getId(), 2);
        Assert.assertEquals(user.getName(), "de");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testException() {
        resource.testException();
    }

    @After
    public void tearDown() {
        refererConfig.destroy();
        serviceConfig.unexport();
    }

}
