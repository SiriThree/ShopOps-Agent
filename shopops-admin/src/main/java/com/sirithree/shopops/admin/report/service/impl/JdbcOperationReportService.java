package com.sirithree.shopops.admin.report.service.impl;

import com.sirithree.shopops.admin.common.JacksonJsonSupport;
import com.sirithree.shopops.admin.persistence.mapper.OperationReportMapper;
import com.sirithree.shopops.admin.persistence.model.OperationReport;
import com.sirithree.shopops.admin.report.domain.OperationReportDto;
import com.sirithree.shopops.admin.report.service.OperationReportService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "jdbc")
public class JdbcOperationReportService implements OperationReportService {
    private final OperationReportMapper operationReportMapper;
    private final JacksonJsonSupport jsonSupport;

    public JdbcOperationReportService(OperationReportMapper operationReportMapper, JacksonJsonSupport jsonSupport) {
        this.operationReportMapper = operationReportMapper;
        this.jsonSupport = jsonSupport;
    }

    @Override
    public OperationReportDto createDailyReviewReport(Long tenantId, Long shopId, Long taskId, Long userId, String traceId, Map<String, Object> reportData) {
        OperationReport report = new OperationReport();
        report.setTenantId(tenantId);
        report.setShopId(shopId);
        report.setTaskId(taskId);
        report.setReportNo("RPT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        report.setReportType("daily_review");
        report.setTitle((String) reportData.get("title"));
        report.setContentMarkdown((String) reportData.get("markdown"));
        report.setContentJson(jsonSupport.toJson(reportData));
        report.setEvidenceJson(jsonSupport.toJson(reportData.get("evidence")));
        report.setTraceId(traceId);
        report.setStatus("SUCCESS");
        report.setCreatedBy(userId);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        operationReportMapper.insert(report);
        return toDto(report);
    }

    @Override
    public Optional<OperationReportDto> getReport(Long tenantId, Long shopId, Long reportId) {
        OperationReport report = operationReportMapper.selectById(tenantId, shopId, reportId);
        return Optional.ofNullable(report).map(this::toDto);
    }

    private OperationReportDto toDto(OperationReport report) {
        OperationReportDto dto = new OperationReportDto();
        dto.setReportId(report.getId());
        dto.setTaskId(report.getTaskId());
        dto.setReportNo(report.getReportNo());
        dto.setReportType(report.getReportType());
        dto.setTitle(report.getTitle());
        dto.setMarkdown(report.getContentMarkdown());
        dto.setEvidence(jsonSupport.toMap(report.getEvidenceJson()));
        dto.setTraceId(report.getTraceId());
        dto.setStatus(report.getStatus());
        return dto;
    }
}
