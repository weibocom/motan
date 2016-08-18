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

import java.util.Set;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import com.weibo.api.motan.config.BasicRefererInterfaceConfig;
import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.rpc.init.Initializable;
import com.weibo.api.motan.rpc.init.InitializationFactory;
import com.weibo.api.motan.util.ConcurrentHashSet;

public class MotanNamespaceHandler extends NamespaceHandlerSupport {
    public final static Set<String> protocolDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> registryDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> basicServiceConfigDefineNames = new ConcurrentHashSet<String>();
    public final static Set<String> basicRefererConfigDefineNames = new ConcurrentHashSet<String>();

    @Override
    public void init() {
        registerBeanDefinitionParser("referer", new MotanBeanDefinitionParser(RefererConfigBean.class, false));
        registerBeanDefinitionParser("service", new MotanBeanDefinitionParser(ServiceConfigBean.class, true));
        registerBeanDefinitionParser("protocol", new MotanBeanDefinitionParser(ProtocolConfig.class, true));
        registerBeanDefinitionParser("registry", new MotanBeanDefinitionParser(RegistryConfig.class, true));
        registerBeanDefinitionParser("basicService", new MotanBeanDefinitionParser(BasicServiceInterfaceConfig.class, true));
        registerBeanDefinitionParser("basicReferer", new MotanBeanDefinitionParser(BasicRefererInterfaceConfig.class, true));
        registerBeanDefinitionParser("spi", new MotanBeanDefinitionParser(SpiConfigBean.class, true));
        registerBeanDefinitionParser("annotation", new MotanBeanDefinitionParser(AnnotationBean.class, true));
        Initializable initialization = InitializationFactory.getInitialization();
        initialization.init();
    }
}
