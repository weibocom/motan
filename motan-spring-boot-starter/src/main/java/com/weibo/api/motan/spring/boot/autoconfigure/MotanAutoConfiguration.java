package com.weibo.api.motan.spring.boot.autoconfigure;

import com.weibo.api.motan.config.springsupport.BasicRefererConfigBean;
import com.weibo.api.motan.config.springsupport.BasicServiceConfigBean;
import com.weibo.api.motan.config.springsupport.ProtocolConfigBean;
import com.weibo.api.motan.config.springsupport.RegistryConfigBean;
import com.weibo.api.motan.spring.boot.autoconfigure.actuator.ActuatorAutoConfiguration;
import com.weibo.api.motan.spring.boot.autoconfigure.properties.MotanProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;

@Configuration
@EnableConfigurationProperties(MotanProperties.class)
@ConditionalOnClass(com.weibo.api.motan.rpc.Exporter.class)
@Import(ActuatorAutoConfiguration.class)
public class MotanAutoConfiguration {

    @Resource
    private MotanProperties motanProperties;

    @Bean
    @ConditionalOnMissingBean
    public RegistryConfigBean registryConfig() {
        return motanProperties.getRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProtocolConfigBean protocolConfig() {
        return motanProperties.getProtocol();
    }

    @Bean
    @ConditionalOnMissingBean
    public BasicServiceConfigBean serviceConfig(RegistryConfigBean registryConfig) {
        BasicServiceConfigBean serviceConfig = motanProperties.getService();
        serviceConfig.setRegistry(registryConfig);
        return serviceConfig;
    }

    @Bean
    @ConditionalOnMissingBean
    public BasicRefererConfigBean refererConfig(RegistryConfigBean registryConfig) {
        BasicRefererConfigBean refererConfig = motanProperties.getReferer();
        refererConfig.setRegistry(registryConfig);
        return refererConfig;
    }
}