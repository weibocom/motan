package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import org.springframework.stereotype.Component;

@Component("testHandler")
public class AnnotationTestHandler {
    @MotanReferer(directUrl = "${motan.refer.directUrl}")
    private MotanReferer referer;

    public MotanReferer getReferer() {
        return referer;
    }

    public void setReferer(MotanReferer referer) {
        this.referer = referer;
    }
}
