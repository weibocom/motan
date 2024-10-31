package com.weibo.api.motan.admin;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public class DefaultAdminHandler implements AdminHandler {
    protected PermissionChecker permissionChecker;
    protected ConcurrentHashMap<String, AdminCommandHandler> routeHandlers = new ConcurrentHashMap<>();

    public DefaultAdminHandler() {
        this(AdminUtil.getDefaultPermissionChecker());
    }

    public DefaultAdminHandler(PermissionChecker permissionChecker) {
        if (permissionChecker == null) {
            throw new MotanFrameworkException("permissionChecker can not be null");
        }
        this.permissionChecker = permissionChecker;
    }

    @Override
    public Response handle(Request request) {
        boolean pass = permissionChecker.check(request);
        if (!pass) {
            LoggerUtil.warn("motan admin: command '" + request.getMethodName() + "' is not allowed. ip:" + request.getAttachment(URLParamType.host.getName()));
            return AdminUtil.notAllowed(request);
        }

        AdminCommandHandler handler = routeHandlers.get(request.getMethodName());
        if (handler == null) {
            LoggerUtil.warn("motan admin: unknown command '" + request.getMethodName() + "'. ip:" + request.getAttachment(URLParamType.host.getName()));
            return AdminUtil.unknownCommand(request);
        }
        try {
            LoggerUtil.info("motan admin: process command '" + request.getMethodName() + "'. ip:" + request.getAttachment(URLParamType.host.getName()));
            return handler.handle(request);
        } catch (MotanAbstractException mae) {
            return AdminUtil.buildErrorResponse(request, mae.getOriginMessage());
        } catch (Throwable e) {
            return AdminUtil.buildErrorResponse(request, e.getMessage());
        }
    }

    @Override
    public void addCommandHandler(AdminCommandHandler adminCommandHandler, boolean override) {
        String[] commands = adminCommandHandler.getCommandName();
        for (String c : commands) {
            if (StringUtils.isNotBlank(c)) {
                c = c.trim();
                if (override) {
                    routeHandlers.put(c, adminCommandHandler);
                } else {
                    routeHandlers.putIfAbsent(c, adminCommandHandler);
                }
            }
        }
    }

    @Override
    public PermissionChecker updatePermissionChecker(PermissionChecker permissionChecker) {
        if (permissionChecker == null) {
            throw new MotanFrameworkException("admin permission checker is null");
        }
        PermissionChecker old = this.permissionChecker;
        this.permissionChecker = permissionChecker;
        return old;
    }

    public Set<String> getCommandSet() {
        return routeHandlers.keySet();
    }
}
