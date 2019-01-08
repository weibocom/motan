package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import org.springframework.util.StringValueResolver;

import java.lang.annotation.Annotation;

class MotanRefererWrapper implements MotanReferer {
    private MotanReferer referer;
    private StringValueResolver resolver;

    MotanRefererWrapper(MotanReferer referer, StringValueResolver resolver) {
        this.referer = referer;
        this.resolver = resolver;
    }

    @Override
    public Class<?> interfaceClass() {
        return referer.interfaceClass();
    }

    @Override
    public String client() {
        return resolveValue(referer.client());
    }

    @Override
    public String directUrl() {
        return resolveValue(referer.directUrl());
    }

    @Override
    public String basicReferer() {
        return resolveValue(referer.basicReferer());
    }

    @Override
    public String protocol() {
        return resolveValue(referer.protocol());
    }

    @Override
    public String[] methods() {
        return referer.methods();
    }

    @Override
    public String registry() {
        return resolveValue(referer.registry());
    }

    @Override
    public String extConfig() {
        return resolveValue(referer.extConfig());
    }

    @Override
    public String application() {
        return resolveValue(referer.application());
    }

    @Override
    public String module() {
        return resolveValue(referer.module());
    }

    @Override
    public String group() {
        return resolveValue(referer.group());
    }

    @Override
    public String version() {
        return resolveValue(referer.version());
    }

    @Override
    public String proxy() {
        return resolveValue(referer.proxy());
    }

    @Override
    public String filter() {
        return resolveValue(referer.filter());
    }

    @Override
    public int actives() {
        return referer.actives();
    }

    @Override
    public boolean async() {
        return referer.async();
    }

    @Override
    public String mock() {
        return resolveValue(referer.mock());
    }

    @Override
    public boolean shareChannel() {
        return referer.shareChannel();
    }

    @Override
    public boolean throwException() {
        return referer.throwException();
    }

    @Override
    public int requestTimeout() {
        return referer.requestTimeout();
    }

    @Override
    public boolean register() {
        return referer.register();
    }

    @Override
    public boolean accessLog() {
        return referer.accessLog();
    }

    @Override
    public boolean check() {
        return referer.check();
    }

    @Override
    public int retries() {
        return referer.retries();
    }

    @Override
    public boolean usegz() {
        return referer.usegz();
    }

    @Override
    public int mingzSize() {
        return referer.mingzSize();
    }

    @Override
    public String codec() {
        return resolveValue(referer.codec());
    }

    @Override
    public String mean() {
        return resolveValue(referer.mean());
    }

    @Override
    public String p90() {
        return resolveValue(referer.p90());
    }

    @Override
    public String p99() {
        return resolveValue(referer.p99());
    }

    @Override
    public String p999() {
        return resolveValue(referer.p999());
    }

    @Override
    public String errorRate() {
        return resolveValue(referer.errorRate());
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return referer.annotationType();
    }

    private String resolveValue(String value) {
        return resolver == null ? value : resolver.resolveStringValue(value);
    }
}
