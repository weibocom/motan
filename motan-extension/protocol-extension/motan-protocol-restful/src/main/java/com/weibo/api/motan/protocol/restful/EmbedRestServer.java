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

import org.jboss.resteasy.plugins.server.embedded.EmbeddedJaxrsServer;
import org.jboss.resteasy.spi.ResteasyDeployment;

/**
 * 内嵌resteasy server
 *
 * @author zhouhaocheng
 *
 */
public class EmbedRestServer implements RestServer {
    private EmbeddedJaxrsServer server;

    public EmbedRestServer(EmbeddedJaxrsServer server) {
        this.server = server;
    }

    @Override
    public ResteasyDeployment getDeployment() {
        return server.getDeployment();
    }

    @Override
    public void start() {
        server.start();
    }

    public void stop() {
        server.stop();
    }

}
