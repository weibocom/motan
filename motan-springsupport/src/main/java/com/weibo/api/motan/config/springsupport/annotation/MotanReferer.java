package com.weibo.api.motan.config.springsupport.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fld
 * Reference Annotation
 * Created by fld on 16/5/13.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface MotanReferer {


    Class<?> interfaceClass() default void.class;


    String client() default "";

    String directUrl() default "";

    String basicReferer() default "";

    String protocol() default "";

    String[] methods() default {};

    // 注册中心的配置列表
    String registry() default "";

    // 扩展配置点
    String extConfig() default ""; // TODO

    // 应用名称
    String application() default "";

    // 模块名称
    String module() default "";

    // 分组
    String group() default "";

    // 服务版本
    String version() default "";

    // 代理类型
    String proxy() default "";

    // 过滤器
    String filter() default "";

    // 最大并发调用
    int actives() default 0;

    // 是否异步
    boolean async() default false;

    // 服务接口的失败mock实现类名
    String mock() default "";

    // 是否共享 channel
    boolean shareChannel() default false;

    // if throw exception when call failure，the default value is ture
    boolean throwException() default false;

    // 请求超时时间
    int requestTimeout() default 0;

    // 是否注册
    boolean register() default false;

    // 是否记录访问日志，true记录，false不记录
    boolean accessLog() default false;

    // 是否进行check，如果为true，则在监测失败后抛异常
    boolean check() default false;

    // 重试次数
    int retries() default 0;

    // 是否开启gzip压缩
    boolean usegz() default false;

    // 进行gzip压缩的最小阈值，usegz开启，且大于此值时才进行gzip压缩。单位Byte
    int mingzSize() default 0;

    String codec() default "";

    String mean() default "";
    String p90() default "";
    String p99() default "";
    String p999() default "";
    String errorRate() default "";

}
