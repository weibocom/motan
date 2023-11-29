package com.weibo.api.motan.admin;

import com.weibo.api.motan.admin.handler.CommandListHandler;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.ExtensionLoader;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.rpc.init.Initializable;
import com.weibo.api.motan.util.LoggerUtil;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhanglei28
 * @date 2023/11/3.
 */
@SpiMeta(name = "admin")
public class AdminInitialization implements Initializable {
    // default values
    private static final String DEFAULT_ADMIN_SERVER = "netty4";
    private static final String DEFAULT_ADMIN_PROTOCOL = "http";

    @Override
    public void init() {
        try {
            int port = getAdminPort();
            if (port >= 0) { // create admin server
                // get spi server factory, an exception will be thrown if not found
                AdminServerFactory adminServerFactory = ExtensionLoader.getExtensionLoader(AdminServerFactory.class).getExtension(MotanGlobalConfigUtil.getConfig(MotanConstants.ADMIN_SERVER, DEFAULT_ADMIN_SERVER));
                // build admin server url
                URL adminUrl = new URL(MotanGlobalConfigUtil.getConfig(MotanConstants.ADMIN_PROTOCOL, DEFAULT_ADMIN_PROTOCOL), "127.0.0.1", port, "/",
                        MotanGlobalConfigUtil.entrySet().stream().filter((entry) -> entry.getKey().startsWith("admin.")).collect(Collectors.toMap((entry) -> entry.getKey().substring("admin.".length()), Map.Entry::getValue)));

                // add default command handlers;
                AdminUtil.addCommandHandler(new CommandListHandler());

                // add command handler extensions from config
                addExtHandlers(MotanGlobalConfigUtil.getConfig(MotanConstants.ADMIN_EXT_HANDLERS));

                // add command handler extensions from ENV
                addExtHandlers(System.getenv(MotanConstants.ENV_MOTAN_ADMIN_EXT_HANDLERS));

                // create admin server
                AdminServer adminServer = adminServerFactory.createServer(adminUrl, AdminUtil.getDefaultAdminHandler());
                adminServer.open();
                LoggerUtil.info("admin server is open. url:" + adminUrl.toFullStr());
            }
        } catch (Exception e) {
            LoggerUtil.error("admin server open fail.", e);
        }
    }

    private int getAdminPort() {
        int port = -1;
        // 'admin.disable' has the highest priority
        if ("true".equals(MotanGlobalConfigUtil.getConfig(MotanConstants.ADMIN_DISABLE))) {
            return port;
        }
        // get from env
        port = parsePort(System.getenv(MotanConstants.ENV_MOTAN_ADMIN_PORT));

        if (port < 0) { // get from global configs
            port = parsePort(MotanGlobalConfigUtil.getConfig(MotanConstants.ADMIN_PORT));
        }
        // default admin port
        if (port < 0) {
            port = MotanConstants.DEFAULT_ADMIN_PORT;
        }
        return port;
    }

    private int parsePort(String portStr) {
        int port = -1;
        if (StringUtils.isNotBlank(portStr)) {
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                LoggerUtil.warn("AdminInitialization parse admin port from env fail. value:" + portStr);
            }
        }
        return port;
    }

    private void addExtHandlers(String handlerString) {
        if (handlerString != null) {
            String[] handlers = handlerString.split(",");
            for (String h : handlers) {
                if (StringUtils.isNotBlank(h)) {
                    try {
                        AdminCommandHandler handler = ExtensionLoader.getExtensionLoader(AdminCommandHandler.class).getExtension(h.trim());
                        AdminUtil.addCommandHandler(handler, true);
                        LoggerUtil.info("admin server add handler " + handler.getClass().getName());
                    } catch (Exception e) {
                        LoggerUtil.warn("can not find admin command handler :" + h);
                    }
                }
            }
        }
    }
}
