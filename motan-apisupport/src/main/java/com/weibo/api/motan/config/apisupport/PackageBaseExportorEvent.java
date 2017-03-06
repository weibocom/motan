package com.weibo.api.motan.config.apisupport;

import com.weibo.api.motan.config.ServiceConfig;

/**
 * Created by yunzhu on 17/3/2.
 */

public interface PackageBaseExportorEvent {


    public void onFetchMotanServiceClass(Class<?> motanServiceClass);

    public void onCreateMotanServiceBean(Object obj,String clazzName);

    public void onCreateMotanServiceConfig(ServiceConfig obj,String clazzName);


    public void beforeExportService(MotanServiceExportContext context);


    public void afterExportServcie();
}
