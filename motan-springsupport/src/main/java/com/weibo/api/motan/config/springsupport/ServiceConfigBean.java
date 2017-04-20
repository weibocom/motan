/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.weibo.api.motan.config.springsupport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ConfigUtil;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.ServiceConfig;
import com.weibo.api.motan.exception.MotanErrorMsgConstant;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.util.CollectionUtil;
import com.weibo.api.motan.util.MotanFrameworkUtil;

public class ServiceConfigBean<T> extends ServiceConfig<T>
        implements
        BeanPostProcessor,
        BeanFactoryAware,
        InitializingBean,
        DisposableBean,
        ApplicationListener<ContextRefreshedEvent> {

    private static final long serialVersionUID = -7247592395983804440L;

    private transient BeanFactory beanFactory;

    @Override
    public void destroy() throws Exception {
        unexport();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // 注意:basicConfig需要首先配置，因为其他可能会依赖于basicConfig的配置
        checkAndConfigBasicConfig();
        checkAndConfigExport();
        checkAndConfigRegistry();

        // 等spring初始化完毕后，再export服务
        // export();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    // 为了让serviceBean最早加载
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!getExported().get()) {
            export();
        }
    }

    /**
     * 检查并配置basicConfig
     */
    private void checkAndConfigBasicConfig() {
        if (getBasicService() == null) {
            if (MotanNamespaceHandler.basicServiceConfigDefineNames.size() == 0) {
                if (beanFactory instanceof ListableBeanFactory) {
                    ListableBeanFactory listableBeanFactory = (ListableBeanFactory) beanFactory;
                    String[] basicServiceConfigNames = listableBeanFactory.getBeanNamesForType
                            (BasicServiceInterfaceConfig
                                    .class);
                    MotanNamespaceHandler.basicServiceConfigDefineNames.addAll(Arrays.asList(basicServiceConfigNames));
                }
            }
            for (String name : MotanNamespaceHandler.basicServiceConfigDefineNames) {
                BasicServiceInterfaceConfig biConfig = beanFactory.getBean(name, BasicServiceInterfaceConfig.class);
                if (biConfig == null) {
                    continue;
                }
                if (MotanNamespaceHandler.basicServiceConfigDefineNames.size() == 1) {
                    setBasicService(biConfig);
                } else if (biConfig.isDefault() != null && biConfig.isDefault().booleanValue()) {
                    setBasicService(biConfig);
                }
            }
        }
    }

    /**
     * 检查是否已经装配export，如果没有则到basicConfig查找
     */
    private void checkAndConfigExport() {
        if (StringUtils.isBlank(getExport()) && getBasicService() != null
                && !StringUtils.isBlank(getBasicService().getExport())) {
            setExport(getBasicService().getExport());
            if (getBasicService().getProtocols() != null) {
                setProtocols(new ArrayList<ProtocolConfig>(getBasicService().getProtocols()));
            }
        }

        if (CollectionUtil.isEmpty(getProtocols()) && StringUtils.isNotEmpty(getExport())) {
            Map<String, Integer> exportMap = ConfigUtil.parseExport(export);
            if (!exportMap.isEmpty()) {
                List<ProtocolConfig> protos = new ArrayList<ProtocolConfig>();
                for (String p : exportMap.keySet()) {
                    ProtocolConfig proto = null;
                    try {
                        proto = beanFactory.getBean(p, ProtocolConfig.class);
                    } catch (NoSuchBeanDefinitionException e) {}
                    if (proto == null) {
                        if (MotanConstants.PROTOCOL_MOTAN.equals(p)) {
                            proto = MotanFrameworkUtil.getDefaultProtocolConfig();
                        } else {
                            throw new MotanFrameworkException(String.format("cann't find %s ProtocolConfig bean! export:%s", p, export),
                                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
                        }
                    }

                    protos.add(proto);
                }
                setProtocols(protos);
            }
        }
        if (StringUtils.isEmpty(getExport()) || CollectionUtil.isEmpty(getProtocols())) {
            throw new MotanFrameworkException(String.format("%s ServiceConfig must config right export value!", getInterface().getName()),
                    MotanErrorMsgConstant.FRAMEWORK_INIT_ERROR);
        }
    }

    /**
     * 检查并配置registry
     */
    private void checkAndConfigRegistry() {
        if (CollectionUtil.isEmpty(getRegistries()) && getBasicService() != null
                && !CollectionUtil.isEmpty(getBasicService().getRegistries())) {
            setRegistries(getBasicService().getRegistries());
        }
        if (CollectionUtil.isEmpty(getRegistries())) {
            for (String name : MotanNamespaceHandler.registryDefineNames) {
                RegistryConfig rc = beanFactory.getBean(name, RegistryConfig.class);
                if (rc == null) {
                    continue;
                }
                if (MotanNamespaceHandler.registryDefineNames.size() == 1) {
                    setRegistry(rc);
                } else if (rc.isDefault() != null && rc.isDefault().booleanValue()) {
                    setRegistry(rc);
                }
            }
        }
        if (CollectionUtil.isEmpty(getRegistries())) {
            setRegistry(MotanFrameworkUtil.getDefaultRegistryConfig());
        }
    }

}
