package com.weibo.api.motan.config.springsupport.annotation;

import com.weibo.api.motan.config.springsupport.EnableMotanRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by Jonathan on 16/7/25.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(EnableMotanRegistrar.class)
public @interface EnableMotan {

    String[] packages() default {};
}
