package com.weibo.api.motan.config.apisupport;

import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ServiceConfig;

import java.util.List;

/**
 * Created by yunzhu on 17/3/2.
 */
public class MotanPackageBaseExportor {


    private MotanServiceExportContext motanServiceExportContext;

    private MotanAnnotationClassScanner motanAnnotationScanner;


    public MotanPackageBaseExportor () {
        this(null,null);
    }

    public MotanPackageBaseExportor(BasicServiceInterfaceConfig basicServiceInterfaceConfig) {

       this(basicServiceInterfaceConfig,null);

    }


    public MotanPackageBaseExportor(BasicServiceInterfaceConfig basicServiceInterfaceConfig,
                                    PackageBaseExportorEvent exportorEvent) {

        this(basicServiceInterfaceConfig,exportorEvent,null);

    }

    public MotanPackageBaseExportor(BasicServiceInterfaceConfig basicServiceInterfaceConfig,
                                    PackageBaseExportorEvent exportorEvent,String pkgName) {

        motanServiceExportContext = new MotanServiceExportContext();

        motanServiceExportContext.setPkgName(pkgName);
        motanServiceExportContext.setExportorEvent(exportorEvent);
        motanServiceExportContext.setBasicServiceInterfaceConfig(basicServiceInterfaceConfig);
        motanAnnotationScanner = new MotanAnnotationClassScanner(motanServiceExportContext);
    }


    public void doExport() {

        motanAnnotationScanner.scanClassesFromPkg();

        List<ServiceConfig> services = motanServiceExportContext.getServiceConfigs();
        PackageBaseExportorEvent event = motanServiceExportContext.getExportorEvent();
        boolean hasEvent = event != null;
        if(hasEvent) {
            event.beforeExportService(motanServiceExportContext);
        }
        for(ServiceConfig serviceConfig:services) {

            serviceConfig.export();
        }

        if(hasEvent) {
            event.afterExportServcie();
        }
    }

}
