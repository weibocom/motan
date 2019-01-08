package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import org.springframework.beans.BeansException;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.util.StringValueResolver;

import java.lang.reflect.Field;

public class AnnotationTestBean extends AnnotationBean implements EmbeddedValueResolverAware {
    private StringValueResolver embeddedValueResolver;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                MotanReferer reference = field.getAnnotation(MotanReferer.class);
                if (reference != null) {
                    field.set(bean, new MotanRefererWrapper(reference, embeddedValueResolver));
                }
            } catch (Exception ignored) {
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        MotanService service = clazz.getAnnotation(MotanService.class);
        if (service != null) {
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.getType() == MotanService.class) {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        field.set(bean, new MotanServiceWrapper(service, embeddedValueResolver));
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return bean;
    }

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }
}
