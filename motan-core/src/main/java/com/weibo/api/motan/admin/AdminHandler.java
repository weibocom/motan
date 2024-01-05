package com.weibo.api.motan.admin;

import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public interface AdminHandler {
    /**
     * @param request 请求参数统一为Map<String, String>。 如果有复杂参数需要传递，可以自行定义Json格式的value string
     * @return response 返回值统一为Json格式的String
     */
    Response handle(Request request);

    void addCommandHandler(AdminCommandHandler adminCommandHandler, boolean override);

    PermissionChecker updatePermissionChecker(PermissionChecker permissionChecker);
}
