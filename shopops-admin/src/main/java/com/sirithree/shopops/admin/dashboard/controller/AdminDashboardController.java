package com.sirithree.shopops.admin.dashboard.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.dashboard.domain.AdminDashboardSummaryDto;
import com.sirithree.shopops.admin.dashboard.service.AdminDashboardService;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public CommonResult<AdminDashboardSummaryDto> getSummary() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(adminDashboardService.getSummary(context.getTenantId(), context.getShopId()));
    }
}
