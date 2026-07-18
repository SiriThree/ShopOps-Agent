package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.auth.service.PasswordHashService;
import com.sirithree.shopops.admin.auth.service.TokenService;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthService implements AuthService {
    private static final String DEFAULT_PASSWORD_HASH = "sha256:9b4ae7b707678ddc613ee713827367b10324f9105218e0b0a5cd098d31a83132";

    private final PasswordHashService passwordHashService;
    private final TokenService tokenService;
    private final Map<String, DevUser> users = Map.of(
            "admin", new DevUser(1L, "admin", List.of("ADMIN")),
            "operator", new DevUser(2L, "operator", List.of("OPERATOR")),
            "viewer", new DevUser(3L, "viewer", List.of("VIEWER"))
    );

    public InMemoryAuthService(PasswordHashService passwordHashService, TokenService tokenService) {
        this.passwordHashService = passwordHashService;
        this.tokenService = tokenService;
    }

    @Override
    public LoginResult login(LoginParam param) {
        DevUser user = users.get(param.getUsername());
        if (user == null || !passwordHashService.matches(param.getPassword(), DEFAULT_PASSWORD_HASH)) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        TokenService.IssuedToken token = tokenService.issue(
                param.getTenantId(),
                param.getShopId(),
                user.userId(),
                user.username(),
                user.roles()
        );
        return LoginResult.of(token.value(), token.expiresAt(), param.getTenantId(), param.getShopId(),
                user.userId(), user.username(), user.roles());
    }

    private record DevUser(Long userId, String username, List<String> roles) {
    }
}
