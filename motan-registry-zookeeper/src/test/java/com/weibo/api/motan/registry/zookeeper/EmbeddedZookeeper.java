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

package com.weibo.api.motan.registry.zookeeper;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmbeddedZookeeper {
    private ZooKeeperServerMain zookeeperServer;
    private Thread t1;

    public void start() throws IOException, QuorumPeerConfig.ConfigException {
        Properties properties = new Properties();
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        properties.load(in);

        QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
        quorumConfiguration.parseProperties(properties);
        in.close();

        zookeeperServer = new ZooKeeperServerMain();
        final ServerConfig configuration = new ServerConfig();
        configuration.readFrom(quorumConfiguration);

        t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    zookeeperServer.runFromConfig(configuration);
                } catch (IOException e) {
                }
            }
        });
        t1.start();
    }
}