
package com.weibo.api.motan.config.springsupport.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Service Annotation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Component
public @interface ApiService {

    Class<?> interfaceClass() default void.class;

    String interfaceName() default "";

    String version() default "";

    String group() default "";

    String[] protocol() default {};

    String[] export() default {};

}
