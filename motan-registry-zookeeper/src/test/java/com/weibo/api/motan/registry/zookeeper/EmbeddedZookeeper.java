package com.weibo.api.motan.registry.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class EmbeddedZookeeper {
    private static Properties properties = new Properties();

    static {
        InputStream in = EmbeddedZookeeper.class.getResourceAsStream("/zoo.cfg");
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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