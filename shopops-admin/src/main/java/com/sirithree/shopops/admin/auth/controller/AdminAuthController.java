package com.sirithree.shopops.admin.auth.controller;

import com.sirithree.shopops.admin.auth.domain.CurrentUserDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventCreateCommand;
import com.sirithree.shopops.admin.auth.domain.LoginParam;
import com.sirithree.shopops.admin.auth.domain.LoginResult;
import com.sirithree.shopops.admin.auth.domain.LogoutResult;
import com.sirithree.shopops.admin.auth.exception.AuthenticationException;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.auth.service.AuthService;
import com.sirithree.shopops.admin.auth.service.TokenSessionService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AuthService authService;
    private final TokenSessionService tokenSessionService;
    private final AuthAuditService authAuditService;

    public AdminAuthController(AuthService authService,
                               TokenSessionService tokenSessionService,
                               AuthAuditService authAuditService) {
        this.authService = authService;
        this.tokenSessionService = tokenSessionService;
        this.authAuditService = authAuditService;
    }

    @PostMapping("/login")
    public CommonResult<LoginResult> login(@Valid @RequestBody LoginParam param) {
        return CommonResult.success(authService.login(param));
    }

    @GetMapping("/me")
    public CommonResult<CurrentUserDto> me() {
        return CommonResult.success(CurrentUserDto.from(RequestContextHolder.current()));
    }

    @PostMapping("/logout")
    public CommonResult<LogoutResult> logout() {
        RequestContext context = RequestContextHolder.current();
        if (context.getTokenId() == null || context.getTokenId().isBlank()) {
            throw new AuthenticationException("Bearer token is required");
        }
        tokenSessionService.revoke(context.getTokenId());
        recordLogout(context);
        return CommonResult.success(new LogoutResult(context.getTokenId(), "REVOKED"));
    }

    private void recordLogout(RequestContext context) {
        AuthAuditEventCreateCommand command = new AuthAuditEventCreateCommand();
        command.setTenantId(context.getTenantId());
        command.setShopId(context.getShopId());
        command.setUserId(context.getUserId());
        command.setUsername(context.getUsername());
        command.setEventType("LOGOUT");
        command.setEventStatus("SUCCESS");
        command.setAuthType(context.getAuthType());
        command.setRequestId(context.getRequestId());
        authAuditService.record(command);
    }
}
