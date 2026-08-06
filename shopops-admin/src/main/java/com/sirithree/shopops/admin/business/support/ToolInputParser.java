package com.sirithree.shopops.admin.business.support;

import java.time.LocalDate;
import java.util.Map;

public final class ToolInputParser {
    private ToolInputParser() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asMap(Object input) {
        if (!(input instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("工具输入必须是 JSON 对象");
        }
        return (Map<String, Object>) map;
    }

    public static Long longValue(Map<String, Object> input, String key, Long defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static Integer intValue(Map<String, Object> input, String key, Integer defaultValue) {
        Object value = input.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    public static LocalDate dateValue(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null) {
            throw new IllegalArgumentException("缺少日期参数: " + key);
        }
        return LocalDate.parse(String.valueOf(value));
    }
}
