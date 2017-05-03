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

package com.weibo.api.motan.core.extension;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * <pre>
 * 	扩展增加的方式：
 * 		支持 JDK ServiceProvider 
 * 
 * 		支持 weibo:spi 配置
 * </pre>
 * 
 * @author maijunsheng
 * @version 创建时间：2013-5-28
 * 
 */
public class ExtensionLoader<T> {

    private static ConcurrentMap<Class<?>, ExtensionLoader<?>> extensionLoaders = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    private ConcurrentMap<String, T> singletonInstances = null;
    private ConcurrentMap<String, Class<T>> extensionClasses = null;

    private Class<T> type;
    private volatile boolean init = false;

    // spi path prefix
    private static final String PREFIX = "META-INF/services/";
    private ClassLoader classLoader;

    private ExtensionLoader(Class<T> type) {
        this(type, Thread.currentThread().getContextClassLoader());
    }

    private ExtensionLoader(Class<T> type, ClassLoader classLoader) {
        this.type = type;
        this.classLoader = classLoader;
    }

    private void checkInit() {
        if (!init) {
            loadExtensionClasses();
        }
    }

    public Class<T> getExtensionClass(String name) {
        checkInit();

        return extensionClasses.get(name);
    }

    public T getExtension(String name) {
        checkInit();

        if (name == null) {
            return null;
        }

        try {
            Spi spi = type.getAnnotation(Spi.class);

            if (spi.scope() == Scope.SINGLETON) {
                return getSingletonInstance(name);
            } else {
                Class<T> clz = extensionClasses.get(name);

                if (clz == null) {
                    return null;
                }

                return clz.newInstance();
            }
        } catch (Exception e) {
            failThrows(type, "Error when getExtension " + name, e);
        }

        return null;
    }

    private T getSingletonInstance(String name) throws InstantiationException, IllegalAccessException {
        T obj = singletonInstances.get(name);

        if (obj != null) {
            return obj;
        }

        Class<T> clz = extensionClasses.get(name);

        if (clz == null) {
            return null;
        }

        synchronized (singletonInstances) {
            obj = singletonInstances.get(name);
            if (obj != null) {
                return obj;
            }

            obj = clz.newInstance();
            singletonInstances.put(name, obj);
        }

        return obj;
    }

    public void addExtensionClass(Class<T> clz) {
        if (clz == null) {
            return;
        }

        checkInit();

        checkExtensionType(clz);

        String spiName = getSpiName(clz);

        synchronized (extensionClasses) {
            if (extensionClasses.containsKey(spiName)) {
                failThrows(clz, ":Error spiName already exist " + spiName);
            } else {
                extensionClasses.put(spiName, clz);
            }
        }
    }

    private synchronized void loadExtensionClasses() {
        if (init) {
            return;
        }

        extensionClasses = loadExtensionClasses(PREFIX);
        singletonInstances = new ConcurrentHashMap<String, T>();

        init = true;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        checkInterfaceType(type);

        ExtensionLoader<T> loader = (ExtensionLoader<T>) extensionLoaders.get(type);

        if (loader == null) {
            loader = initExtensionLoader(type);
        }
        return loader;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> ExtensionLoader<T> initExtensionLoader(Class<T> type) {
        ExtensionLoader<T> loader = (ExtensionLoader<T>) extensionLoaders.get(type);

        if (loader == null) {
            loader = new ExtensionLoader<T>(type);

            extensionLoaders.putIfAbsent(type, loader);

            loader = (ExtensionLoader<T>) extensionLoaders.get(type);
        }

        return loader;
    }

    /**
     * 有些地方需要spi的所有激活的instances，所以需要能返回一个列表的方法 注意：1 SpiMeta 中的active 为true； 2
     * 按照spiMeta中的sequence进行排序 FIXME： 是否需要对singleton来区分对待，后面再考虑 fishermen
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<T> getExtensions(String key) {
        checkInit();

        if (extensionClasses.size() == 0) {
            return Collections.emptyList();
        }

        // 如果只有一个实现，直接返回
        List<T> exts = new ArrayList<T>(extensionClasses.size());

        // 多个实现，按优先级排序返回
        for (Map.Entry<String, Class<T>> entry : extensionClasses.entrySet()) {
            Activation activation = entry.getValue().getAnnotation(Activation.class);
            if (StringUtils.isBlank(key)) {
                exts.add(getExtension(entry.getKey()));
            } else if (activation != null && activation.key() != null) {
                for (String k : activation.key()) {
                    if (key.equals(k)) {
                        exts.add(getExtension(entry.getKey()));
                        break;
                    }
                }
            }
        }
        Collections.sort(exts, new ActivationComparator<T>());
        return exts;
    }

    /**
     * check clz
     * 
     * <pre>
	 * 		1.  is interface
	 * 		2.  is contains @Spi annotation
	 * </pre>
     * 
     * 
     * @param <T>
     * @param clz
     */
    private static <T> void checkInterfaceType(Class<T> clz) {
        if (clz == null) {
            failThrows(clz, "Error extension type is null");
        }

        if (!clz.isInterface()) {
            failThrows(clz, "Error extension type is not interface");
        }

        if (!isSpiType(clz)) {
            failThrows(clz, "Error extension type without @Spi annotation");
        }
    }

    /**
     * check extension clz
     * 
     * <pre>
	 * 		1) is public class
	 * 		2) contain public constructor and has not-args constructor
	 * 		3) check extension clz instanceof Type.class
	 * </pre>
     * 
     * @param clz
     */
    private void checkExtensionType(Class<T> clz) {
        checkClassPublic(clz);

        checkConstructorPublic(clz);

        checkClassInherit(clz);
    }

    private void checkClassInherit(Class<T> clz) {
        if (!type.isAssignableFrom(clz)) {
            failThrows(clz, "Error is not instanceof " + type.getName());
        }
    }

    private void checkClassPublic(Class<T> clz) {
        if (!Modifier.isPublic(clz.getModifiers())) {
            failThrows(clz, "Error is not a public class");
        }
    }

    private void checkConstructorPublic(Class<T> clz) {
        Constructor<?>[] constructors = clz.getConstructors();

        if (constructors == null || constructors.length == 0) {
            failThrows(clz, "Error has no public no-args constructor");
        }

        for (Constructor<?> constructor : constructors) {
            if (Modifier.isPublic(constructor.getModifiers()) && constructor.getParameterTypes().length == 0) {
                return;
            }
        }

        failThrows(clz, "Error has no public no-args constructor");
    }

    private static <T> boolean isSpiType(Class<T> clz) {
        return clz.isAnnotationPresent(Spi.class);
    }

    private ConcurrentMap<String, Class<T>> loadExtensionClasses(String prefix) {
        String fullName = prefix + type.getName();
        List<String> classNames = new ArrayList<String>();

        try {
            Enumeration<URL> urls;
            if (classLoader == null) {
                urls = ClassLoader.getSystemResources(fullName);
            } else {
                urls = classLoader.getResources(fullName);
            }

            if (urls == null || !urls.hasMoreElements()) {
                return new ConcurrentHashMap<String, Class<T>>();
            }

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();

                parseUrl(type, url, classNames);
            }
        } catch (Exception e) {
            throw new MotanFrameworkException(
                    "ExtensionLoader loadExtensionClasses error, prefix: " + prefix + " type: " + type.getClass(), e);
        }

        return loadClass(classNames);
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<String, Class<T>> loadClass(List<String> classNames) {
        ConcurrentMap<String, Class<T>> map = new ConcurrentHashMap<String, Class<T>>();

        for (String className : classNames) {
            try {
                Class<T> clz;
                if (classLoader == null) {
                    clz = (Class<T>) Class.forName(className);
                } else {
                    clz = (Class<T>) Class.forName(className, true, classLoader);
                }

                checkExtensionType(clz);

                String spiName = getSpiName(clz);

                if (map.containsKey(spiName)) {
                    failThrows(clz, ":Error spiName already exist " + spiName);
                } else {
                    map.put(spiName, clz);
                }
            } catch (Exception e) {
                failLog(type, "Error load spi class", e);
            }
        }

        return map;

    }

    /**
     * 获取扩展点的名字
     * 
     * <pre>
	 * 		如果扩展类有SpiMeta的注解，那么获取对应的name，如果没有的话获取classname
	 * </pre>
     * 
     * @param clz
     * @return
     */
    public String getSpiName(Class<?> clz) {
        SpiMeta spiMeta = clz.getAnnotation(SpiMeta.class);

        String name = (spiMeta != null && !"".equals(spiMeta.name())) ? spiMeta.name() : clz.getSimpleName();

        return name;
    }

    private void parseUrl(Class<T> type, URL url, List<String> classNames) throws ServiceConfigurationError {
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, MotanConstants.DEFAULT_CHARACTER));

            String line = null;
            int indexNumber = 0;

            while ((line = reader.readLine()) != null) {
                indexNumber++;
                parseLine(type, url, line, indexNumber, classNames);
            }
        } catch (Exception x) {
            failLog(type, "Error reading spi configuration file", x);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException y) {
                failLog(type, "Error closing spi configuration file", y);
            }
        }
    }

    private void parseLine(Class<T> type, URL url, String line, int lineNumber, List<String> names) throws IOException,
            ServiceConfigurationError {
        int ci = line.indexOf('#');

        if (ci >= 0) {
            line = line.substring(0, ci);
        }

        line = line.trim();

        if (line.length() <= 0) {
            return;
        }

        if ((line.indexOf(' ') >= 0) || (line.indexOf('\t') >= 0)) {
            failThrows(type, url, lineNumber, "Illegal spi configuration-file syntax");
        }

        int cp = line.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            failThrows(type, url, lineNumber, "Illegal spi provider-class name: " + line);
        }

        for (int i = Character.charCount(cp); i < line.length(); i += Character.charCount(cp)) {
            cp = line.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                failThrows(type, url, lineNumber, "Illegal spi provider-class name: " + line);
            }
        }

        if (!names.contains(line)) {
            names.add(line);
        }
    }

    private static <T> void failLog(Class<T> type, String msg, Throwable cause) {
        LoggerUtil.error(type.getName() + ": " + msg, cause);
    }

    private static <T> void failThrows(Class<T> type, String msg, Throwable cause) {
        throw new MotanFrameworkException(type.getName() + ": " + msg, cause);
    }

    private static <T> void failThrows(Class<T> type, String msg) {
        throw new MotanFrameworkException(type.getName() + ": " + msg);
    }

    private static <T> void failThrows(Class<T> type, URL url, int line, String msg) throws ServiceConfigurationError {
        failThrows(type, url + ":" + line + ": " + msg);
    }

}
