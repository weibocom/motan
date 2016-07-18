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

import com.weibo.api.motan.config.BasicRefererInterfaceConfig;
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
 *         <p>
 *         Created by fld on 16/5/13.
 */
public class BasicRefererConfigBean extends BasicRefererInterfaceConfig implements BeanNameAware, InitializingBean, BeanFactoryAware {

    private String protocolNames;
    private String registryNames;
    private BeanFactory beanFactory;

    @Override
    public void setBeanName(String name) {
        setId(name);
        MotanNamespaceHandler.basicRefererConfigDefineNames.add(name);
    }

    public void setProtocol(String protocolNames) {
        this.protocolNames = protocolNames;
    }

    public void setRegistry(String registryNames) {
        this.registryNames = registryNames;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        setRegistries(extractRegistries(registryNames, beanFactory));
        setProtocols(extractProtocols(protocolNames, beanFactory));
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
            List<RegistryConfig> registryConfigList = SpringBeanUtil.getMultiBeans(beanFactory, registries,
                    SpringBeanUtil.COMMA_SPLIT_PATTERN, RegistryConfig.class);
            return registryConfigList;
        } else {
            return null;
        }
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public void setCheck(boolean value) {
        setCheck(String.valueOf(value));
    }

    public void setAccessLog(boolean value) {
        setAccessLog(String.valueOf(value));
    }
}
