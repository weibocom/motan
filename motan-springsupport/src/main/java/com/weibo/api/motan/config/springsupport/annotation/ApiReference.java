package com.weibo.api.motan.config.springsupport.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reference Annotation
 * Created by fld on 16/5/13.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ApiReference {
    String value() default "";

    String protocol() default "";

    String interfaceName() default "";

    Class<?> interfaceClass() default void.class;

    String group() default "";

    String version() default "";

    String basicReferer() default "";

    boolean throwException() default false;

    String requestTimeout() default "";

    boolean shareChannel() default false;

    String directUrl() default "";

}
