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

import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.springsupport.util.SpringBeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author fld
 *         <p>
 *         Created by fld on 16/5/13.
 */
public class RegistryConfigBean extends RegistryConfig implements BeanNameAware, InitializingBean, BeanFactoryAware {

    private String proxyRegistryId;
    private BeanFactory beanFactory;

    @Override
    public void setBeanName(String name) {
        setId(name);
        setName(name);
        MotanNamespaceHandler.registryDefineNames.add(name);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (StringUtils.isNotBlank(this.proxyRegistryId)) {
            setProxyRegistry(beanFactory.getBean(proxyRegistryId, RegistryConfig.class));
        }
        SpringBeanUtil.addRegistryParamBean(this, beanFactory);
    }

    public void setProxyRegistryId(String proxyRegistryId) {
        this.proxyRegistryId = proxyRegistryId;
    }
}
