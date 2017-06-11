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

import java.util.Map;

import javax.servlet.ServletContext;

import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.jboss.resteasy.spi.ResteasyDeployment;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.restful.RestServer;

@SuppressWarnings("unchecked")
public class ServletRestServer implements RestServer {
	private static ServletContext servletContext;

	public static void setServletContext(ServletContext servletContext) {
		ServletRestServer.servletContext = servletContext;
	}

	public void checkEnv() {
		if (servletContext == null) {
			throw new MotanFrameworkException("please config <listener-class>"
					+ RestfulServletContainerListener.class.getName() + "</listener-class> in your web.xml file");
		}

		if (getDeployment() == null) {
			throw new MotanFrameworkException("please config <servlet>" + HttpServletDispatcher.class.getName()
					+ "</servlet> and <load-on-start>1</load-on-start> in your web.xml file");
		}
	}

	@Override
	public void start() {
	}

	@Override
	public ResteasyDeployment getDeployment() {
		ResteasyDeployment deployment = (ResteasyDeployment) servletContext
				.getAttribute(ResteasyDeployment.class.getName());
		if (deployment == null) {
			// 在ListenerBootstrap中创建，key为servletMappingPrefix
			Map<String, ResteasyDeployment> deployments = (Map<String, ResteasyDeployment>) servletContext
					.getAttribute(ResteasyContextParameters.RESTEASY_DEPLOYMENTS);
			if (deployments != null && !deployments.isEmpty()) {
				deployment = deployments.values().iterator().next();
			}
		}

		return deployment;
	}

	@Override
	public void stop() {
	}

}
