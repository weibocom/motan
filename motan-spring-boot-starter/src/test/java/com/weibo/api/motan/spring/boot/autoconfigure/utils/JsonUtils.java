package com.weibo.api.motan.spring.boot.autoconfigure.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private JsonUtils() {

    }

    public static <T> T toObject(String json, Class<T> clazz) {
        if (!Strings.isNullOrEmpty(json)) {
            try {
                return MAPPER.readValue(json, clazz);
            } catch (Throwable e) {
                LOG.error("json转object异常: json=[{}], class=[{}]", json, clazz, e);
                return null;
            }
        } else {
            return null;
        }
    }


}
