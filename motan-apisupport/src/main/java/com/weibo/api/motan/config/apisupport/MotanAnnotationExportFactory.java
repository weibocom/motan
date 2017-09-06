package com.weibo.api.motan.config.apisupport;

import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.api.motan.exception.MotanServiceException;

/**
 * Created by yunzhu on 17/3/6.
 */
public class MotanAnnotationExportFactory {



    private MotanAnnotationExportFactory() {

    }

    public static ServiceConfig generateExportor(MotanServiceExportContext context,
                                        MotanService service,Class<?> clazz) {




        ServiceConfig serviceConfig = new ServiceConfig();


        serviceConfig.setBasicServiceConfig(context.getBasicServiceInterfaceConfig());

        serviceConfig.setHost(service.host());
        serviceConfig.setInterface(getInterface(service,clazz));


        PackageBaseExportorEvent event = context.getExportorEvent();
        boolean hasEvents = event != null;
        if(serviceConfig.getInterface() == null) {
            throw new MotanServiceException("export service [" + clazz.getName()  +"] error ,cause by interface is null");
        }
        try {
            Object object = clazz.newInstance();
            if(hasEvents) {
                event.onCreateMotanServiceBean(object,clazz.getSimpleName());
            }
        }
        catch (Throwable ex) {
            throw new MotanServiceException("export service [" + clazz.getName()  +"] error ",ex);
        }



        serviceConfig.setActives(service.actives());
        serviceConfig.setApplication(service.application());
        serviceConfig.setAsync(service.async());
        serviceConfig.setCodec(service.codec());
        serviceConfig.setFilter(service.filter());
        serviceConfig.setGroup(service.group());
        serviceConfig.setModule(service.module());
        serviceConfig.setVersion(service.version());
        serviceConfig.setMock(service.mock());
        serviceConfig.setRequestTimeout(service.requestTimeout());
        serviceConfig.setRetries(service.retries());
        serviceConfig.setUsegz(service.usegz());
        serviceConfig.setMingzSize(service.mingzSize());
        serviceConfig.setExport(service.export());

        if(hasEvents) {
            event.onCreateMotanServiceConfig(serviceConfig,clazz.getSimpleName());
        }
        return serviceConfig;

    }


    private static Class<?> getInterface(MotanService service,Class<?> clazz) {


        if(service.interfaceClass() != void.class) {
            return service.interfaceClass();
        }
        if(clazz != null) {
            Class<?>[] interfaces = clazz.getInterfaces();
            if(interfaces != null && interfaces.length == 1) {

                return clazz.getInterfaces()[0];
            }
        }
        return null;

    }


}
