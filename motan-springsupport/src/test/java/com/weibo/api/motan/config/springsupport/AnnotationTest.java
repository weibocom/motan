package com.weibo.api.motan.config.springsupport;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.*;

public class AnnotationTest {
    private static ApplicationContext context;

    @Before
    public void before() {
        System.setProperty("motan.refer.directUrl", "localhost:8080");
        System.setProperty("motan.service.export", "motan:8080");
        context = new ClassPathXmlApplicationContext("classpath:annotationPlaceholderTestContext.xml");
    }

    @Test
    public void test() {
        AnnotationTestHandler handler = (AnnotationTestHandler) context.getBean("testHandler");
        assertEquals("localhost:8080", handler.getReferer().directUrl());
        AnnotationTestService service = (AnnotationTestService) context.getBean("testService");
        assertEquals("motan:8080", service.getService().export());
    }

    @After
    public void after() {
        context = null;
    }
}
