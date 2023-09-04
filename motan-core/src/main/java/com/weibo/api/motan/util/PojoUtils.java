package com.weibo.api.motan.util;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * @author dinglang
 * @since 2023/9/4
 */
public class PojoUtils {

    private static final ConcurrentMap<String, Method> NAME_METHODS_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<Class<?>, ConcurrentMap<String, Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<>();

    private static final ConcurrentMap<String, Object> CLASS_NOT_FOUND_CACHE = new ConcurrentHashMap<>();

    private static final Object NOT_FOUND_VALUE = new Object();

    public static Object[] realize(Object[] objs, Class<?>[] types, Type[] gtypes) {
        if (objs.length != types.length || objs.length != gtypes.length) {
            throw new IllegalArgumentException("args.length != types.length");
        }
        Object[] dests = new Object[objs.length];
        for (int i = 0; i < objs.length; i++) {
            dests[i] = realize(objs[i], types[i], gtypes[i]);
        }
        return dests;
    }

    public static Object realize(Object pojo, Class<?> type, Type genericType) {
        return realize0(pojo, type, genericType, new IdentityHashMap<>());
    }

    private static Object realize0(Object pojo, Class<?> type, Type genericType, final Map<Object, Object> history) {
        return realize1(pojo, type, genericType, new HashMap<>(8), history);
    }

    private static Object realize1(Object pojo, Class<?> type, Type genericType, final Map<String, Type> mapParent, final Map<Object, Object> history) {
        if (pojo == null) {
            return null;
        }

        if (type != null && type.isEnum() && pojo.getClass() == String.class) {
            return Enum.valueOf((Class<Enum>) type, (String) pojo);
        }

        if (ReflectUtil.isPrimitives(pojo.getClass())
                && !(type != null && type.isArray()
                && type.getComponentType().isEnum()
                && pojo.getClass() == String[].class)) {
            return CompatibleTypeUtils.compatibleTypeConvert(pojo, type);
        }

        Object o = history.get(pojo);

        if (o != null) {
            return o;
        }

        history.put(pojo, pojo);

        Map<String, Type> mapGeneric = new HashMap<>(8);
        mapGeneric.putAll(mapParent);
        TypeVariable<? extends Class<?>>[] typeParameters = type.getTypeParameters();
        if (genericType instanceof ParameterizedType && typeParameters.length > 0) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < typeParameters.length; i++) {
                if (!(actualTypeArguments[i] instanceof TypeVariable)) {
                    mapGeneric.put(typeParameters[i].getTypeName(), actualTypeArguments[i]);
                }
            }
        }

        if (pojo.getClass().isArray()) {
            if (Collection.class.isAssignableFrom(type)) {
                Class<?> ctype = pojo.getClass().getComponentType();
                int len = Array.getLength(pojo);
                Collection dest = createCollection(type, len);
                history.put(pojo, dest);
                for (int i = 0; i < len; i++) {
                    Object obj = Array.get(pojo, i);
                    Object value = realize1(obj, ctype, null, mapGeneric, history);
                    dest.add(value);
                }
                return dest;
            } else {
                Class<?> ctype = (type != null && type.isArray() ? type.getComponentType() : pojo.getClass().getComponentType());
                int len = Array.getLength(pojo);
                Object dest = Array.newInstance(ctype, len);
                history.put(pojo, dest);
                for (int i = 0; i < len; i++) {
                    Object obj = Array.get(pojo, i);
                    Object value = realize1(obj, ctype, null, mapGeneric, history);
                    Array.set(dest, i, value);
                }
                return dest;
            }
        }

        if (pojo instanceof Collection<?>) {
            if (type.isArray()) {
                Class<?> ctype = type.getComponentType();
                Collection<Object> src = (Collection<Object>) pojo;
                int len = src.size();
                Object dest = Array.newInstance(ctype, len);
                history.put(pojo, dest);
                int i = 0;
                for (Object obj : src) {
                    Object value = realize1(obj, ctype, null, mapGeneric, history);
                    Array.set(dest, i, value);
                    i++;
                }
                return dest;
            } else {
                Collection<Object> src = (Collection<Object>) pojo;
                int len = src.size();
                Collection<Object> dest = createCollection(type, len);
                history.put(pojo, dest);
                for (Object obj : src) {
                    Type keyType = getGenericClassByIndex(genericType, 0);
                    Class<?> keyClazz = obj == null ? null : obj.getClass();
                    if (keyType instanceof Class) {
                        keyClazz = (Class<?>) keyType;
                    }
                    Object value = realize1(obj, keyClazz, keyType, mapGeneric, history);
                    dest.add(value);
                }
                return dest;
            }
        }

        if (pojo instanceof Map<?, ?> && type != null) {
            Object className = ((Map<Object, Object>) pojo).get("class");
            if (className instanceof String) {
                if (!CLASS_NOT_FOUND_CACHE.containsKey(className)) {
                    //TODO 这里先注释，暂时不支持这种
                   /* try {
                        type = DefaultSerializeClassChecker.getInstance().loadClass(ClassUtils.getClassLoader(), (String) className);
                    } catch (ClassNotFoundException e) {
                        CLASS_NOT_FOUND_CACHE.put((String) className, NOT_FOUND_VALUE);
                    }*/
                }
            }

            // special logic for enum
            if (type.isEnum()) {
                Object name = ((Map<Object, Object>) pojo).get("name");
                if (name != null) {
                    if (!(name instanceof String)) {
                        throw new IllegalArgumentException("`name` filed should be string!");
                    } else {
                        return Enum.valueOf((Class<Enum>) type, (String) name);
                    }
                }
            }
            Map<Object, Object> map;
            // when return type is not the subclass of return type from the signature and not an interface
            if (!type.isInterface() && !type.isAssignableFrom(pojo.getClass())) {
                try {
                    map = (Map<Object, Object>) type.getDeclaredConstructor().newInstance();
                    Map<Object, Object> mapPojo = (Map<Object, Object>) pojo;
                    map.putAll(mapPojo);
                    //if (GENERIC_WITH_CLZ) {
                        map.remove("class");
                    //}
                } catch (Exception e) {
                    //ignore error
                    map = (Map<Object, Object>) pojo;
                }
            } else {
                map = (Map<Object, Object>) pojo;
            }

            if (Map.class.isAssignableFrom(type) || type == Object.class) {
                final Map<Object, Object> result;
                // fix issue#5939
                Type mapKeyType = getKeyTypeForMap(map.getClass());
                Type typeKeyType = getGenericClassByIndex(genericType, 0);
                boolean typeMismatch = mapKeyType instanceof Class
                        && typeKeyType instanceof Class
                        && !typeKeyType.getTypeName().equals(mapKeyType.getTypeName());
                if (typeMismatch) {
                    result = createMap(new HashMap(0));
                } else {
                    result = createMap(map);
                }

                history.put(pojo, result);
                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    Type keyType = getGenericClassByIndex(genericType, 0);
                    Type valueType = getGenericClassByIndex(genericType, 1);
                    Class<?> keyClazz;
                    if (keyType instanceof Class) {
                        keyClazz = (Class<?>) keyType;
                    } else if (keyType instanceof ParameterizedType) {
                        keyClazz = (Class<?>) ((ParameterizedType) keyType).getRawType();
                    } else {
                        keyClazz = entry.getKey() == null ? null : entry.getKey().getClass();
                    }
                    Class<?> valueClazz;
                    if (valueType instanceof Class) {
                        valueClazz = (Class<?>) valueType;
                    } else if (valueType instanceof ParameterizedType) {
                        valueClazz = (Class<?>) ((ParameterizedType) valueType).getRawType();
                    } else {
                        valueClazz = entry.getValue() == null ? null : entry.getValue().getClass();
                    }

                    Object key = keyClazz == null ? entry.getKey() : realize1(entry.getKey(), keyClazz, keyType, mapGeneric, history);
                    Object value = valueClazz == null ? entry.getValue() : realize1(entry.getValue(), valueClazz, valueType, mapGeneric, history);
                    result.put(key, value);
                }
                return result;
            } else if (type.isInterface()) {
                Object dest = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{type}, new PojoInvocationHandler(map));
                history.put(pojo, dest);
                return dest;
            } else {
                Object dest;
                if (Throwable.class.isAssignableFrom(type)) {
                    Object message = map.get("message");
                    if (message instanceof String) {
                        dest = newThrowableInstance(type, (String) message);
                    } else {
                        dest = newInstance(type);
                    }
                } else {
                    dest = newInstance(type);
                }

                history.put(pojo, dest);

                for (Map.Entry<Object, Object> entry : map.entrySet()) {
                    Object key = entry.getKey();
                    if (key instanceof String) {
                        String name = (String) key;
                        Object value = entry.getValue();
                        if (value != null) {
                            Method method = getSetterMethod(dest.getClass(), name, value.getClass());
                            Field field = getAndCacheField(dest.getClass(), name);
                            if (method != null) {
                                if (!method.isAccessible()) {
                                    method.setAccessible(true);
                                }
                                Type containType = Optional.ofNullable(field)
                                        .map(Field::getGenericType)
                                        .map(Type::getTypeName)
                                        .map(mapGeneric::get)
                                        .orElse(null);
                                if (containType != null) {
                                    //is generic
                                    if (containType instanceof ParameterizedType) {
                                        value = realize1(value, (Class<?>) ((ParameterizedType) containType).getRawType(), containType, mapGeneric, history);
                                    } else if (containType instanceof Class) {
                                        value = realize1(value, (Class<?>) containType, containType, mapGeneric, history);
                                    } else {
                                        Type ptype = method.getGenericParameterTypes()[0];
                                        value = realize1(value, method.getParameterTypes()[0], ptype, mapGeneric, history);
                                    }
                                } else {
                                    Type ptype = method.getGenericParameterTypes()[0];
                                    value = realize1(value, method.getParameterTypes()[0], ptype, mapGeneric, history);
                                }
                                try {
                                    method.invoke(dest, value);
                                } catch (Exception e) {
                                    String exceptionDescription = "Failed to set pojo " + dest.getClass().getSimpleName() + " property " + name
                                            + " value " + value.getClass() + ", cause: " + e.getMessage();

                                    throw new RuntimeException(exceptionDescription, e);
                                }
                            } else if (field != null) {
                                value = realize1(value, field.getType(), field.getGenericType(), mapGeneric, history);
                                try {
                                    field.set(dest, value);
                                } catch (IllegalAccessException e) {
                                    throw new RuntimeException("Failed to set field " + name + " of pojo " + dest.getClass().getName() + " : " + e.getMessage(), e);
                                }
                            }
                        }
                    }
                }
                return dest;
            }
        }
        return pojo;
    }
    private static Collection<Object> createCollection(Class<?> type, int len) {
        if (type.isAssignableFrom(ArrayList.class)) {
            return new ArrayList<>(len);
        }
        if (type.isAssignableFrom(HashSet.class)) {
            return new HashSet<>(len);
        }
        if (!type.isInterface() && !Modifier.isAbstract(type.getModifiers())) {
            try {
                return (Collection<Object>) type.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                // ignore
            }
        }
        return new ArrayList<>();
    }

    private static Type getGenericClassByIndex(Type genericType, int index) {
        Type clazz = null;
        // find parameterized type
        if (genericType instanceof ParameterizedType) {
            ParameterizedType t = (ParameterizedType) genericType;
            Type[] types = t.getActualTypeArguments();
            clazz = types[index];
        }
        return clazz;
    }

    private static Type getKeyTypeForMap(Class<?> clazz) {
        Type[] interfaces = clazz.getGenericInterfaces();
        if (!ArrayUtils.isEmpty(interfaces)) {
            for (Type type : interfaces) {
                if (type instanceof ParameterizedType) {
                    ParameterizedType t = (ParameterizedType) type;
                    if ("java.util.Map".equals(t.getRawType().getTypeName())) {
                        return t.getActualTypeArguments()[0];
                    }
                }
            }
        }
        return null;
    }

    private static Method getSetterMethod(Class<?> cls, String property, Class<?> valueCls) {
        String name = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
        Method method = NAME_METHODS_CACHE.get(cls.getName() + "." + name + "(" + valueCls.getName() + ")");
        if (method == null) {
            try {
                method = cls.getMethod(name, valueCls);
            } catch (NoSuchMethodException e) {
                for (Method m : cls.getMethods()) {
                    if (ReflectUtil.isBeanPropertyWriteMethod(m) && m.getName().equals(name)) {
                        method = m;
                        break;
                    }
                }
            }
            if (method != null) {
                NAME_METHODS_CACHE.put(cls.getName() + "." + name + "(" + valueCls.getName() + ")", method);
            }
        }
        return method;
    }

    private static Object newInstance(Class<?> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception t) {
            Constructor<?>[] constructors = cls.getDeclaredConstructors();
            /*
              From Javadoc java.lang.Class#getDeclaredConstructors
              This method returns an array of Constructor objects reflecting all the constructors
              declared by the class represented by this Class object.
              This method returns an array of length 0,
              if this Class object represents an interface, a primitive type, an array class, or void.
             */
            if (constructors.length == 0) {
                throw new RuntimeException("Illegal constructor: " + cls.getName());
            }
            Throwable lastError = null;
            Arrays.sort(constructors, Comparator.comparingInt(a -> a.getParameterTypes().length));
            for (Constructor<?> constructor : constructors) {
                try {
                    constructor.setAccessible(true);
                    Object[] parameters = Arrays.stream(constructor.getParameterTypes()).map(PojoUtils::getDefaultValue).toArray();
                    return constructor.newInstance(parameters);
                } catch (Exception e) {
                    lastError = e;
                }
            }
            throw new RuntimeException(lastError.getMessage(), lastError);
        }
    }
    private static Object getDefaultValue(Class<?> parameterType) {
        if ("char".equals(parameterType.getName())) {
            return Character.MIN_VALUE;
        }
        if ("boolean".equals(parameterType.getName())) {
            return false;
        }
        if ("byte".equals(parameterType.getName())) {
            return (byte) 0;
        }
        if ("short".equals(parameterType.getName())) {
            return (short) 0;
        }
        return parameterType.isPrimitive() ? 0 : null;
    }

    private static Map createMap(Map src) {
        Class<? extends Map> cl = src.getClass();
        Map result = null;
        if (HashMap.class == cl) {
            result = new HashMap();
        } else if (Hashtable.class == cl) {
            result = new Hashtable();
        } else if (IdentityHashMap.class == cl) {
            result = new IdentityHashMap();
        } else if (LinkedHashMap.class == cl) {
            result = new LinkedHashMap();
        } else if (Properties.class == cl) {
            result = new Properties();
        } else if (TreeMap.class == cl) {
            result = new TreeMap();
        } else if (WeakHashMap.class == cl) {
            return new WeakHashMap();
        } else if (ConcurrentHashMap.class == cl) {
            result = new ConcurrentHashMap();
        } else if (ConcurrentSkipListMap.class == cl) {
            result = new ConcurrentSkipListMap();
        } else {
            try {
                result = cl.getDeclaredConstructor().newInstance();
            } catch (Exception e) { /* ignore */ }

            if (result == null) {
                try {
                    Constructor<?> constructor = cl.getConstructor(Map.class);
                    result = (Map) constructor.newInstance(Collections.EMPTY_MAP);
                } catch (Exception e) { /* ignore */ }
            }
        }

        if (result == null) {
            result = new HashMap<>();
        }

        return result;
    }

    private static Object newThrowableInstance(Class<?> cls, String message) {
        try {
            Constructor<?> messagedConstructor = cls.getDeclaredConstructor(String.class);
            return messagedConstructor.newInstance(message);
        } catch (Exception t) {
            return newInstance(cls);
        }
    }

    private static Field getAndCacheField(Class<?> cls, String fieldName) {
        Field result;
        if (CLASS_FIELD_CACHE.containsKey(cls) && CLASS_FIELD_CACHE.get(cls).containsKey(fieldName)) {
            return CLASS_FIELD_CACHE.get(cls).get(fieldName);
        }

        result = getField(cls, fieldName);

        if (result != null) {
            ConcurrentMap<String, Field> fields = CLASS_FIELD_CACHE.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());
            fields.putIfAbsent(fieldName, result);
        }
        return result;
    }

    private static Field getField(Class<?> cls, String fieldName) {
        Field result = null;
        for (Class<?> acls = cls; acls != null; acls = acls.getSuperclass()) {
            try {
                result = acls.getDeclaredField(fieldName);
                if (!Modifier.isPublic(result.getModifiers())) {
                    result.setAccessible(true);
                }
            } catch (NoSuchFieldException e) {
            }
        }
        if (result == null && cls != null) {
            for (Field field : cls.getFields()) {
                if (fieldName.equals(field.getName()) && ReflectUtil.isPublicInstanceField(field)) {
                    result = field;
                    break;
                }
            }
        }
        return result;
    }

    private static class PojoInvocationHandler implements InvocationHandler {

        private final Map<Object, Object> map;

        public PojoInvocationHandler(Map<Object, Object> map) {
            this.map = map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(map, args);
            }
            String methodName = method.getName();
            Object value = null;
            if (methodName.length() > 3 && methodName.startsWith("get")) {
                value = map.get(methodName.substring(3, 4).toLowerCase() + methodName.substring(4));
            } else if (methodName.length() > 2 && methodName.startsWith("is")) {
                value = map.get(methodName.substring(2, 3).toLowerCase() + methodName.substring(3));
            } else {
                value = map.get(methodName.substring(0, 1).toLowerCase() + methodName.substring(1));
            }
            if (value instanceof Map<?, ?> && !Map.class.isAssignableFrom(method.getReturnType())) {
                value = realize0(value, method.getReturnType(), null, new IdentityHashMap<>());
            }
            return value;
        }
    }
}
