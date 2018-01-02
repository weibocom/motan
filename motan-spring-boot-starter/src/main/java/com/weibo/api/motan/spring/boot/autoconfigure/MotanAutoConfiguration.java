package com.weibo.api.motan.spring.boot.autoconfigure;

import com.weibo.api.motan.config.springsupport.*;
import com.weibo.api.motan.spring.boot.autoconfigure.actuator.ActuatorAutoConfiguration;
import com.weibo.api.motan.spring.boot.autoconfigure.properties.MotanBeanNames;
import com.weibo.api.motan.spring.boot.autoconfigure.properties.MotanProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@EnableConfigurationProperties(MotanProperties.class)
@ConditionalOnClass(com.weibo.api.motan.rpc.Exporter.class)
@Import(ActuatorAutoConfiguration.class)
public class MotanAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MotanAutoConfiguration.class);

    @Bean(name = MotanBeanNames.ANNOTATION_BEAN_NAME)
    @ConditionalOnMissingBean
    public static AnnotationBean annotationBean(@Value("${spring.motan.scanPackage}") String scanPackage) {
        AnnotationBean scan = new AnnotationBean();
        if (scanPackage != null && !scanPackage.isEmpty()) {
            scan.setPackage(scanPackage);
        } else {
            log.warn("scanPackage is empty, nothing to load");
        }
        return scan;
    }

    @Bean(name = MotanBeanNames.REGISTRY_CONFIG_BEAN_NAME)
    @ConditionalOnMissingBean
    public static RegistryConfigBean registryConfigBean(MotanProperties properties) {
        return properties.getRegistry();
    }

    @Bean(name = MotanBeanNames.PROTOCOL_CONFIG_BEAN_NAME)
    @ConditionalOnMissingBean
    public static ProtocolConfigBean protocolConfigBean(MotanProperties properties) {
        return properties.getProtocol();
    }

    @Bean(name = MotanBeanNames.SERVICE_CONFIG_BEAN_NAME)
    @ConditionalOnMissingBean
    public static BasicServiceConfigBean serviceConfigBean(RegistryConfigBean registryConfigBean,
                                                           MotanProperties properties, ProtocolConfigBean protocolConfigBean) {
        BasicServiceConfigBean bean = properties.getService();
        bean.setProtocol(protocolConfigBean);
        bean.setRegistry(registryConfigBean);
        return bean;
    }

    @Bean(name = MotanBeanNames.REFERER_CONFIG_BEAN_NAME)
    @ConditionalOnMissingBean
    public static BasicRefererConfigBean refererConfigBean(RegistryConfigBean registryConfigBean,
                                                           MotanProperties properties, ProtocolConfigBean protocolConfigBean) {
        BasicRefererConfigBean bean = properties.getReferer();
        bean.setProtocol(protocolConfigBean);
        bean.setRegistry(registryConfigBean);
        return bean;
    }

    @Bean(name = "motanCommandLineRunner")
    @ConditionalOnMissingBean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public static MotanCommandLineRunner motanCommandLineRunner(MotanProperties properties) {
        return new MotanCommandLineRunner(properties);
    }
}