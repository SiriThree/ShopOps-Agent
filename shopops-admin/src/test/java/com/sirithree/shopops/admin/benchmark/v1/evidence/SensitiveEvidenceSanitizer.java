package com.sirithree.shopops.admin.benchmark.v1.evidence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SensitiveEvidenceSanitizer {
    private static final Set<String> SENSITIVE_PARTS = Set.of(
            "password", "secret", "token", "credential", "authorization",
            "phone", "mobile", "email", "address", "idcard", "id_card"
    );

    public Object sanitize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, isSensitive(key) ? "[REDACTED]" : sanitize(entry.getValue()));
            }
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : iterable) sanitized.add(sanitize(item));
            return sanitized;
        }
        return value;
    }

    private boolean isSensitive(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_PARTS.stream().anyMatch(normalized::contains);
    }
}
