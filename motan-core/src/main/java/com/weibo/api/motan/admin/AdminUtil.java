package com.weibo.api.motan.admin;

import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.DefaultResponse;
import com.weibo.api.motan.rpc.Request;

import java.util.Collections;
import java.util.Map;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
public class AdminUtil {
    private static final DefaultPermissionChecker DEFAULT_PERMISSION_CHECKER = new DefaultPermissionChecker();
    private static final AdminHandler DEFAULT_ADMIN_HANDLER = new DefaultAdminHandler();

    public static AdminHandler getDefaultAdminHandler() {
        return DEFAULT_ADMIN_HANDLER;
    }

    public static PermissionChecker getDefaultPermissionChecker() {
        return DEFAULT_PERMISSION_CHECKER;
    }

    public static void addCommandHandler(AdminCommandHandler adminCommandHandler) {
        addCommandHandler(adminCommandHandler, false);
    }

    public static void addCommandHandler(AdminCommandHandler adminCommandHandler, boolean override) {
        DEFAULT_ADMIN_HANDLER.addCommandHandler(adminCommandHandler, override);
    }

    public static void updatePermissionChecker(PermissionChecker permissionChecker) {
        DEFAULT_ADMIN_HANDLER.updatePermissionChecker(permissionChecker);
    }

    /**
     * build response for admin server
     *
     * @param request     admin request
     * @param returnValue return value for the request
     * @return admin response
     */
    public static DefaultResponse buildResponse(Request request, String returnValue) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(request.getRequestId());
        response.setRpcProtocolVersion(request.getRpcProtocolVersion());
        response.setValue(returnValue);
        return response;
    }

    /**
     * build error response for admin server
     *
     * @param request    admin request
     * @param errMessage error message for the request
     * @return error admin response
     */
    public static DefaultResponse buildErrorResponse(Request request, String errMessage) {
        DefaultResponse response = new DefaultResponse();
        response.setRequestId(request.getRequestId());
        response.setRpcProtocolVersion(request.getRpcProtocolVersion());
        JSONObject errJson = new JSONObject();
        errJson.put("result", "fail");
        errJson.put("error", errMessage == null ? "null" : errMessage);
        response.setException(new MotanServiceException(errJson.toJSONString()));
        return response;
    }

    public static DefaultResponse unknownCommand(Request request) {
        return buildErrorResponse(request, "unknown command " + request.getMethodName());
    }

    public static DefaultResponse notAllowed(Request request) {
        return buildErrorResponse(request, "not allowed");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> getParams(Request request) {
        if (request.getArguments() == null || request.getArguments().length < 1
                || !(request.getArguments()[0] instanceof Map)) {
            return Collections.emptyMap();
        }
        return (Map<String, String>) request.getArguments()[0];
    }

}
