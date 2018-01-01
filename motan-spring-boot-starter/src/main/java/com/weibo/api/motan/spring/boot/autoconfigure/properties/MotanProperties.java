package com.weibo.api.motan.spring.boot.autoconfigure.properties;

import com.weibo.api.motan.config.springsupport.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.motan")
public class MotanProperties {

    private AnnotationBean scan;

    private ProtocolConfigBean protocol;

    private RegistryConfigBean registry;

    private BasicServiceConfigBean service;

    private BasicRefererConfigBean referer;

    public AnnotationBean getScan() {
        return scan;
    }

    public void setScan(AnnotationBean scan) {
        this.scan = scan;
    }

    public ProtocolConfigBean getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolConfigBean protocol) {
        this.protocol = protocol;
    }

    public RegistryConfigBean getRegistry() {
        return registry;
    }

    public void setRegistry(RegistryConfigBean registry) {
        this.registry = registry;
    }

    public BasicServiceConfigBean getService() {
        return service;
    }

    public void setService(BasicServiceConfigBean service) {
        this.service = service;
    }

    public BasicRefererConfigBean getReferer() {
        return referer;
    }

    public void setReferer(BasicRefererConfigBean referer) {
        this.referer = referer;
    }

    @Override
    public String toString() {
        return "MotanProperties{" +
                "scan=" + scan +
                ", protocol=" + protocol +
                ", registry=" + registry +
                ", service=" + service +
                ", referer=" + referer +
                '}';
    }
}
