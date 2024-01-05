package com.weibo.api.motan.admin;

import com.weibo.api.motan.rpc.Request;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public interface PermissionChecker {
    boolean check(Request request);
}
