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
package com.weibo.api.motan.protocol.restful.support.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;

import com.weibo.api.motan.protocol.restful.support.RestfulInjectorFactory;
import com.weibo.api.motan.protocol.restful.support.RpcExceptionMapper;

public class RestfulServletContainerListener extends ResteasyBootstrap implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();

        servletContext.setInitParameter("resteasy.injector.factory", RestfulInjectorFactory.class.getName());
        servletContext.setInitParameter(ResteasyContextParameters.RESTEASY_PROVIDERS,
                RpcExceptionMapper.class.getName());

        super.contextInitialized(sce);

        ServletRestServer.setResteasyDeployment(deployment);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        super.contextDestroyed(sce);
    }

}
