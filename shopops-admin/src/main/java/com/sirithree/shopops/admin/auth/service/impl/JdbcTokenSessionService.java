package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.TokenPrincipal;
import com.sirithree.shopops.admin.auth.domain.TokenSessionCreateCommand;
import com.sirithree.shopops.admin.auth.service.TokenSessionService;
import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.AuthTokenSessionMapper;
import com.sirithree.shopops.admin.persistence.model.AuthTokenSession;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcTokenSessionService implements TokenSessionService {
    private final AuthTokenSessionMapper authTokenSessionMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcTokenSessionService(AuthTokenSessionMapper authTokenSessionMapper,
                                   JacksonJsonSupport jsonSupport) {
        this.authTokenSessionMapper = authTokenSessionMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public void createSession(TokenSessionCreateCommand command) {
        LocalDateTime now = LocalDateTime.now();
        AuthTokenSession session = new AuthTokenSession();
        session.setTokenId(command.getTokenId());
        session.setTenantId(command.getTenantId());
        session.setShopId(command.getShopId());
        session.setUserId(command.getUserId());
        session.setUsername(command.getUsername());
        session.setRolesJson(jsonSupport.toJson(command.getRoles()));
        session.setStatus("ACTIVE");
        session.setIssuedAt(toLocalDateTime(command.getIssuedAt()));
        session.setExpiresAt(toLocalDateTime(command.getExpiresAt()));
        session.setLastSeenAt(now);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        authTokenSessionMapper.insert(session);
    }

    @Override
    public boolean validateAndTouch(TokenPrincipal principal) {
        AuthTokenSession session = authTokenSessionMapper.selectByTokenId(principal.getTokenId());
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            return false;
        }
        if (!session.getTenantId().equals(principal.getTenantId())
                || !session.getShopId().equals(principal.getShopId())
                || !session.getUserId().equals(principal.getUserId())) {
            return false;
        }
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(LocalDateTime.now())) {
            return false;
        }
        authTokenSessionMapper.touch(principal.getTokenId(), LocalDateTime.now());
        return true;
    }

    @Override
    public boolean revoke(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        return authTokenSessionMapper.revoke(tokenId, LocalDateTime.now()) > 0;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }
}
