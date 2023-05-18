package com.weibo.api.motan.config.springsupport;

import com.weibo.api.motan.cluster.support.ClusterSupport;
import com.weibo.api.motan.config.*;
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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
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
public class AnnotationBean implements DisposableBean, BeanFactoryPostProcessor, BeanPostProcessor, BeanFactoryAware, Ordered {


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
                    MotanReferer reference = AnnotatedElementUtils.findMergedAnnotation(method, MotanReferer.class);
                    if (reference != null) {
                        Object value = refer(reference, method.getParameterTypes()[0]);
                        if (value != null) {
                            method.invoke(bean, new Object[]{value});
                        }
                    }
                } catch (Throwable t) {
                    throw new BeanInitializationException("Failed to init remote service reference at method " + name
                            + " in class " + bean.getClass().getName(), t);
                }
            }
        }


        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                MotanReferer reference = AnnotatedElementUtils.findMergedAnnotation(field, MotanReferer.class);
                if (reference != null) {
                    Object value = refer(reference, field.getType());
                    if (value != null) {
                        field.set(bean, value);
                    }
                }
            } catch (Throwable t) {
                throw new BeanInitializationException("Failed to init remote service reference at filed " + field.getName()
                        + " in class " + bean.getClass().getName(), t);
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
        MotanService service = AnnotatedElementUtils.findMergedAnnotation(clazz, MotanService.class);
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
                    serviceConfig.setBasicService(beanFactory.getBean(service.basicService(), BasicServiceInterfaceConfig.class));
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
            export(serviceConfig);
        }
        return bean;
    }

    /**
     * 使用protected允许子类进行自定义,这里针对导出进行方法单独提炼，可以更好的扩展，允许去改造导出的策略
     * 目前这个导出是使用同步的，如果一个应用的接口非常多的时候，其实我们的应用是启动很慢的，一直在等待rpc的导出
     * 所以如果有需要改造成异步导出的，可以重写这个方法进行导出
     * @param serviceConfig
     */
    protected void export(ServiceConfigBean<Object> serviceConfig){
        serviceConfigs.add(serviceConfig);
        serviceConfig.export();
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
     * 优先使用spring 容器本地存在的bean，使用protected允许子类进行自定义
     * @param reference
     * @param referenceClass
     * @return
     * @param <T>
     */
    protected  <T> Object localBeanFirstRefer(MotanReferer reference, Class<T> referenceClass) {
        try {
            if (!void.class.equals(reference.interfaceClass())) {
                return this.beanFactory.getBean(reference.interfaceClass());
            } else if (referenceClass.isInterface()) {
                return this.beanFactory.getBean(referenceClass);
            }
        } catch (Exception e) {
            //可能存在找不到bean的情况，这个可以忽略，由motan代理去创建
        }
        return null;
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
        /**
         * 场景：当我们的应用可能存在应用内进行rpc调用，这样即便使用localFirst负载均衡策略，依然会用到网络进行请求，浪费资源，还存在网络超时等情况
         * 如果应用为了后期好拆分微服务可能会按模块去规范代码，约定每个模块使用rpc进行通信，这样前期可以使用单体应用发布，且还可以使用rpc对外服务，
         * 后续应用体量大了后也可以以最小的成本进行改造单体应用进行快速的微服务拆分。这样可以避免前期应用的资源浪费和后期改造成本。
         */
        if (reference.localBeanFirstRefer()) {
            Object ref = localBeanFirstRefer(reference, referenceClass);
            if (ref != null) {
                return ref;
            }
        }
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

    @Override
    public int getOrder() {
        return 0;
    }
}
