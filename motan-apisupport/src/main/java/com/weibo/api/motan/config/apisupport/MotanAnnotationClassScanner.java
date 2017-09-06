package com.weibo.api.motan.config.apisupport;

import com.google.common.collect.Lists;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.api.motan.exception.MotanServiceException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by yunzhu on 17/3/2.
 */
public class MotanAnnotationClassScanner extends MotanClassScanner {

    private final MotanServiceExportContext motanServiceExportContext;

    public MotanAnnotationClassScanner(MotanServiceExportContext exportor) {

        this.motanServiceExportContext = exportor;
    }

    public void scanClassesFromPkg()  {

        if( motanServiceExportContext == null) {
            throw new MotanServiceException("scan service from package faild cause by MotanServiceExportContext is null");
        }
        if(StringUtils.isEmpty(motanServiceExportContext.getPkgName())) {

            throw new MotanServiceException("scan service from package faild cause by package name is null");
        }


        Set<Class<?>> motanAnnotantionServices = new LinkedHashSet<Class<?>>(1);
        PackageBaseExportorEvent event = motanServiceExportContext.getExportorEvent();

        boolean hasEvent = event != null;
        try {
            Set<MotanResources> motanResources = doScan(motanServiceExportContext.getPkgName());
            List<ServiceConfig> serviceConfigs = Lists.newArrayList() ;
            for(MotanResources resources:motanResources) {
                File file = resources.getFile();
                String classPath =  replace(file.getPath(), PATH_SEPARATOR, PACKAGE_SEPARATOR);
                int clazzPos = classPath.indexOf(motanServiceExportContext.getPkgName());
                String clsFullName = classPath.substring(clazzPos,classPath.length() - 6);
                Class<?> clszz = Class.forName(clsFullName);
                if(clszz != null && !clszz.isInterface()) {
                    MotanService motanService = clszz.getAnnotation(MotanService.class);
                    if(motanService != null) {
                        motanAnnotantionServices.add(clszz);
                        if(hasEvent) {
                            event.onFetchMotanServiceClass(clszz);
                        }
                        serviceConfigs.add(MotanAnnotationExportFactory.generateExportor(

                                        motanServiceExportContext, motanService, clszz)
                        );

                    }
                }

            }
            motanServiceExportContext.setServiceConfigs(serviceConfigs);
            motanServiceExportContext.setMotanServieClasses(motanAnnotantionServices);

        }
        catch (Throwable e) {
            throw new MotanServiceException("scan motan service faild",e);
        }
    }

}
