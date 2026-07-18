package com.sirithree.shopops.admin.report.controller;

import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import com.sirithree.shopops.common.api.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class OperationReportController {
    private final OperationReportService operationReportService;

    public OperationReportController(OperationReportService operationReportService) {
        this.operationReportService = operationReportService;
    }

    @GetMapping("/{reportId}")
    public CommonResult<OperationReportDto> getReport(@RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId,
                                                      @RequestHeader(value = "X-Shop-Id", defaultValue = "1") Long shopId,
                                                      @PathVariable Long reportId) {
        return operationReportService.getReport(tenantId, shopId, reportId)
                .map(CommonResult::success)
                .orElseGet(() -> CommonResult.failed("报告不存在"));
    }
}
