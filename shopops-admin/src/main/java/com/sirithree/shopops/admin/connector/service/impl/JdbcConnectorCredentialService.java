package com.sirithree.shopops.admin.connector.service.impl;

import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialDto;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialParam;
import com.sirithree.shopops.admin.connector.domain.ConnectorCredentialTestResult;
import com.sirithree.shopops.admin.connector.service.ConnectorCredentialService;
import com.sirithree.shopops.admin.persistence.mapper.ConnectorCredentialMapper;
import com.sirithree.shopops.admin.persistence.model.ConnectorCredential;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcConnectorCredentialService implements ConnectorCredentialService {
    private static final List<String> KNOWN_CONNECTORS = List.of(
            "file.order-summary",
            "file.negative-comments",
            "file.product-candidates"
    );

    private final ConnectorCredentialMapper connectorCredentialMapper;
    private final ConnectorCredentialCrypto crypto;

    public JdbcConnectorCredentialService(ConnectorCredentialMapper connectorCredentialMapper,
                                          ConnectorCredentialCrypto crypto) {
        this.connectorCredentialMapper = connectorCredentialMapper;
        this.crypto = crypto;
    }

    @Override
    public List<ConnectorCredentialDto> list(Long tenantId, Long shopId) {
        Map<String, ConnectorCredential> existing = connectorCredentialMapper.list(tenantId, shopId).stream()
                .collect(Collectors.toMap(ConnectorCredential::getConnectorCode, Function.identity(), (left, right) -> left));
        return KNOWN_CONNECTORS.stream()
                .map(connectorCode -> toDto(existing.get(connectorCode), connectorCode))
                .sorted(Comparator.comparing(ConnectorCredentialDto::getConnectorCode))
                .toList();
    }

    @Override
    public ConnectorCredentialDto save(Long tenantId, Long shopId, Long userId, ConnectorCredentialParam param) {
        String connectorCode = normalize(param.getConnectorCode());
        requireKnownConnector(connectorCode);
        String secret = param.getSecretValue().trim();
        LocalDateTime now = LocalDateTime.now();
        ConnectorCredential credential = new ConnectorCredential();
        credential.setTenantId(tenantId);
        credential.setShopId(shopId);
        credential.setConnectorCode(connectorCode);
        credential.setCredentialType(normalizeType(param.getCredentialType()));
        credential.setEncryptedSecret(crypto.encrypt(secret));
        credential.setSecretPreview(mask(secret));
        credential.setStatus("ENABLED");
        credential.setExpiresAt(parseExpiresAt(param.getExpiresAt()));
        credential.setUpdatedBy(userId);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);
        connectorCredentialMapper.upsert(credential);
        return toDto(connectorCredentialMapper.find(tenantId, shopId, connectorCode), connectorCode);
    }

    @Override
    public ConnectorCredentialDto disable(Long tenantId, Long shopId, String connectorCode) {
        String normalized = normalize(connectorCode);
        requireKnownConnector(normalized);
        connectorCredentialMapper.disable(tenantId, shopId, normalized);
        return toDto(connectorCredentialMapper.find(tenantId, shopId, normalized), normalized);
    }

    @Override
    public ConnectorCredentialTestResult test(Long tenantId, Long shopId, String connectorCode) {
        String normalized = normalize(connectorCode);
        requireKnownConnector(normalized);
        ConnectorCredential credential = connectorCredentialMapper.find(tenantId, shopId, normalized);
        ConnectorCredentialTestResult result = new ConnectorCredentialTestResult();
        result.setConnectorCode(normalized);
        result.setTestedAt(LocalDateTime.now().toString());
        if (credential == null) {
            result.setSuccess(false);
            result.setStatus("NOT_CONFIGURED");
            result.setMessage("未配置凭证");
            return result;
        }
        if (!"ENABLED".equals(credential.getStatus())) {
            result.setSuccess(false);
            result.setStatus("DISABLED");
            result.setMessage("凭证已停用");
            return result;
        }
        if (credential.getExpiresAt() != null && credential.getExpiresAt().isBefore(LocalDateTime.now())) {
            result.setSuccess(false);
            result.setStatus("EXPIRED");
            result.setMessage("凭证已过期，请轮换后再测试");
            return result;
        }
        crypto.decrypt(credential.getEncryptedSecret());
        result.setSuccess(true);
        result.setStatus("PASS");
        result.setMessage("凭证可解密且格式可用");
        return result;
    }

    private ConnectorCredentialDto toDto(ConnectorCredential credential, String connectorCode) {
        if (credential == null) {
            return emptyDto(connectorCode);
        }
        ConnectorCredentialDto dto = new ConnectorCredentialDto();
        dto.setConnectorCode(credential.getConnectorCode());
        dto.setCredentialType(credential.getCredentialType());
        dto.setMaskedSecret(credential.getSecretPreview());
        dto.setConfigured(true);
        dto.setEnabled("ENABLED".equals(credential.getStatus()));
        dto.setStatus(credential.getStatus());
        dto.setExpiresAt(credential.getExpiresAt() == null ? null : credential.getExpiresAt().toString());
        applyRotation(dto, credential.getExpiresAt());
        dto.setUpdatedBy(credential.getUpdatedBy());
        dto.setUpdatedAt(credential.getUpdatedAt() == null ? null : credential.getUpdatedAt().toString());
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeType(String value) {
        return value == null || value.isBlank() ? "API_KEY" : value.trim().toUpperCase(Locale.ROOT);
    }
}
