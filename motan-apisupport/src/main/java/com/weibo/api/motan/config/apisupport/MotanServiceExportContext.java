package com.weibo.api.motan.config.apisupport;

import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ServiceConfig;

import java.util.List;
import java.util.Set;

/**
 * Created by yunzhu on 17/3/3.
 */
public class MotanServiceExportContext {

    private Set<Class<?>> motanServieClasses;


    private String pkgName;

    private PackageBaseExportorEvent exportorEvent;

    private List<ServiceConfig> serviceConfigs;



    private BasicServiceInterfaceConfig basicServiceInterfaceConfig;

    public Set<Class<?>> getMotanServieClasses() {
        return motanServieClasses;
    }

    public void setMotanServieClasses(Set<Class<?>> motanServieClasses) {
        this.motanServieClasses = motanServieClasses;
    }

    public String getPkgName() {
        return pkgName;
    }

    public void setPkgName(String pkgName) {
        this.pkgName = pkgName;
    }

    public PackageBaseExportorEvent getExportorEvent() {
        return exportorEvent;
    }

    public void setExportorEvent(PackageBaseExportorEvent exportorEvent) {
        this.exportorEvent = exportorEvent;
    }

    public BasicServiceInterfaceConfig getBasicServiceInterfaceConfig() {
        return basicServiceInterfaceConfig;
    }

    public void setBasicServiceInterfaceConfig(BasicServiceInterfaceConfig basicServiceInterfaceConfig) {
        this.basicServiceInterfaceConfig = basicServiceInterfaceConfig;
    }

    public List<ServiceConfig> getServiceConfigs() {
        return serviceConfigs;
    }

    public void setServiceConfigs(List<ServiceConfig> serviceConfigs) {
        this.serviceConfigs = serviceConfigs;
    }
}
