package com.weibo.api.motan.spring.boot.autoconfigure.actuator;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = {"org.springframework.boot.actuate.endpoint.AbstractEndpoint",
        "org.springframework.boot.actuate.health.AbstractHealthIndicator"})
public class ActuatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MotanEndpoint motanEndpoint() {
        return new MotanEndpoint();
    }

    @Bean
    @ConditionalOnMissingBean
    public MotanHealthIndicator motanHealthIndicator() {
        return new MotanHealthIndicator();
    }
}
