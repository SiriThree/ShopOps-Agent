package com.sirithree.shopops.admin.organization.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface ShopRuntimeConfigService {
    Optional<String> value(Long tenantId, Long shopId, String configKey);

    default boolean booleanValue(Long tenantId, Long shopId, String configKey, boolean defaultValue) {
        return value(tenantId, shopId, configKey)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    default BigDecimal decimalValue(Long tenantId, Long shopId, String configKey, BigDecimal defaultValue) {
        return value(tenantId, shopId, configKey)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(BigDecimal::new)
                .orElse(defaultValue);
    }
}
