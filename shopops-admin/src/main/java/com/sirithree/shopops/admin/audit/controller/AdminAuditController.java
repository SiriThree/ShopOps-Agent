package com.sirithree.shopops.admin.audit.controller;

import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineDetailDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineEventDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditTimelineQueryParam;
import com.sirithree.shopops.admin.audit.service.AdminAuditService;
import com.sirithree.shopops.admin.auth.annotation.RequireRole;
import com.sirithree.shopops.admin.auth.domain.AuthRole;
import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@RequireRole(AuthRole.ADMIN)
public class AdminAuditController {
    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/overview")
    public CommonResult<AdminAuditOverviewDto> getOverview() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(adminAuditService.getOverview(context.getTenantId(), context.getShopId()));
    }

    @GetMapping("/timeline")
    public CommonResult<CommonPage<AdminAuditTimelineEventDto>> listTimeline(AdminAuditTimelineQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(adminAuditService.listTimeline(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/timeline/{source}/{resourceId}")
    public CommonResult<AdminAuditTimelineDetailDto> getTimelineDetail(@PathVariable String source,
                                                                       @PathVariable String resourceId) {
        RequestContext context = RequestContextHolder.current();
        return adminAuditService.getTimelineDetail(context.getTenantId(), context.getShopId(), source, resourceId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("Audit event not found"));
    }
}
