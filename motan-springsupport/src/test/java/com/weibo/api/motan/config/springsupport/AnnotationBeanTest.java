package com.weibo.api.motan.config.springsupport;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class AnnotationBeanTest {

    @Test
    public void testGetAllClassFields() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = AnnotationBean.class.getDeclaredMethod("getAllClassFields", Class.class);
        method.setAccessible(true);
        Field[] fields = (Field[]) method.invoke(new AnnotationBean(), Child.class);
        Map<Class<?>, String> fieldMap = new HashMap<>();
        for (Field f : fields) {
            fieldMap.put(f.getType(), f.getName());
        }
        Map<Class<?>, String> expectMap = new HashMap<>();
        expectMap.put(byte.class, "f1");
        expectMap.put(short.class, "f2");
        expectMap.put(int.class, "f3");
        expectMap.put(long.class, "f4");
        expectMap.put(float.class, "f5");
        expectMap.put(double.class, "f6");
        expectMap.put(boolean.class, "f7");
        expectMap.put(char.class, "f8");
        assertTrue(fieldMap.equals(expectMap));
    }

    private static class Child extends Parent {
        private byte f1;
        private short f2;
        private int f3;
        private long f4;
        private float f5;
    }

    private static class Parent {
        private double f6;
        private boolean f7;
        private char f8;
    }
}
