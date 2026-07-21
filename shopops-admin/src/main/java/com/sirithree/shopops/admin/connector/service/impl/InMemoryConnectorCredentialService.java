package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import com.sirithree.shopops.admin.connector.service.ConnectorCredentialService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryConnectorCredentialService implements ConnectorCredentialService {
    private static final List<String> KNOWN_CONNECTORS = List.of(
            "file.order-summary",
            "file.negative-comments",
            "file.product-candidates",
            "file.ad-performance"
    );

    private final Map<String, CredentialRecord> credentials = new ConcurrentHashMap<>();

    @Override
    public List<ConnectorCredentialDto> list(Long tenantId, Long shopId) {
        return KNOWN_CONNECTORS.stream()
                .map(connectorCode -> toDto(credentials.get(key(tenantId, shopId, connectorCode)), connectorCode))
                .sorted(Comparator.comparing(ConnectorCredentialDto::getConnectorCode))
                .toList();
    }

    @Override
    public ConnectorCredentialDto save(Long tenantId, Long shopId, Long userId, ConnectorCredentialParam param) {
        String connectorCode = normalize(param.getConnectorCode());
        requireKnownConnector(connectorCode);
        CredentialRecord record = new CredentialRecord(
                connectorCode,
                normalizeType(param.getCredentialType()),
                param.getSecretValue().trim(),
                true,
                parseExpiresAt(param.getExpiresAt()),
                userId,
                LocalDateTime.now().toString()
        );
        credentials.put(key(tenantId, shopId, connectorCode), record);
        return toDto(record, connectorCode);
    }

    @Override
    public ConnectorCredentialDto disable(Long tenantId, Long shopId, String connectorCode) {
        String normalized = normalize(connectorCode);
        requireKnownConnector(normalized);
        CredentialRecord current = credentials.get(key(tenantId, shopId, normalized));
        if (current == null) {
            return emptyDto(normalized);
        }
        CredentialRecord disabled = new CredentialRecord(
                current.connectorCode(),
                current.credentialType(),
                current.secretValue(),
                false,
                current.expiresAt(),
                current.updatedBy(),
                LocalDateTime.now().toString()
        );
        credentials.put(key(tenantId, shopId, normalized), disabled);
        return toDto(disabled, normalized);
    }

    @Override
    public ConnectorCredentialTestResult test(Long tenantId, Long shopId, String connectorCode) {
        String normalized = normalize(connectorCode);
        requireKnownConnector(normalized);
        CredentialRecord record = credentials.get(key(tenantId, shopId, normalized));
        ConnectorCredentialTestResult result = new ConnectorCredentialTestResult();
        result.setConnectorCode(normalized);
        result.setTestedAt(LocalDateTime.now().toString());
        if (record == null) {
            result.setSuccess(false);
            result.setStatus("NOT_CONFIGURED");
            result.setMessage("未配置凭证");
            return result;
        }
        if (!record.enabled()) {
            result.setSuccess(false);
            result.setStatus("DISABLED");
            result.setMessage("凭证已停用");
            return result;
        }
        if (record.expiresAt() != null && record.expiresAt().isBefore(LocalDateTime.now())) {
            result.setSuccess(false);
            result.setStatus("EXPIRED");
            result.setMessage("凭证已过期，请轮换后再测试");
            return result;
        }
        result.setSuccess(true);
        result.setStatus("PASS");
        result.setMessage("凭证格式可用");
        return result;
    }

    private ConnectorCredentialDto toDto(CredentialRecord record, String connectorCode) {
        if (record == null) {
            return emptyDto(connectorCode);
        }
        ConnectorCredentialDto dto = new ConnectorCredentialDto();
        dto.setConnectorCode(record.connectorCode());
        dto.setCredentialType(record.credentialType());
        dto.setMaskedSecret(mask(record.secretValue()));
        dto.setConfigured(true);
        dto.setEnabled(record.enabled());
        dto.setStatus(record.enabled() ? "ENABLED" : "DISABLED");
        dto.setExpiresAt(record.expiresAt() == null ? null : record.expiresAt().toString());
        applyRotation(dto, record.expiresAt());
        dto.setUpdatedBy(record.updatedBy());
        dto.setUpdatedAt(record.updatedAt());
        return dto;
    }

    private ConnectorCredentialDto emptyDto(String connectorCode) {
        ConnectorCredentialDto dto = new ConnectorCredentialDto();
        dto.setConnectorCode(connectorCode);
        dto.setCredentialType("API_KEY");
        dto.setMaskedSecret("");
        dto.setConfigured(false);
        dto.setEnabled(false);
        dto.setStatus("NOT_CONFIGURED");
        dto.setRotationStatus("NOT_CONFIGURED");
        dto.setRotationMessage("未配置凭证");
        return dto;
    }

    private void applyRotation(ConnectorCredentialDto dto, LocalDateTime expiresAt) {
        if (!dto.isEnabled()) {
            dto.setRotationStatus("DISABLED");
            dto.setRotationMessage("凭证已停用");
            return;
        }
        if (expiresAt == null) {
            dto.setRotationStatus("NO_EXPIRY");
            dto.setRotationMessage("未设置过期时间");
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        long days = Duration.between(now, expiresAt).toDays();
        dto.setDaysUntilExpiry(days);
        if (expiresAt.isBefore(now)) {
            dto.setRotationStatus("EXPIRED");
            dto.setRotationMessage("凭证已过期，请立即轮换");
        } else if (days <= 14) {
            dto.setRotationStatus("EXPIRING_SOON");
            dto.setRotationMessage("凭证将在" + Math.max(days, 0) + "天内过期");
        } else {
            dto.setRotationStatus("OK");
            dto.setRotationMessage("凭证有效");
        }
    }

    private LocalDateTime parseExpiresAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 10) {
            return LocalDate.parse(trimmed).atTime(23, 59, 59);
        }
        return LocalDateTime.parse(trimmed);
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "****";
        }
        return trimmed.substring(0, 3) + "****" + trimmed.substring(trimmed.length() - 3);
    }

    private void requireKnownConnector(String connectorCode) {
        if (!KNOWN_CONNECTORS.contains(connectorCode)) {
            throw new IllegalArgumentException("连接器不存在: " + connectorCode);
        }
    }

    private String key(Long tenantId, Long shopId, String connectorCode) {
        return tenantId + ":" + shopId + ":" + connectorCode;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeType(String value) {
        return value == null || value.isBlank() ? "API_KEY" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record CredentialRecord(String connectorCode,
                                    String credentialType,
                                    String secretValue,
                                    boolean enabled,
                                    LocalDateTime expiresAt,
                                    Long updatedBy,
                                    String updatedAt) {
    }
}
