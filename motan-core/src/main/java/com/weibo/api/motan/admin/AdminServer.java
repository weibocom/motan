package com.weibo.api.motan.admin;

import com.weibo.api.motan.rpc.URL;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public interface AdminServer {

    boolean open();

    void close();

    URL getUrl();

    AdminHandler getAdminHandler();
}
