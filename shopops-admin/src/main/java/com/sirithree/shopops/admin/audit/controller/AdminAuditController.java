package com.sirithree.shopops.admin.audit.controller;

import com.sirithree.shopops.admin.audit.domain.AdminAuditExportDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditOverviewDto;
import com.sirithree.shopops.admin.audit.domain.AdminAuditRiskSummaryDto;
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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/high-risk")
    public CommonResult<AdminAuditRiskSummaryDto> getRiskSummary() {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(adminAuditService.getRiskSummary(context.getTenantId(), context.getShopId()));
    }

    @GetMapping("/export")
    public CommonResult<AdminAuditExportDto> exportTimeline(AdminAuditTimelineQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        return CommonResult.success(adminAuditService.exportTimeline(context.getTenantId(), context.getShopId(), param));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> downloadTimelineCsv(AdminAuditTimelineQueryParam param) {
        RequestContext context = RequestContextHolder.current();
        AdminAuditExportDto export = adminAuditService.exportTimeline(context.getTenantId(), context.getShopId(), param);
        byte[] body = csv(export).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + export.getFileName() + "\"")
                .body(body);
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

    private String csv(AdminAuditExportDto export) {
        StringBuilder builder = new StringBuilder();
        appendCsvLine(builder, export.getColumns());
        for (Map<String, Object> row : export.getRows()) {
            appendCsvLine(builder, export.getColumns().stream()
                    .map(column -> row.get(column))
                    .toList());
        }
        return builder.toString();
    }

    private void appendCsvLine(StringBuilder builder, List<?> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escapeCsv(values.get(i)));
        }
        builder.append("\r\n");
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean formulaLike = text.startsWith("=") || text.startsWith("+") || text.startsWith("-") || text.startsWith("@");
        if (formulaLike) {
            text = "\t" + text;
        }
        if (text.contains("\"") || text.contains(",") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
