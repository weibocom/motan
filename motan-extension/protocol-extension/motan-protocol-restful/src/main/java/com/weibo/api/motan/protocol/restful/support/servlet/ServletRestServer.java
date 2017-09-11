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

import org.jboss.resteasy.spi.ResteasyDeployment;

import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.protocol.restful.RestServer;

public class ServletRestServer implements RestServer {
    private static ResteasyDeployment deployment;

    public static void setResteasyDeployment(ResteasyDeployment deployment) {
        ServletRestServer.deployment = deployment;
    }

    public void checkEnv() {
        if (deployment == null) {
            throw new MotanFrameworkException("please config <listener-class>"
                    + RestfulServletContainerListener.class.getName() + "</listener-class> in your web.xml file");
        }
    }

    @Override
    public void start() {
    }

    @Override
    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    @Override
    public void stop() {
    }

}
