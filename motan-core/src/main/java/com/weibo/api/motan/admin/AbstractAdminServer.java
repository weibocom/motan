package com.weibo.api.motan.admin;

import com.weibo.api.motan.rpc.URL;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public abstract class AbstractAdminServer implements AdminServer {
    protected URL url;
    protected AdminHandler adminHandler;

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public AdminHandler getAdminHandler() {
        return adminHandler;
    }
}
