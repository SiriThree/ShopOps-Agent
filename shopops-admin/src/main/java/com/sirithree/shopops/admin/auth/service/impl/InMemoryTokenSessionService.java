package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.TokenPrincipal;
import com.sirithree.shopops.admin.auth.domain.TokenSessionCreateCommand;
import com.sirithree.shopops.admin.auth.service.TokenSessionService;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenSessionService implements TokenSessionService {
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();

    @Override
    public void createSession(TokenSessionCreateCommand command) {
        sessions.put(command.getTokenId(), new SessionState(
                command.getTenantId(),
                command.getShopId(),
                command.getUserId(),
                command.getExpiresAt(),
                "ACTIVE"
        ));
    }

    @Override
    public boolean validateAndTouch(TokenPrincipal principal) {
        SessionState session = sessions.get(principal.getTokenId());
        if (session == null || !"ACTIVE".equals(session.status())) {
            return false;
        }
        if (!session.tenantId().equals(principal.getTenantId())
                || !session.shopId().equals(principal.getShopId())
                || !session.userId().equals(principal.getUserId())) {
            return false;
        }
        return session.expiresAt() != null && session.expiresAt().isAfter(Instant.now());
    }

    @Override
    public boolean revoke(String tokenId) {
        SessionState session = sessions.get(tokenId);
        if (session == null || !"ACTIVE".equals(session.status())) {
            return false;
        }
        return sessions.replace(tokenId, session, session.revoked());
    }

    private record SessionState(Long tenantId, Long shopId, Long userId, Instant expiresAt, String status) {
        SessionState revoked() {
            return new SessionState(tenantId, shopId, userId, expiresAt, "REVOKED");
        }
    }
}
