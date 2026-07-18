package com.sirithree.shopops.admin.report.controller;

import com.sirithree.shopops.admin.common.context.RequestContext;
import com.sirithree.shopops.admin.common.context.RequestContextHolder;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.domain.OperationReportQueryParam;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.common.api.CommonPage;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class OperationReportController {
    private final OperationReportService operationReportService;

    public OperationReportController(OperationReportService operationReportService) {
        this.operationReportService = operationReportService;
    }

    @GetMapping
    public CommonResult<CommonPage<OperationReportDto>> listReports(OperationReportQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(operationReportService.listReports(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping("/{reportId}")
    public CommonResult<OperationReportDto> getReport(@PathVariable Long reportId) {
        RequestContext context = RequestContextHolder.current();
        return operationReportService.getReport(context.getTenantId(), context.getShopId(), reportId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("Report not found"));
    }
}
