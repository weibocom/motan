package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import org.springframework.util.StringValueResolver;

import java.lang.annotation.Annotation;

class MotanServiceWrapper implements MotanService {
    private MotanService service;
    private StringValueResolver resolver;

    MotanServiceWrapper(MotanService service, StringValueResolver resolver) {
        this.service = service;
        this.resolver = resolver;
    }

    @Override
    public Class<?> interfaceClass() {
        return service.interfaceClass();
    }

    @Override
    public String basicService() {
        return resolveValue(service.basicService());
    }

    @Override
    public String export() {
        return resolveValue(service.export());
    }

    @Override
    public String host() {
        return resolveValue(service.host());
    }

    @Override
    public String protocol() {
        return resolveValue(service.protocol());
    }

    @Override
    public String[] methods() {
        return service.methods();
    }

    @Override
    public String registry() {
        return resolveValue(service.registry());
    }

    @Override
    public String extConfig() {
        return resolveValue(service.extConfig());
    }

    @Override
    public String application() {
        return resolveValue(service.application());
    }

    @Override
    public String module() {
        return resolveValue(service.module());
    }

    @Override
    public String group() {
        return resolveValue(service.group());
    }

    @Override
    public String version() {
        return resolveValue(service.version());
    }

    @Override
    public String proxy() {
        return resolveValue(service.proxy());
    }

    @Override
    public String filter() {
        return resolveValue(service.filter());
    }

    @Override
    public int actives() {
        return service.actives();
    }

    @Override
    public boolean async() {
        return service.async();
    }

    @Override
    public String mock() {
        return resolveValue(service.mock());
    }

    @Override
    public boolean shareChannel() {
        return service.shareChannel();
    }

    @Override
    public boolean throwException() {
        return service.throwException();
    }

    @Override
    public int requestTimeout() {
        return service.requestTimeout();
    }

    @Override
    public boolean register() {
        return service.register();
    }

    @Override
    public boolean accessLog() {
        return service.accessLog();
    }

    @Override
    public boolean check() {
        return service.check();
    }

    @Override
    public int retries() {
        return service.retries();
    }

    @Override
    public boolean usegz() {
        return service.usegz();
    }

    @Override
    public int mingzSize() {
        return service.mingzSize();
    }

    @Override
    public String codec() {
        return resolveValue(service.codec());
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return service.annotationType();
    }

    private String resolveValue(String value) {
        return resolver == null ? value : resolver.resolveStringValue(value);
    }
}
