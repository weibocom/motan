package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.config.BasicRefererInterfaceConfig;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.springsupport.annotation.ApiReference;
import com.weibo.api.motan.config.springsupport.annotation.ApiService;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Annotation bean for motan
 *
 * <p>
 * Created by fld on 16/5/13.
 */
public class MotanAnnotationBean implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, ApplicationContextAware {
    public static final Pattern COMMA_SPLIT_PATTERN = Pattern.compile("\\s*[,]+\\s*");

    private String annotationPackage;

    private String[] annotationPackages;

    private ApplicationContext applicationContext;

    List<ClusterSupport<?>> clusterSupportList;

    public MotanAnnotationBean() {
        clusterSupportList = new ArrayList<ClusterSupport<?>>();
    }

    private final Set<ServiceConfigBean<?>> serviceConfigs = new ConcurrentHashSet<ServiceConfigBean<?>>();

    private final ConcurrentMap<String, RefererConfigBean> referenceConfigs = new ConcurrentHashMap<String, RefererConfigBean>();


    /**
     * @param beanFactory
     * @throws BeansException
     */
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
            throws BeansException {
        if (annotationPackage == null || annotationPackage.length() == 0) {
            return;
        }
        if (beanFactory instanceof BeanDefinitionRegistry) {
            try {
                // init scanner
                Class<?> scannerClass = ClassUtils.forName("org.springframework.context.annotation.ClassPathBeanDefinitionScanner",
                        MotanAnnotationBean.class.getClassLoader());
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
                        .newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
                // add filter
                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter",
                        MotanAnnotationBean.class.getClassLoader());
                Object filter = filterClass.getConstructor(Class.class).newInstance(ApiService.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter",
                        ClassUtils.forName("org.springframework.core.type.filter.TypeFilter", MotanAnnotationBean.class.getClassLoader()));
                addIncludeFilter.invoke(scanner, filter);
                // scan packages
                Method scan = scannerClass.getMethod("scan", new Class<?>[]{String[].class});
                scan.invoke(scanner, new Object[]{annotationPackages});
            } catch (Throwable e) {
                // spring 2.0
            }
        }
    }

    /**
     * init reference field
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Class<?> clazz = bean.getClass();
        if (isProxyBean(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            String name = method.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())) {
                try {
                    ApiReference reference = method.getAnnotation(ApiReference.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Exception e) {
                    throw new BeanInitializationException("Failed to init remote service reference at method " + name
                            + " in class " + bean.getClass().getName(), e);
                }
            }
        }


        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                ApiReference reference = field.getAnnotation(ApiReference.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Exception e) {
                throw new BeanInitializationException("Failed to init remote service reference at filed " + field.getName()
                        + " in class " + bean.getClass().getName(), e);
            }
        }
        return bean;
    }

    /**
     * init service config and export servcice
     *
     * @param bean
     * @param beanName
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!isMatchPackage(bean)) {
            return bean;
        }
        Class<?> clazz = bean.getClass();
        if (isProxyBean(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        ApiService service = clazz.getAnnotation(ApiService.class);
        if (service != null) {
            ServiceConfigBean<Object> serviceConfig = new ServiceConfigBean<Object>();
            if (void.class.equals(service.interfaceClass())
                    && "".equals(service.interfaceName())) {
                if (clazz.getInterfaces().length > 0) {
                    Class<Object> clz = (Class<Object>) clazz.getInterfaces()[0];
                    serviceConfig.setInterface(clz);
                } else {
                    throw new IllegalStateException("Failed to export remote service class " + clazz.getName()
                            + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                }
            }
            if (applicationContext != null) {
                serviceConfig.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());

                if (service.protocol() != null && service.protocol().length > 0) {
                    List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
                    for (String protocolId : service.protocol()) {
                        if (protocolId != null && protocolId.length() > 0) {
                            protocolConfigs.add((ProtocolConfig) applicationContext.getBean(protocolId, ProtocolConfig.class));
                        }
                    }
                    serviceConfig.setProtocols(protocolConfigs);
                }

                try {
                    serviceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            serviceConfig.setRef(bean);
            serviceConfigs.add(serviceConfig);
            serviceConfig.export();
        }
        return bean;
    }

    /**
     * release service/reference
     *
     * @throws Exception
     */
    public void destroy() throws Exception {
        for (ServiceConfigBean<?> serviceConfig : serviceConfigs) {
            try {
                serviceConfig.unexport();
            } catch (Throwable e) {
                LoggerUtil.error(e.getMessage(), e);
            }
        }
        for (RefererConfigBean<?> referenceConfig : referenceConfigs.values()) {
            try {
                referenceConfig.destroy();
            } catch (Throwable e) {
                LoggerUtil.error(e.getMessage(), e);
            }
        }
    }

    /**
     * refer proxy
     *
     * @param reference
     * @param referenceClass
     * @param <T>
     * @return
     */
    private <T> Object refer(ApiReference reference, Class<?> referenceClass) {
        String interfaceName;
        if (!"".equals(reference.interfaceName())) {
            interfaceName = reference.interfaceName();
        } else if (!void.class.equals(reference.interfaceClass())) {
            interfaceName = reference.interfaceClass().getName();
        } else if (referenceClass.isInterface()) {
            interfaceName = referenceClass.getName();
        } else {
            throw new IllegalStateException("The @Reference undefined interfaceClass or interfaceName, and the property type "
                    + referenceClass.getName() + " is not a interface.");
        }
        String key = reference.group() + "/" + interfaceName + ":" + reference.version();
        RefererConfigBean<T> referenceConfig = referenceConfigs.get(key);
        if (referenceConfig == null) {
            referenceConfig = new RefererConfigBean<T>();
            referenceConfig.setBeanFactory(applicationContext.getAutowireCapableBeanFactory());
            if (void.class.equals(reference.interfaceClass())
                    && "".equals(reference.interfaceName())
                    && referenceClass.isInterface()) {
                referenceConfig.setInterface((Class<T>) referenceClass);
            }

            if (applicationContext != null) {

                if (reference.group() != null && reference.group().length() > 0) {
                    referenceConfig.setGroup(reference.group());
                }

                if (reference.version() != null && reference.version().length() > 0) {
                    referenceConfig.setVersion(reference.version());
                }

                if (reference.protocol() != null && reference.protocol().length() > 0) {
                    //多个PROTOCOL
                    if (!reference.protocol().contains(",")) {
                        ProtocolConfig pc = applicationContext.getBean(reference.protocol(), ProtocolConfig.class);
                        if (pc != null) {
                            referenceConfig.setProtocol(pc);
                        }
                    } else {
                        String[] values = reference.protocol().split("\\s*[,]+\\s*");
                        List<ProtocolConfig> protocolConfigs = new ArrayList<ProtocolConfig>();
                        for (int i = 0; i < values.length; i++) {
                            String val = values[i];
                            ProtocolConfig pc = applicationContext.getBean(val, ProtocolConfig.class);
                            if (pc != null) {
                                protocolConfigs.add(pc);
                            }
                        }
                        referenceConfig.setProtocols(protocolConfigs);
                    }
                }

                if (reference.requestTimeout() != null && reference.requestTimeout().length() > 0) {
                    referenceConfig.setRequestTimeout(Integer.valueOf(reference.requestTimeout()));
                }

                if (reference.directUrl() != null && reference.directUrl().length() > 0) {
                    referenceConfig.setDirectUrl(reference.directUrl());
                }

                if (reference.basicReferer() != null && reference.basicReferer().length() > 0) {
                    BasicRefererInterfaceConfig biConfig = applicationContext.getBean(reference.basicReferer(), BasicRefererInterfaceConfig.class);
                    if (biConfig != null) {
                        referenceConfig.setBasicReferer(biConfig);
                    }
                }

                if (reference.throwException()) {
                    referenceConfig.setThrowException(true);
                }

                if (reference.shareChannel()) {
                    referenceConfig.setShareChannel(true);
                }


                try {
                    referenceConfig.afterPropertiesSet();
                } catch (RuntimeException e) {
                    throw (RuntimeException) e;
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            }
            referenceConfigs.putIfAbsent(key, referenceConfig);
            referenceConfig = referenceConfigs.get(key);
        }

        return referenceConfig.getRef();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private boolean isMatchPackage(Object bean) {
        if (annotationPackages == null || annotationPackages.length == 0) {
            return true;
        }
        Class clazz = bean.getClass();
        if (isProxyBean(bean)) {
            clazz = AopUtils.getTargetClass(bean);
        }
        String beanClassName = clazz.getName();
        for (String pkg : annotationPackages) {
            if (beanClassName.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProxyBean(Object bean) {
        return AopUtils.isAopProxy(bean);
    }

    public String getPackageName() {
        return annotationPackage;
    }

    public void setPackageName(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : COMMA_SPLIT_PATTERN.split(annotationPackage);
    }

}
