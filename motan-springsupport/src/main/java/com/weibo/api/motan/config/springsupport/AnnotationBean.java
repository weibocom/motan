package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.config.BasicRefererInterfaceConfig;
import com.weibo.api.motan.config.BasicServiceInterfaceConfig;
import com.weibo.api.motan.config.ConfigUtil;
import com.weibo.api.motan.config.ExtConfig;
import com.weibo.api.motan.config.ProtocolConfig;
import com.weibo.api.motan.config.RegistryConfig;
import com.weibo.api.motan.config.springsupport.annotation.MotanReferer;
import com.weibo.api.motan.config.springsupport.annotation.MotanService;
import com.weibo.api.motan.config.springsupport.util.SpringBeanUtil;
import com.weibo.api.motan.rpc.init.Initializable;
import com.weibo.api.motan.rpc.init.InitializationFactory;
import com.weibo.api.motan.util.ConcurrentHashSet;
import com.weibo.api.motan.util.LoggerUtil;

import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author fld
 *
 * Annotation bean for motan
 * <p>
 * <p>
 * Created by fld on 16/5/13.
 */
public class AnnotationBean implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, BeanFactoryAware {


    private String id;

    private String annotationPackage;

    private String[] annotationPackages;


    private BeanFactory beanFactory;

    List<ClusterSupport<?>> clusterSupportList = new ArrayList<ClusterSupport<?>>();

    public AnnotationBean() {
    }

    private final Set<ServiceConfigBean<?>> serviceConfigs = new ConcurrentHashSet<ServiceConfigBean<?>>();

    private final ConcurrentMap<String, RefererConfigBean> referenceConfigs = new ConcurrentHashMap<String, RefererConfigBean>();
    static{
        //custom Initializable before motan beans inited
        Initializable initialization = InitializationFactory.getInitialization();
        initialization.init();
    }

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
                        AnnotationBean.class.getClassLoader());
                Object scanner = scannerClass.getConstructor(new Class<?>[]{BeanDefinitionRegistry.class, boolean.class})
                        .newInstance(new Object[]{(BeanDefinitionRegistry) beanFactory, true});
                // add filter
                Class<?> filterClass = ClassUtils.forName("org.springframework.core.type.filter.AnnotationTypeFilter",
                        AnnotationBean.class.getClassLoader());
                Object filter = filterClass.getConstructor(Class.class).newInstance(MotanService.class);
                Method addIncludeFilter = scannerClass.getMethod("addIncludeFilter",
                        ClassUtils.forName("org.springframework.core.type.filter.TypeFilter", AnnotationBean.class.getClassLoader()));
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
                    MotanReferer reference = method.getAnnotation(MotanReferer.class);
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
                MotanReferer reference = field.getAnnotation(MotanReferer.class);
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
        MotanService service = clazz.getAnnotation(MotanService.class);
        if (service != null) {
            ServiceConfigBean<Object> serviceConfig = new ServiceConfigBean<Object>();
            if (void.class.equals(service.interfaceClass())) {
                if (clazz.getInterfaces().length > 0) {
                    Class<Object> clz = (Class<Object>) clazz.getInterfaces()[0];
                    serviceConfig.setInterface(clz);
                } else {
                    throw new IllegalStateException("Failed to export remote service class " + clazz.getName()
                            + ", cause: The @Service undefined interfaceClass or interfaceName, and the service class unimplemented any interfaces.");
                }
            } else {
                serviceConfig.setInterface((Class<Object>) service.interfaceClass());
            }
            if (beanFactory != null) {

                serviceConfig.setBeanFactory(beanFactory);

                if (service.basicService() != null && service.basicService().length() > 0) {
                    serviceConfig.setBasicServiceConfig(beanFactory.getBean(service.basicService(), BasicServiceInterfaceConfig.class));
                }

                if (service.export() != null && service.export().length() > 0) {
                    serviceConfig.setExport(service.export());
                }

                if (service.host() != null && service.host().length() > 0) {
                    serviceConfig.setHost(service.host());
                }

                String protocolValue = null;
                if (service.protocol() != null && service.protocol().length() > 0) {
                    protocolValue = service.protocol();
                } else if (service.export() != null && service.export().length() > 0) {
                    protocolValue = ConfigUtil.extractProtocols(service.export());
                }

                if (!StringUtils.isBlank(protocolValue)) {
                    List<ProtocolConfig> protocolConfigs = SpringBeanUtil.getMultiBeans(beanFactory, protocolValue, SpringBeanUtil.COMMA_SPLIT_PATTERN,
                            ProtocolConfig.class);
                    serviceConfig.setProtocols(protocolConfigs);
                }

//                String[] methods() default {};

                if (service.registry() != null && service.registry().length() > 0) {
                    List<RegistryConfig> registryConfigs = SpringBeanUtil.getMultiBeans(beanFactory, service.registry
                            (), SpringBeanUtil.COMMA_SPLIT_PATTERN, RegistryConfig.class);
                    serviceConfig.setRegistries(registryConfigs);
                }

                if (service.extConfig() != null && service.extConfig().length() > 0) {
                    serviceConfig.setExtConfig(beanFactory.getBean(service.extConfig(), ExtConfig.class));
                }

                if (service.application() != null && service.application().length() > 0) {
                    serviceConfig.setApplication(service.application());
                }
                if (service.module() != null && service.module().length() > 0) {
                    serviceConfig.setModule(service.module());
                }
                if (service.group() != null && service.group().length() > 0) {
                    serviceConfig.setGroup(service.group());
                }

                if (service.version() != null && service.version().length() > 0) {
                    serviceConfig.setVersion(service.version());
                }

                if (service.proxy() != null && service.proxy().length() > 0) {
                    serviceConfig.setProxy(service.proxy());
                }

                if (service.filter() != null && service.filter().length() > 0) {
                    serviceConfig.setFilter(service.filter());
                }


                if (service.actives() > 0) {
                    serviceConfig.setActives(service.actives());
                }

                if(service.async()) {
                    serviceConfig.setAsync(service.async());
                }

                if (service.mock() != null && service.mock().length() > 0) {
                    serviceConfig.setMock(service.mock());
                }


                // 是否共享 channel
                if (service.shareChannel()) {
                    serviceConfig.setShareChannel(service.shareChannel());
                }

                // if throw exception when call failure，the default value is ture
                if (service.throwException()) {
                    serviceConfig.setThrowException(service.throwException());
                }
                if(service.requestTimeout()>0) {
                    serviceConfig.setRequestTimeout(service.requestTimeout());
                }
                if (service.register()) {
                    serviceConfig.setRegister(service.register());
                }
                if (service.accessLog()) {
                    serviceConfig.setAccessLog("true");
                }
                if (service.check()) {
                    serviceConfig.setCheck("true");
                }
                if (service.usegz()) {
                    serviceConfig.setUsegz(service.usegz());
                }

                if(service.retries()>0) {
                    serviceConfig.setRetries(service.retries());
                }

                if(service.mingzSize()>0) {
                    serviceConfig.setMingzSize(service.mingzSize());
                }

                if (service.codec() != null && service.codec().length() > 0) {
                    serviceConfig.setCodec(service.codec());
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
    private <T> Object refer(MotanReferer reference, Class<?> referenceClass) {
        String interfaceName;
        if (!void.class.equals(reference.interfaceClass())) {
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
            referenceConfig.setBeanFactory(beanFactory);
            if (void.class.equals(reference.interfaceClass())
                    && referenceClass.isInterface()) {
                referenceConfig.setInterface((Class<T>) referenceClass);
            } else if (!void.class.equals(reference.interfaceClass())) {
                referenceConfig.setInterface((Class<T>) reference.interfaceClass());
            }

            if (beanFactory != null) {
                if (reference.protocol() != null && reference.protocol().length() > 0) {
                    //多个PROTOCOL
                    List<ProtocolConfig> protocolConfigs = SpringBeanUtil.getMultiBeans(beanFactory, reference
                            .protocol(), SpringBeanUtil.COMMA_SPLIT_PATTERN, ProtocolConfig.class);
                    referenceConfig.setProtocols(protocolConfigs);
                }

                if (reference.directUrl() != null && reference.directUrl().length() > 0) {
                    referenceConfig.setDirectUrl(reference.directUrl());
                }

                if (reference.basicReferer() != null && reference.basicReferer().length() > 0) {
                    BasicRefererInterfaceConfig biConfig = beanFactory.getBean(reference.basicReferer(), BasicRefererInterfaceConfig.class);
                    if (biConfig != null) {
                        referenceConfig.setBasicReferer(biConfig);
                    }
                }

                if (reference.client() != null && reference.client().length() > 0) {
                    //TODO?
//                    referenceConfig.setC(reference.client());
                }


//                String[] methods() default {};

                if (reference.registry() != null && reference.registry().length() > 0) {
                    List<RegistryConfig> registryConfigs = SpringBeanUtil.getMultiBeans(beanFactory, reference
                            .registry(), SpringBeanUtil.COMMA_SPLIT_PATTERN, RegistryConfig.class);
                    referenceConfig.setRegistries(registryConfigs);
                }

                if (reference.extConfig() != null && reference.extConfig().length() > 0) {
                    referenceConfig.setExtConfig(beanFactory.getBean(reference.extConfig(), ExtConfig.class));
                }

                if (reference.application() != null && reference.application().length() > 0) {
                    referenceConfig.setApplication(reference.application());
                }
                if (reference.module() != null && reference.module().length() > 0) {
                    referenceConfig.setModule(reference.module());
                }
                if (reference.group() != null && reference.group().length() > 0) {
                    referenceConfig.setGroup(reference.group());
                }

                if (reference.version() != null && reference.version().length() > 0) {
                    referenceConfig.setVersion(reference.version());
                }

                if (reference.proxy() != null && reference.proxy().length() > 0) {
                    referenceConfig.setProxy(reference.proxy());
                }

                if (reference.filter() != null && reference.filter().length() > 0) {
                    referenceConfig.setFilter(reference.filter());
                }


                if (reference.actives() > 0) {
                    referenceConfig.setActives(reference.actives());
                }

                if (reference.async()) {
                    referenceConfig.setAsync(reference.async());
                }


                if (reference.mock() != null && reference.mock().length() > 0) {
                    referenceConfig.setMock(reference.mock());
                }

                if (reference.shareChannel()) {
                    referenceConfig.setShareChannel(reference.shareChannel());
                }

                // if throw exception when call failure，the default value is ture
                if (reference.throwException()) {
                    referenceConfig.setThrowException(reference.throwException());
                }
                if(reference.requestTimeout()>0) {
                    referenceConfig.setRequestTimeout(reference.requestTimeout());
                }
                if (reference.register()) {
                    referenceConfig.setRegister(reference.register());
                }
                if (reference.accessLog()) {
                    referenceConfig.setAccessLog("true");
                }
                if (reference.check()) {
                    referenceConfig.setCheck("true");
                }
                if(reference.retries()>0) {
                    referenceConfig.setRetries(reference.retries());
                }
                if (reference.usegz()) {
                    referenceConfig.setUsegz(reference.usegz());
                }
                if(reference.mingzSize()>0) {
                    referenceConfig.setMingzSize(reference.mingzSize());
                }
                if (reference.codec() != null && reference.codec().length() > 0) {
                    referenceConfig.setCodec(reference.codec());
                }


                if (reference.mean() != null && reference.mean().length() > 0) {
                    referenceConfig.setMean(reference.mean());
                }
                if (reference.p90() != null && reference.p90().length() > 0) {
                    referenceConfig.setP90(reference.p90());
                }
                if (reference.p99() != null && reference.p99().length() > 0) {
                    referenceConfig.setP99(reference.p99());
                }
                if (reference.p999() != null && reference.p999().length() > 0) {
                    referenceConfig.setP999(reference.p999());
                }
                if (reference.errorRate() != null && reference.errorRate().length() > 0) {
                    referenceConfig.setErrorRate(reference.errorRate());
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

    public String getPackage() {
        return annotationPackage;
    }

    public void setPackage(String annotationPackage) {
        this.annotationPackage = annotationPackage;
        this.annotationPackages = (annotationPackage == null || annotationPackage.length() == 0) ? null
                : annotationPackage.split(SpringBeanUtil.COMMA_SPLIT_PATTERN);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
