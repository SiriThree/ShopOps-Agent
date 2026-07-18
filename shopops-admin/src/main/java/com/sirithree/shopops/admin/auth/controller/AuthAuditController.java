package com.sirithree.shopops.admin.auth.controller;

import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventDto;
import com.sirithree.shopops.admin.auth.domain.AuthAuditEventQueryParam;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.auth.service.AuthAuditService;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth/audit-events")
@RequireRole(AuthRole.ADMIN)
public class AuthAuditController {
    private final AuthAuditService authAuditService;

    public AuthAuditController(AuthAuditService authAuditService) {
        this.authAuditService = authAuditService;
    }

    @GetMapping
    public CommonResult<CommonPage<AuthAuditEventDto>> listEvents(AuthAuditEventQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(authAuditService.listEvents(context.getTenantId(), context.getShopId(), param));
    }
}
