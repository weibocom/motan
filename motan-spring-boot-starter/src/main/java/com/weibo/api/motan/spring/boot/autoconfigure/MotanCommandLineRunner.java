package com.weibo.api.motan.spring.boot.autoconfigure;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.spring.boot.autoconfigure.properties.MotanProperties;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;

public class MotanCommandLineRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MotanCommandLineRunner.class);

    private final MotanProperties properties;

    public MotanCommandLineRunner(MotanProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(String... args) throws Exception {
        String regProtocol = properties.getRegistry().getRegProtocol().toLowerCase();
        if (!regProtocol.equals(MotanConstants.REGISTRY_PROTOCOL_LOCAL)) {
            // 开启注册中心
            MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, true);
            log.info("regProtocol was {}, open heartbeat switcher", regProtocol);
        } else {
            log.info("regProtocol was local,  not need to open heartbeat switcher");
        }
    }

}