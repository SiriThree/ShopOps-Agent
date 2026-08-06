package com.sirithree.shopops.admin.organization.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

final class ShopConfigValueValidator {
    private static final List<String> MODEL_POLICIES = List.of("conservative", "balanced", "aggressive", "default");

    private ShopConfigValueValidator() {
    }

    static String normalizeConfigKey(String configKey) {
        String value = configKey == null ? "" : configKey.trim();
        if (!List.of(
                "refund_rate_warn_threshold",
                "negative_comment_warn_threshold",
                "agent_tool_approval_enabled",
                "agent_model_policy"
        ).contains(value)) {
            throw new IllegalArgumentException("不支持的店铺配置项: " + configKey);
        }
        return value;
    }

    static String normalizeValueType(String configKey, String valueType) {
        String normalized = valueType == null ? "" : valueType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("string", "number", "boolean", "json").contains(normalized)) {
            throw new IllegalArgumentException("不支持的配置类型: " + valueType);
        }
        return switch (configKey) {
            case "refund_rate_warn_threshold", "negative_comment_warn_threshold" -> requiredType(configKey, normalized, "number");
            case "agent_tool_approval_enabled" -> requiredType(configKey, normalized, "boolean");
            case "agent_model_policy" -> requiredType(configKey, normalized, "string");
            default -> normalized;
        };
    }

    static String normalizeConfigValue(String configKey, String configValue) {
        String value = configValue == null ? "" : configValue.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("配置值不能为空");
        }
        return switch (configKey) {
            case "refund_rate_warn_threshold" -> normalizeRefundRateThreshold(value);
            case "negative_comment_warn_threshold" -> normalizeNegativeCommentThreshold(value);
            case "agent_tool_approval_enabled" -> normalizeBoolean(value);
            case "agent_model_policy" -> normalizeModelPolicy(value);
            default -> value;
        };
    }

    private static String requiredType(String configKey, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException("配置项 " + configKey + " 仅支持 " + expected + " 类型");
        }
        return actual;
    }

    private static String normalizeRefundRateThreshold(String value) {
        try {
            BigDecimal threshold = new BigDecimal(value);
            if (threshold.compareTo(BigDecimal.ZERO) < 0 || threshold.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("退款率预警阈值必须在 0 到 1 之间");
            }
            return threshold.stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("退款率预警阈值必须是数字");
        }
    }

    private static String normalizeNegativeCommentThreshold(String value) {
        try {
            int threshold = Integer.parseInt(value);
            if (threshold < 0 || threshold > 9999) {
                throw new IllegalArgumentException("差评预警阈值必须在 0 到 9999 之间");
            }
            return String.valueOf(threshold);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("差评预警阈值必须是整数");
        }
    }

    private static String normalizeBoolean(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!List.of("true", "false").contains(normalized)) {
            throw new IllegalArgumentException("高风险工具审批配置必须是 true 或 false");
        }
        return normalized;
    }

    private static String normalizeModelPolicy(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!MODEL_POLICIES.contains(normalized)) {
            throw new IllegalArgumentException("模型策略仅支持 conservative、balanced、aggressive、default");
        }
        return normalized;
    }
}
