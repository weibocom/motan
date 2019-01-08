package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import org.springframework.stereotype.Component;

@Component("testService")
@MotanService(export = "${motan.service.export}")
public class AnnotationTestService {
    private MotanService service;

    public MotanService getService() {
        return service;
    }

    public void setService(MotanService service) {
        this.service = service;
    }
}
