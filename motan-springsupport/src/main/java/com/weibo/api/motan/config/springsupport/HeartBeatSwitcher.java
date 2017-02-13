package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Created by kegao on 2016/10/18.
 */
public class HeartBeatSwitcher implements ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = Logger.getLogger(HeartBeatSwitcher.class);
    private boolean switcherValue;

    public void setSwitcherValue(boolean switcherValue) {
        this.switcherValue = switcherValue;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if(contextRefreshedEvent.getApplicationContext().getParent() != null) {//root application context 没有parent，这里需要projectName-servlet  context（作为root application context的子容器）之后执行
            MotanSwitcherUtil.setSwitcherValue(MotanConstants.REGISTRY_HEARTBEAT_SWITCHER, switcherValue);
            logger.info("rpc server start.switcherValue ====== " + switcherValue + ".");
        }
    }
}
