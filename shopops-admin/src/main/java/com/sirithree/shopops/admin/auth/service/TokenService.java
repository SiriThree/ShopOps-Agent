package com.sirithree.shopops.admin.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.auth.domain.TokenPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;
    private final String secret;
    private final Duration ttl;

    public TokenService(ObjectMapper objectMapper,
                        @Value("${shopops.auth.token-secret:shopops-dev-token-secret-change-me}") String secret,
                        @Value("${shopops.auth.token-ttl-seconds:7200}") long ttlSeconds) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public IssuedToken issue(Long tenantId, Long shopId, Long userId, String username, List<String> roles) {
        Instant expiresAt = Instant.now().plus(ttl);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "SHOPOPS");
        Map<String, Object> payload = Map.of(
                "tenantId", tenantId,
                "shopId", shopId,
                "userId", userId,
                "username", username,
                "roles", roles,
                "exp", expiresAt.getEpochSecond()
        );
        String headerPart = encodeJson(header);
        String payloadPart = encodeJson(payload);
        String signature = sign(headerPart + "." + payloadPart);
        return new IssuedToken(headerPart + "." + payloadPart + "." + signature, expiresAt);
    }

    public Optional<TokenPrincipal> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String expectedSignature = sign(parts[0] + "." + parts[1]);
        if (!MessageDigestSupport.constantTimeEquals(expectedSignature, parts[2])) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), new TypeReference<>() {
            });
            Instant expiresAt = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
            if (expiresAt.isBefore(Instant.now())) {
                return Optional.empty();
            }
            TokenPrincipal principal = new TokenPrincipal();
            principal.setTenantId(longValue(payload.get("tenantId")));
            principal.setShopId(longValue(payload.get("shopId")));
            principal.setUserId(longValue(payload.get("userId")));
            principal.setUsername(String.valueOf(payload.get("username")));
            principal.setRoles(stringList(payload.get("roles")));
            principal.setExpiresAt(expiresAt);
            return Optional.of(principal);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof List<?> rawList)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : rawList) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private Long longValue(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return encode(objectMapper.writeValueAsBytes(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Token JSON serialization failed", ex);
        }
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Token signing failed", ex);
        }
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    private static final class MessageDigestSupport {
        private MessageDigestSupport() {
        }

        static boolean constantTimeEquals(String left, String right) {
            return java.security.MessageDigest.isEqual(
                    left.getBytes(StandardCharsets.UTF_8),
                    right.getBytes(StandardCharsets.UTF_8)
            );
        }
    }
}
