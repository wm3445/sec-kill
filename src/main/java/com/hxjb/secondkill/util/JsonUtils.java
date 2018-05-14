package com.hxjb.secondkill.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created by xpcomrade on 2016/9/12.
 * Copyright (c) 2016, xpcomrade@gmail.com All Rights Reserved.
 * Description: (json工具). <br/>
 */
public class JsonUtils {

    public static String JSON_ERROR = "parse Json to Java Object error！";

    private static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();

        //设置为中国上海时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));

        //序列化时设置统一的日期格式
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        //反序列化时，属性不存在的兼容处理
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //objectMapper.getDeserializationConfig().withoutFeatures(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 将 Java 对象转为 JSON 字符串
     */
    public static <T> String toJSON(T obj) {
        String jsonStr = null;
        try {
            jsonStr = objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            logger.error(JSON_ERROR, e);
        }
        return jsonStr;
    }

    /**
     * 将 JSON 字符串转为 Java 对象
     */
    public static <T> T fromJSON(String json, Class<T> type) {
        if(null == json || "".equals(json)) return null;
        T obj = null;
        try {
            obj = objectMapper.readValue(json, type);
        } catch (Exception e) {
            logger.error(JSON_ERROR, e);
        }
        return obj;
    }


}
