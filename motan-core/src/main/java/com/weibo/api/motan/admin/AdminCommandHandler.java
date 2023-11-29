package com.weibo.api.motan.admin;

import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
@Spi(scope = Scope.SINGLETON)
public interface AdminCommandHandler {

    String[] getCommandName(); // Get the list of commands that this handler can handle

    Response handle(Request request);
}
