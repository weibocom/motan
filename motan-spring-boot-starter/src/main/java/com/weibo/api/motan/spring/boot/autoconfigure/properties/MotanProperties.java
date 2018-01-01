package com.weibo.api.motan.spring.boot.autoconfigure.properties;

import com.weibo.api.motan.config.springsupport.BasicRefererConfigBean;
import com.weibo.api.motan.config.springsupport.BasicServiceConfigBean;
import com.weibo.api.motan.config.springsupport.ProtocolConfigBean;
import com.weibo.api.motan.config.springsupport.RegistryConfigBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.motan")
public class MotanProperties implements InitializingBean {

    private String scanPackage;

    private ProtocolConfigBean protocol;

    private RegistryConfigBean registry;

    private BasicServiceConfigBean service;

    private BasicRefererConfigBean referer;

    public String getScanPackage() {
        return scanPackage;
    }

    public void setScanPackage(String scanPackage) {
        this.scanPackage = scanPackage;
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
                "scanPackage='" + scanPackage + '\'' +
                ", protocol=" + protocol +
                ", registry=" + registry +
                ", service=" + service +
                ", referer=" + referer +
                '}';
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("sdfdsf");
    }
}
