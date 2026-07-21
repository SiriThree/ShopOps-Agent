package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import com.sirithree.shopops.admin.connector.service.ConnectorCredentialService;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InMemoryConnectorCredentialService implements ConnectorCredentialService {
    private static final List<String> KNOWN_CONNECTORS = List.of(
            "file.order-summary",
            "file.negative-comments",
            "file.product-candidates"
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
        return dto;
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
                                    Long updatedBy,
                                    String updatedAt) {
    }
}
