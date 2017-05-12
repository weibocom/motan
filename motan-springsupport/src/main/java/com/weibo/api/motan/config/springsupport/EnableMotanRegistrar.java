package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.EnableMotan;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * Created by Jonathan on 16/7/25.
 */
public class EnableMotanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

        Map attr = metadata.getAnnotationAttributes(EnableMotan.class.getName());


        RootBeanDefinition bd = new RootBeanDefinition(AnnotationBean.class);
        bd.setLazyInit(false);
        bd.getPropertyValues().addPropertyValue("package", attr.get("packages"));

        String beanName = AnnotationBean.class.getName();
        int counter = 2;
        while (registry.containsBeanDefinition(beanName)) {
            beanName = AnnotationBean.class.getName() + (counter++);
        }
        registry.registerBeanDefinition(beanName, bd);
    }
}
