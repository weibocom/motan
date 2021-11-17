package com.weibo.api.motan.config.springsupport.util;

import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author fld
 *         Created by fld on 16/7/18.
 */
public class SpringBeanUtil {
    public static final String COMMA_SPLIT_PATTERN = "\\s*[,]+\\s*";

    public static <T> List<T> getMultiBeans(BeanFactory beanFactory, String names, String pattern, Class<T> clazz) {
        String[] nameArr = names.split(pattern);
        List<T> beans = new ArrayList<T>();
        for (String name : nameArr) {
            if (name != null && name.length() > 0) {
                beans.add(beanFactory.getBean(name, clazz));
            }
        }
        return beans;
    }

    public static void addRegistryParamBean(RegistryConfig registryConfig, BeanFactory beanFactory) {
        if (registryConfig.getProxyRegistry() == null) {
            Map<String, String> addressParams = registryConfig.getAddressParams();
            String proxyRegistryId = addressParams.get(URLParamType.proxyRegistryId);
            if (StringUtils.isNotBlank(proxyRegistryId)) {
                String identity = registryConfig.getId() + "-" + registryConfig.getName();
                RegistryConfig proxyRegistry = beanFactory.getBean(proxyRegistryId, RegistryConfig.class);
                if (proxyRegistry != null) {
                    registryConfig.setProxyRegistry(proxyRegistry);
                    LoggerUtil.info("add proxy registry bean by address params. proxyRegistryId:" + proxyRegistryId + ", RegistryConfig:" + identity);
                } else {
                    LoggerUtil.warn("proxy registry bean not found. proxyRegistryId:" + proxyRegistryId + ", RegistryConfig:" + identity);
                }
            }
        }
    }
}
