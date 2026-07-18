package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.domain.LoginUserRecord;
import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.auth.service.PasswordHashService;
import com.sirithree.shopops.admin.auth.service.TokenService;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import com.sirithree.shopops.admin.persistence.mapper.AuthUserMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAuthService implements AuthService {
    private final AuthUserMapper authUserMapper;
    private final UserRoleService userRoleService;
    private final PasswordHashService passwordHashService;
    private final TokenService tokenService;

    public JdbcAuthService(AuthUserMapper authUserMapper,
                           UserRoleService userRoleService,
                           PasswordHashService passwordHashService,
                           TokenService tokenService) {
        this.authUserMapper = authUserMapper;
        this.userRoleService = userRoleService;
        this.passwordHashService = passwordHashService;
        this.tokenService = tokenService;
    }

    @Override
    public LoginResult login(LoginParam param) {
        LoginUserRecord user = authUserMapper.selectLoginUserByUsername(param.getUsername());
        if (user == null || !passwordHashService.matches(param.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        UserRoleProfile profile = userRoleService
                .getUserRoleProfile(param.getTenantId(), param.getShopId(), user.getUserId())
                .filter(candidate -> !candidate.getRoles().isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("User has no active shop role"));
        TokenService.IssuedToken token = tokenService.issue(
                param.getTenantId(),
                param.getShopId(),
                user.getUserId(),
                profile.getUsername(),
                profile.getRoles()
        );
        return LoginResult.of(token.value(), token.expiresAt(), param.getTenantId(), param.getShopId(),
                user.getUserId(), profile.getUsername(), profile.getRoles());
    }
}
