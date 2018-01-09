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

import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ConfigUtil;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.springsupport.util.SpringBeanUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collections;
import java.util.List;

/**
 * @author fld
 * <p>
 * Created by fld on 16/5/13.
 */
public class BasicServiceConfigBean extends BasicServiceInterfaceConfig implements BeanNameAware,
        InitializingBean, BeanFactoryAware {

    BeanFactory beanFactory;
    private String registryNames;

    @Override
    public void setBeanName(String name) {
        setId(name);

        MotanNamespaceHandler.basicServiceConfigDefineNames.add(name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        String protocol = ConfigUtil.extractProtocols(getExport());
        setRegistries(extractRegistries(registryNames, beanFactory));
        setProtocols(extractProtocols(protocol, beanFactory));
    }

    public List<ProtocolConfig> extractProtocols(String protocols, BeanFactory beanFactory) {
        if (protocols != null && protocols.length() > 0) {
            List<ProtocolConfig> protocolConfigList = SpringBeanUtil.getMultiBeans(beanFactory, protocols,
                    SpringBeanUtil.COMMA_SPLIT_PATTERN, ProtocolConfig.class);

            return protocolConfigList;
        } else {
            return null;
        }
    }

    public List<RegistryConfig> extractRegistries(String registries, BeanFactory beanFactory) {
        if (registries != null && registries.length() > 0) {
            if (!registries.contains(",")) {
                RegistryConfig registryConfig = beanFactory.getBean(registries, RegistryConfig.class);
                return Collections.singletonList(registryConfig);
            } else {
                List<RegistryConfig> registryConfigList = SpringBeanUtil.getMultiBeans(beanFactory, registries,
                        SpringBeanUtil.COMMA_SPLIT_PATTERN, RegistryConfig.class);
                return registryConfigList;
            }
        } else {
            return null;
        }
    }


    public void setRegistry(String registryNames) {
        this.registryNames = registryNames;
    }

    public void setCheck(boolean value) {
        setCheck(String.valueOf(value));
    }

    public void setAccessLog(boolean value) {
        setAccessLog(String.valueOf(value));
    }
}
