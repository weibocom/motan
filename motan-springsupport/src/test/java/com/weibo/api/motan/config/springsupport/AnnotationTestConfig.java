package com.weibo.api.motan.config.springsupport;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnnotationTestConfig {

    @Bean
    public AnnotationTestBean annotationTestBean() {
        AnnotationTestBean annotationTestBean = new AnnotationTestBean();
        annotationTestBean.setPackage("com.weibo.api.motan.config.springsupport");
        return annotationTestBean;
    }
}
