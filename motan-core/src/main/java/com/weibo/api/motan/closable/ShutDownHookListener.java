package com.weibo.api.motan.closable;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author zhanran
 * Date: 2017/5/24
 * In order to shutdown motan server running in tomcat(run tomcat's shutdown.sh rather than kill PID manually),add ShutDownHookListener to web.xml
 * 为了关闭在tomcat中运行的motan server（运行tomcat的shutdown.sh关闭而不是手动kill pid），在web.xml中添加ShutDownHookListener
 */
public class ShutDownHookListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ShutDownHook.runHook(true);
    }
}
