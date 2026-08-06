package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.domain.LoginUserRecord;
import com.sirithree.shopops.admin.auth.domain.TokenSessionCreateCommand;
import com.sirithree.shopops.admin.auth.domain.UserRoleProfile;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.auth.service.PasswordHashService;
import com.sirithree.shopops.admin.auth.service.TokenSessionService;
import com.sirithree.shopops.admin.auth.service.TokenService;
import com.sirithree.shopops.admin.auth.service.UserRoleService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.persistence.mapper.AuthUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcAuthService implements AuthService {
    private final AuthUserMapper authUserMapper;
    private final UserRoleService userRoleService;
    private final PasswordHashService passwordHashService;
    private final TokenService tokenService;
    private final AuthAuditService authAuditService;
    private final TokenSessionService tokenSessionService;

    public JdbcAuthService(AuthUserMapper authUserMapper,
                           UserRoleService userRoleService,
                           PasswordHashService passwordHashService,
                           TokenService tokenService,
                           AuthAuditService authAuditService,
                           TokenSessionService tokenSessionService) {
        this.authUserMapper = authUserMapper;
        this.userRoleService = userRoleService;
        this.passwordHashService = passwordHashService;
        this.tokenService = tokenService;
        this.authAuditService = authAuditService;
        this.tokenSessionService = tokenSessionService;
    }

    @Override
    public LoginResult login(LoginParam param) {
        LoginUserRecord user = authUserMapper.selectLoginUserByUsername(param.getUsername());
        if (user == null || !passwordHashService.matches(param.getPassword(), user.getPasswordHash())) {
            recordLogin(param, null, param.getUsername(), "FAILURE", "Invalid username or password");
            throw new IllegalArgumentException("Invalid username or password");
        }
        UserRoleProfile profile = userRoleService
                .getUserRoleProfile(param.getTenantId(), param.getShopId(), user.getUserId())
                .filter(candidate -> !candidate.getRoles().isEmpty())
                .orElseThrow(() -> {
                    recordLogin(param, user.getUserId(), user.getUsername(), "FAILURE", "User has no active shop role");
                    return new IllegalArgumentException("User has no active shop role");
                });
        TokenService.IssuedToken token = tokenService.issue(
                param.getTenantId(),
                param.getShopId(),
                user.getUserId(),
                profile.getUsername(),
                profile.getRoles()
        );
        createTokenSession(token, param, user.getUserId(), profile.getUsername(), profile.getRoles());
        recordLogin(param, user.getUserId(), profile.getUsername(), "SUCCESS", null);
        return LoginResult.of(token.value(), token.expiresAt(), param.getTenantId(), param.getShopId(),
                user.getUserId(), profile.getUsername(), profile.getRoles());
    }

    private void createTokenSession(TokenService.IssuedToken token, LoginParam param, Long userId,
                                    String username, java.util.List<String> roles) {
        TokenSessionCreateCommand command = new TokenSessionCreateCommand();
        command.setTokenId(token.tokenId());
        command.setTenantId(param.getTenantId());
        command.setShopId(param.getShopId());
        command.setUserId(userId);
        command.setUsername(username);
        command.setRoles(roles);
        command.setIssuedAt(token.issuedAt());
        command.setExpiresAt(token.expiresAt());
        tokenSessionService.createSession(command);
    }

    private void recordLogin(LoginParam param, Long userId, String username, String status, String failureReason) {
        RequestContext context = currentContext();
        HttpServletRequest request = currentRequest();
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(param.getTenantId());
        command.setShopId(param.getShopId());
        command.setUserId(userId);
        command.setUsername(username);
        command.setEventType("LOGIN");
        command.setEventStatus(status);
        command.setAuthType(context == null ? "PASSWORD" : context.getAuthType());
        command.setRequestId(context == null ? null : context.getRequestId());
        if (request != null) {
            command.setClientIp(clientIp(request));
            command.setUserAgent(request.getHeader("User-Agent"));
        }
        command.setFailureReason(failureReason);
        authAuditService.record(command);
    }

    private RequestContext currentContext() {
        try {
            return RequestContextHolder.current();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    private HttpServletRequest currentRequest() {
        if (org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
