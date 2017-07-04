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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RefererConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.protocol.restful.HelloResource.User;
import com.weibo.api.motan.protocol.restful.support.servlet.RestfulServletContainerListener;

/**
 * servlet容器下restful协议测试
 *
 * @author zhouhaocheng
 *
 */
public class TestServletCotainerRestful {
    private Tomcat tomcat;

    private ServiceConfig<HelloResource> serviceConfig;
    private RefererConfig<HelloResource> refererConfig;
    private HelloResource resource;

    @Before
    public void setUp() throws Exception {
        tomcat = new Tomcat();
        String baseDir = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        tomcat.setBaseDir(baseDir);
        tomcat.setPort(8002);

        tomcat.getConnector().setProperty("URIEncoding", "UTF-8");
        tomcat.getConnector().setProperty("socket.soReuseAddress", "true");
        tomcat.getConnector().setProperty("connectionTimeout", "20000");

        String contextpath = "/cp";

        String servletPrefix = "/servlet";

        /**
         * <pre>
         *
         * <servlet>
         *  <servlet-name>dispatcher</servlet-name>
         *  <servlet-class>org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher</servlet-class>
         *  <load-on-startup>1</load-on-startup>
         *  <init-param>
         *    <param-name>resteasy.servlet.mapping.prefix</param-name>
         *    <param-value>/servlet</param-value>  <!-- 此处实际为servlet-mapping的url-pattern，具体配置见resteasy文档-->
         *  </init-param>
         * </servlet>
         *
         * <servlet-mapping>
         *   <servlet-name>dispatcher</servlet-name>
         *   <url-pattern>/servlet/*</url-pattern>
         * </servlet-mapping>
         *
         * </pre>
         */
        Context context = tomcat.addContext(contextpath, baseDir);
        Wrapper wrapper = Tomcat.addServlet(context, "dispatcher", HttpServletDispatcher.class.getName());
        wrapper.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, servletPrefix);
        wrapper.setLoadOnStartup(1);
        context.addServletMapping(servletPrefix + "/*", "dispatcher");

        /**
         * <listener>
         * <listener-class>com.weibo.api.motan.protocol.restful.support.servlet.RestfulServletContainerListener</listener-class>
         * </listener>
         */
        context.addApplicationListener(RestfulServletContainerListener.class.getName());

        tomcat.start();

        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setId("testRpc");
        protocolConfig.setName("restful");
        protocolConfig.setEndpointFactory("servlet");

        Map<String, String> ext = new HashMap<String, String>();
        ext.put("contextpath", contextpath + servletPrefix);
        protocolConfig.setParameters(ext);

        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setName("local");
        registryConfig.setAddress("127.0.0.1");
        registryConfig.setPort(0);

        serviceConfig = new ServiceConfig<HelloResource>();
        serviceConfig.setRef(new RestHelloResource());
        serviceConfig.setInterface(HelloResource.class);
        serviceConfig.setProtocol(protocolConfig);
        // 注意此处配置的端口并不会被使用，建议配置servlet服务器端口
        serviceConfig.setExport("testRpc:8003");
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
    public void tearDown() throws Exception {
        serviceConfig.unexport();
        refererConfig.destroy();

        tomcat.stop();
        tomcat.destroy();
    }

}
