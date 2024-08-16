package com.weibo.api.motan.admin.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.weibo.api.motan.admin.AbstractAdminCommandHandler;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.filter.FaultInjectionFilter;
import com.weibo.api.motan.util.MotanGlobalConfigUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

import static com.weibo.api.motan.common.MotanConstants.ENV_GLOBAL_FILTERS;

/**
 * @author zhanglei28
 * @date 2023/11/23.
 */
@SpiMeta(name = "faultInjection")
public class FaultInjectCommandHandler extends AbstractAdminCommandHandler {
    private static final String[] commands = new String[]{
            "/faultInjection/config/update",
            "/faultInjection/config/clear",
            "/faultInjection/config/get"};

    static {
        String filters = MotanGlobalConfigUtil.getConfig(ENV_GLOBAL_FILTERS);
        filters = StringUtils.isBlank(filters) ? "faultInjection" : filters + MotanConstants.COMMA_SEPARATOR + "faultInjection";
        MotanGlobalConfigUtil.putConfig(ENV_GLOBAL_FILTERS, filters);
    }

    @Override
    public String[] getCommandName() {
        return commands;
    }

    @Override
    protected void process(String command, Map<String, String> params, Map<String, String> attachments, JSONObject result) {
        if (commands[0].equals(command)) {
            String configs = params.get("configs");
            List<FaultInjectionFilter.FaultInjectionConfig> configList = JSONArray.parseArray(configs, FaultInjectionFilter.FaultInjectionConfig.class);
            if (configList == null) {
                throw new MotanServiceException("param configs not correct");
            }
            FaultInjectionFilter.FaultInjectionUtil.updateConfigs(configList);
        } else if (commands[1].equals(command)) {
            FaultInjectionFilter.FaultInjectionUtil.clearConfigs();
        } else if (commands[2].equals(command)) {
            List<FaultInjectionFilter.FaultInjectionConfig> conf = FaultInjectionFilter.FaultInjectionUtil.getConfigs();
            result.put("data", JSON.parseArray(JSON.toJSONString(conf)));
        }
    }
}
