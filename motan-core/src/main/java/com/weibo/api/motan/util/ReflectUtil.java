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

package com.weibo.api.motan.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 反射相关的辅助类
 *
 * @author maijunsheng
 * @version 创建时间：2013-5-23
 *
 */
public class ReflectUtil {
    public static final String PARAM_CLASS_SPLIT = ",";
    public static final String EMPTY_PARAM = "void";
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];

    private static final ConcurrentMap<String, Class<?>> name2ClassCache = new ConcurrentHashMap<String, Class<?>>();
    private static final ConcurrentMap<Class<?>, String> class2NameCache = new ConcurrentHashMap<Class<?>, String>();

    private static final String[] PRIMITIVE_NAMES = new String[] {"boolean", "byte", "char", "double", "float", "int", "long", "short",
            "void"};

    private static final Class<?>[] PRIMITIVE_CLASSES = new Class[] {boolean.class, byte.class, char.class, double.class, float.class,
            int.class, long.class, short.class, Void.TYPE};

    private static final int PRIMITIVE_CLASS_NAME_MAX_LENGTH = 7;

    /**
     * 获取method方式的接口参数，以逗号分割，拼接clz列表。 如果没有参数，那么void表示
     *
     * @param method
     * @return
     */
    public static String getMethodParamDesc(Method method) {
        if (method.getParameterTypes() == null || method.getParameterTypes().length == 0) {
            return EMPTY_PARAM;
        }

        StringBuilder builder = new StringBuilder();

        Class<?>[] clzs = method.getParameterTypes();

        for (Class<?> clz : clzs) {
            String className = getName(clz);
            builder.append(className).append(PARAM_CLASS_SPLIT);
        }

        return builder.substring(0, builder.length() - 1);
    }

    /**
     * 获取方法的标示 : method_name + "(" + paramDesc + ")"
     *
     * @param method
     * @return
     */
    public static String getMethodDesc(Method method) {
        String methodParamDesc = getMethodParamDesc(method);
        return getMethodDesc(method.getName(), methodParamDesc);
    }

    /**
     * 获取方法的标示 : method_name + "(" + paramDesc + ")"
     *
     * @param
     * @return
     */
    public static String getMethodDesc(String methodName, String paramDesc) {
        if (paramDesc == null) {
            return methodName + "()";
        } else {
            return methodName + "(" + paramDesc + ")";
        }
    }

    public static Class<?>[] forNames(String classList) throws ClassNotFoundException {
        if (classList == null || "".equals(classList) || EMPTY_PARAM.equals(classList)) {
            return EMPTY_CLASS_ARRAY;
        }

        String[] classNames = classList.split(PARAM_CLASS_SPLIT);
        Class<?>[] classTypes = new Class<?>[classNames.length];
        for (int i = 0; i < classNames.length; i++) {
            String className = classNames[i];

            classTypes[i] = forName(className);
        }

        return classTypes;
    }

    public static Class<?> forName(String className) throws ClassNotFoundException {
        if (null == className || "".equals(className)) {
            return null;
        }

        Class<?> clz = name2ClassCache.get(className);

        if (clz != null) {
            return clz;
        }

        clz = forNameWithoutCache(className);

        // 应该没有内存消耗过多的可能，除非有些代码很恶心，创建特别多的类
        name2ClassCache.putIfAbsent(className, clz);

        return clz;
    }

    private static Class<?> forNameWithoutCache(String className) throws ClassNotFoundException {
        if (!className.endsWith("[]")) { // not array
            Class<?> clz = getPrimitiveClass(className);

            clz = (clz != null) ? clz : Class.forName(className, true, Thread.currentThread().getContextClassLoader());
            return clz;
        }

        int dimensionSiz = 0;

        while (className.endsWith("[]")) {
            dimensionSiz++;

            className = className.substring(0, className.length() - 2);
        }

        int[] dimensions = new int[dimensionSiz];

        Class<?> clz = getPrimitiveClass(className);

        if (clz == null) {
            clz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        }

        return Array.newInstance(clz, dimensions).getClass();
    }

    /**
     * 需要支持一维数组、二维数组等
     *
     * @param
     * @return
     */
    public static String getName(Class<?> clz) {
        if (clz == null) {
            return null;
        }

        String className = class2NameCache.get(clz);

        if (className != null) {
            return className;
        }

        className = getNameWithoutCache(clz);

        // 与name2ClassCache同样道理，如果没有恶心的代码，这块内存大小应该可控
        class2NameCache.putIfAbsent(clz, className);

        return className;
    }

    private static String getNameWithoutCache(Class<?> clz) {
        if (!clz.isArray()) {
            return clz.getName();
        }

        StringBuilder sb = new StringBuilder();
        while (clz.isArray()) {
            sb.append("[]");
            clz = clz.getComponentType();
        }

        return clz.getName() + sb.toString();
    }

    public static Class<?> getPrimitiveClass(String name) {
        // check if is primitive class
        if (name.length() <= PRIMITIVE_CLASS_NAME_MAX_LENGTH) {
            int index = Arrays.binarySearch(PRIMITIVE_NAMES, name);
            if (index >= 0) {
                return PRIMITIVE_CLASSES[index];
            }
        }
        return null;
    }

    /**
     * 获取clz public method
     *
     * <pre>
     *      1）不包含构造函数
     *      2）不包含Object.class
     *      3）包含该clz的父类的所有public方法
     * </pre>
     *
     * @param clz
     * @return
     */
    public static List<Method> getPublicMethod(Class<?> clz) {
        Method[] methods = clz.getMethods();
        List<Method> ret = new ArrayList<Method>();

        for (Method method : methods) {

            boolean isPublic = Modifier.isPublic(method.getModifiers());
            boolean isNotObjectClass = method.getDeclaringClass() != Object.class;

            if (isPublic && isNotObjectClass) {
                ret.add(method);
            }
        }

        return ret;
    }

    public static Object getEmptyObject(Class<?> returnType) {
        return getEmptyObject(returnType, new HashMap<Class<?>, Object>(), 0);
    }

    private static Object getEmptyObject(Class<?> returnType, Map<Class<?>, Object> emptyInstances, int level) {
        if (level > 2) return null;
        if (returnType == null) {
            return null;
        } else if (returnType == boolean.class || returnType == Boolean.class) {
            return false;
        } else if (returnType == char.class || returnType == Character.class) {
            return '\0';
        } else if (returnType == byte.class || returnType == Byte.class) {
            return (byte) 0;
        } else if (returnType == short.class || returnType == Short.class) {
            return (short) 0;
        } else if (returnType == int.class || returnType == Integer.class) {
            return 0;
        } else if (returnType == long.class || returnType == Long.class) {
            return 0L;
        } else if (returnType == float.class || returnType == Float.class) {
            return 0F;
        } else if (returnType == double.class || returnType == Double.class) {
            return 0D;
        } else if (returnType.isArray()) {
            return Array.newInstance(returnType.getComponentType(), 0);
        } else if (returnType.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<Object>(0);
        } else if (returnType.isAssignableFrom(HashSet.class)) {
            return new HashSet<Object>(0);
        } else if (returnType.isAssignableFrom(HashMap.class)) {
            return new HashMap<Object, Object>(0);
        } else if (String.class.equals(returnType)) {
            return "";
        } else if (!returnType.isInterface()) {
            try {
                Object value = emptyInstances.get(returnType);
                if (value == null) {
                    value = returnType.newInstance();
                    emptyInstances.put(returnType, value);
                }
                Class<?> cls = value.getClass();
                while (cls != null && cls != Object.class) {
                    Field[] fields = cls.getDeclaredFields();
                    for (Field field : fields) {
                        Object property = getEmptyObject(field.getType(), emptyInstances, level + 1);
                        if (property != null) {
                            try {
                                if (!field.isAccessible()) {
                                    field.setAccessible(true);
                                }
                                field.set(value, property);
                            } catch (Throwable e) {}
                        }
                    }
                    cls = cls.getSuperclass();
                }
                return value;
            } catch (Throwable e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
