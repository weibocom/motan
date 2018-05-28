package com.weibo.api.motan.config.springsupport.util;

import org.springframework.beans.factory.BeanFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fld
 * Created by fld on 16/7/18.
 */
public class SpringBeanUtil {
    public static final String COMMA_SPLIT_PATTERN = "\\s*[,]+\\s*";

    public static <T> List<T> getMultiBeans(BeanFactory beanFactory, String names, String pattern, Class<T> clazz) {
        String[] nameArr = names.split(pattern);
        List<T> beans = new ArrayList<T>();
        for (String name : nameArr) {
            if (name != null && name.length() > 0) {
                beans.add(beanFactory.getBean(name, clazz));
            }
        }
        return beans;
    }
}
