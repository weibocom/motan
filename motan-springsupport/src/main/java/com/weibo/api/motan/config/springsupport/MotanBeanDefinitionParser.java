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

import com.weibo.api.motan.config.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * MotanBeanDefinitionParser
 *
 * @author fishermen
 * @version V1.0 created at: 2013-7-22
 */

public class MotanBeanDefinitionParser implements BeanDefinitionParser {

    private final Class<?> beanClass;

    private final boolean required;

    public MotanBeanDefinitionParser(Class<?> beanClass, boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        try {
            return parse(element, parserContext, beanClass, required);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BeanDefinition parse(Element element, ParserContext parserContext, Class<?> beanClass, boolean required)
            throws ClassNotFoundException {
        RootBeanDefinition bd = new RootBeanDefinition();
        bd.setBeanClass(beanClass);
        // 不允许lazy init
        bd.setLazyInit(false);

        // 如果没有id则按照规则生成一个id,注册id到context中
        String id = element.getAttribute("id");
        if ((id == null || id.length() == 0) && required) {
            String generatedBeanName = element.getAttribute("name");
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = element.getAttribute("class");
            }
            if (generatedBeanName == null || generatedBeanName.length() == 0) {
                generatedBeanName = beanClass.getName();
            }
            id = generatedBeanName;
            int counter = 2;
            while (parserContext.getRegistry().containsBeanDefinition(id)) {
                id = generatedBeanName + (counter++);
            }
        }
        if (id != null && id.length() > 0) {
            if (parserContext.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContext.getRegistry().registerBeanDefinition(id, bd);
        }
        bd.getPropertyValues().addPropertyValue("id", id);
        if (ProtocolConfig.class.equals(beanClass)) {
            MotanNamespaceHandler.protocolDefineNames.add(id);

        } else if (RegistryConfig.class.equals(beanClass)) {
            MotanNamespaceHandler.registryDefineNames.add(id);

        } else if (BasicServiceInterfaceConfig.class.equals(beanClass)) {
            MotanNamespaceHandler.basicServiceConfigDefineNames.add(id);

        } else if (BasicRefererInterfaceConfig.class.equals(beanClass)) {
            MotanNamespaceHandler.basicRefererConfigDefineNames.add(id);

        } else if (ServiceConfigBean.class.equals(beanClass)) {
            String className = element.getAttribute("class");
            if (className != null && className.length() > 0) {
                RootBeanDefinition classDefinition = new RootBeanDefinition();
                classDefinition.setBeanClass(Class.forName(className, true, Thread.currentThread().getContextClassLoader()));
                classDefinition.setLazyInit(false);
                parseProperties(element.getChildNodes(), classDefinition);
                bd.getPropertyValues().addPropertyValue("ref", new BeanDefinitionHolder(classDefinition, id + "Impl"));
            }
        }

        Set<String> props = new HashSet<String>();
        ManagedMap parameters = null;
        // 把配置文件中的可以set的属性放到bd中
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            // 必须是setXXX
            if (name.length() <= 3 || !name.startsWith("set") || !Modifier.isPublic(setter.getModifiers())
                    || setter.getParameterTypes().length != 1) {
                continue;
            }
            String property = (name.substring(3, 4).toLowerCase() + name.substring(4)).replaceAll("_", "-");
            props.add(property);
            if ("id".equals(property)) {
                bd.getPropertyValues().addPropertyValue("id", id);
                continue;
            }
            String value = element.getAttribute(property);
            if ("methods".equals(property)) {
                parseMethods(id, element.getChildNodes(), bd, parserContext);
            }
            if (StringUtils.isBlank(value)) {
                continue;
            }
            value = value.trim();
            if (value.length() == 0) {
                continue;
            }
            Object reference;
            if ("ref".equals(property)) {
                if (parserContext.getRegistry().containsBeanDefinition(value)) {
                    BeanDefinition refBean = parserContext.getRegistry().getBeanDefinition(value);
                    if (!refBean.isSingleton()) {
                        throw new IllegalStateException("The exported service ref " + value + " must be singleton! Please set the " + value
                                + " bean scope to singleton, eg: <bean id=\"" + value + "\" scope=\"singleton\" ...>");
                    }
                }
                reference = new RuntimeBeanReference(value);
            } else if ("protocol".equals(property) && !StringUtils.isBlank(value)) {
                if (!value.contains(",")) {
                    reference = new RuntimeBeanReference(value);
                } else {
                    parseMultiRef("protocols", value, bd, parserContext);
                    reference = null;
                }
            } else if ("registry".equals(property)) {
                parseMultiRef("registries", value, bd, parserContext);
                reference = null;
            } else if ("basicService".equals(property)) {
                reference = new RuntimeBeanReference(value);

            } else if ("basicReferer".equals(property)) {
                reference = new RuntimeBeanReference(value);

            } else if ("extConfig".equals(property)) {
                reference = new RuntimeBeanReference(value);
            } else {
                reference = new TypedStringValue(value);
            }

            if (reference != null) {
                bd.getPropertyValues().addPropertyValue(property, reference);
            }
        }
        if (ProtocolConfig.class.equals(beanClass)) {
            // 把剩余的属性放到protocol的parameters里面
            NamedNodeMap attributes = element.getAttributes();
            int len = attributes.getLength();
            for (int i = 0; i < len; i++) {
                Node node = attributes.item(i);
                String name = node.getLocalName();
                if (!props.contains(name)) {
                    if (parameters == null) {
                        parameters = new ManagedMap();
                    }
                    String value = node.getNodeValue();
                    parameters.put(name, new TypedStringValue(value, String.class));
                }
            }
            bd.getPropertyValues().addPropertyValue("parameters", parameters);
        }
        return bd;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void parseMultiRef(String property, String value, RootBeanDefinition beanDefinition, ParserContext parserContext) {
        String[] values = value.split("\\s*[,]+\\s*");
        ManagedList list = null;
        for (String v : values) {
            if (v != null && v.length() > 0) {
                if (list == null) {
                    list = new ManagedList();
                }
                list.add(new RuntimeBeanReference(v));
            }
        }
        beanDefinition.getPropertyValues().addPropertyValue(property, list);
    }

    private static void parseProperties(NodeList nodeList, RootBeanDefinition beanDefinition) {
        if (nodeList != null && nodeList.getLength() > 0) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    if ("property".equals(node.getNodeName()) || "property".equals(node.getLocalName())) {
                        String name = ((Element) node).getAttribute("name");
                        if (name != null && name.length() > 0) {
                            String value = ((Element) node).getAttribute("value");
                            String ref = ((Element) node).getAttribute("ref");
                            if (value != null && value.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, value);
                            } else if (ref != null && ref.length() > 0) {
                                beanDefinition.getPropertyValues().addPropertyValue(name, new RuntimeBeanReference(ref));
                            } else {
                                throw new UnsupportedOperationException("Unsupported <property name=\"" + name
                                        + "\"> sub tag, Only supported <property name=\"" + name + "\" ref=\"...\" /> or <property name=\""
                                        + name + "\" value=\"...\" />");
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void parseMethods(String id, NodeList nodeList, RootBeanDefinition beanDefinition, ParserContext parserContext)
            throws ClassNotFoundException {
        if (nodeList != null && nodeList.getLength() > 0) {
            ManagedList methods = null;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node instanceof Element) {
                    Element element = (Element) node;
                    if ("method".equals(node.getNodeName()) || "method".equals(node.getLocalName())) {
                        String methodName = element.getAttribute("name");
                        if (methodName == null || methodName.length() == 0) {
                            throw new IllegalStateException("<motan:method> name attribute == null");
                        }
                        if (methods == null) {
                            methods = new ManagedList();
                        }
                        BeanDefinition methodBeanDefinition = parse((Element) node, parserContext, MethodConfig.class, false);
                        String name = id + "." + methodName;
                        BeanDefinitionHolder methodBeanDefinitionHolder = new BeanDefinitionHolder(methodBeanDefinition, name);
                        methods.add(methodBeanDefinitionHolder);
                    }
                }
            }
            if (methods != null) {
                beanDefinition.getPropertyValues().addPropertyValue("methods", methods);
            }
        }
    }

}
