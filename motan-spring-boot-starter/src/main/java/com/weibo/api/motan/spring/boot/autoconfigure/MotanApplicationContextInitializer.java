//package com.weibo.api.motan.spring.boot.autoconfigure;
//
//import com.weibo.api.motan.config.springsupport.AnnotationBean;
//import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
//import org.springframework.context.ApplicationContextInitializer;
//import org.springframework.context.ConfigurableApplicationContext;
//import org.springframework.core.env.Environment;
//
//public class MotanApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
//
//    @Override
//    public void initialize(ConfigurableApplicationContext applicationContext) {
//        Environment env = applicationContext.getEnvironment();
//        String scan = env.getProperty("spring.motan.scan.package");
//
//        if (scan != null && !scan.isEmpty()) {
//            AnnotationBean scanner = new AnnotationBean();
//            scanner.setPackage(scan);
//
//            ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
//            applicationContext.addBeanFactoryPostProcessor(scanner);
//            beanFactory.addBeanPostProcessor(scanner);
//            beanFactory.registerSingleton("annotationBean", scanner);
//        }
//    }
//}
