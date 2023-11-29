package com.weibo.api.motan.admin;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.URL;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
@Spi(scope = Scope.SINGLETON)
public interface AdminServerFactory {
    AdminServer createServer(URL url, AdminHandler adminHandler);
}
