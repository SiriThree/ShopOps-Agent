package com.sirithree.shopops.admin.auth.service.impl;

import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.auth.service.PasswordHashService;
import com.sirithree.shopops.admin.auth.service.TokenService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryAuthService implements AuthService {
    private static final String DEFAULT_PASSWORD_HASH = "sha256:9b4ae7b707678ddc613ee713827367b10324f9105218e0b0a5cd098d31a83132";

    private final PasswordHashService passwordHashService;
    private final TokenService tokenService;
    private final AuthAuditService authAuditService;
    private final Map<String, DevUser> users = Map.of(
            "admin", new DevUser(1L, "admin", List.of("ADMIN")),
            "operator", new DevUser(2L, "operator", List.of("OPERATOR")),
            "viewer", new DevUser(3L, "viewer", List.of("VIEWER"))
    );

    public InMemoryAuthService(PasswordHashService passwordHashService,
                               TokenService tokenService,
                               AuthAuditService authAuditService) {
        this.passwordHashService = passwordHashService;
        this.tokenService = tokenService;
        this.authAuditService = authAuditService;
    }

    @Override
    public LoginResult login(LoginParam param) {
        DevUser user = users.get(param.getUsername());
        if (user == null || !passwordHashService.matches(param.getPassword(), DEFAULT_PASSWORD_HASH)) {
            recordLogin(param, null, param.getUsername(), "FAILURE", "Invalid username or password");
            throw new IllegalArgumentException("Invalid username or password");
        }
        TokenService.IssuedToken token = tokenService.issue(
                param.getTenantId(),
                param.getShopId(),
                user.userId(),
                user.username(),
                user.roles()
        );
        recordLogin(param, user.userId(), user.username(), "SUCCESS", null);
        return LoginResult.of(token.value(), token.expiresAt(), param.getTenantId(), param.getShopId(),
                user.userId(), user.username(), user.roles());
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

    private record DevUser(Long userId, String username, List<String> roles) {
    }
}
