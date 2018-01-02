package com.weibo.api.motan.demo;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {AppLauncher.class})
public class AppLauncher {

    public static void main(String[] args) {
        System.setProperty("server.port", "9090");
        ConfigurableApplicationContext context = SpringApplication.run(AppLauncher.class, args);
        System.out.println("server start...");
    }

}
