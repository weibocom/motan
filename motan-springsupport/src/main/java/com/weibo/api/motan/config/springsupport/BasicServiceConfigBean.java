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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BasicServiceConfigBean extends BasicServiceInterfaceConfig implements BeanNameAware,
        InitializingBean, BeanFactoryAware {

    private String registryNames;


    BeanFactory beanFactory;

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

    public  List<ProtocolConfig> extractProtocols(String protocols, BeanFactory beanFactory) {
        if (protocols != null && protocols.length() > 0) {
            if (!protocols.contains(",")) {
                ProtocolConfig protocolConfig = beanFactory.getBean(protocols, ProtocolConfig.class);
                return Collections.singletonList(protocolConfig);
            } else {
                String[] values = protocols.split("\\s*[,]+\\s*");
                List<ProtocolConfig> protocolConfigList = new ArrayList<ProtocolConfig>();
                for (String proto : values) {
                    ProtocolConfig protocolConfig = beanFactory.getBean(proto, ProtocolConfig.class);
                    protocolConfigList.add(protocolConfig);
                }
                return protocolConfigList;
            }
        } else {
            return null;
        }
    }

    public  List<RegistryConfig> extractRegistries(String registries, BeanFactory beanFactory) {
        if (registries != null && registries.length() > 0) {
            if (!registries.contains(",")) {
                RegistryConfig registryConfig = beanFactory.getBean(registries, RegistryConfig.class);
                return Collections.singletonList(registryConfig);
            } else {
                String[] names = registries.split("\\s*[,]+\\s*");
                List<RegistryConfig> registryConfigList = new ArrayList<RegistryConfig>();
                for (String registryName : names) {
                    RegistryConfig registryConfig = beanFactory.getBean(registryName, RegistryConfig.class);
                    registryConfigList.add(registryConfig);
                }
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

    public void setAccessLog(boolean value){
        setAccessLog(String.valueOf(value));
    }
}
