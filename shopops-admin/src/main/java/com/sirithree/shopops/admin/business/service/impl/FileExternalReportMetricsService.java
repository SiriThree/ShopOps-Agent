package com.sirithree.shopops.admin.business.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.business.service.ExternalReportMetricsService;
import com.sirithree.shopops.admin.business.support.ConfiguredFilePathResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileExternalReportMetricsService implements ExternalReportMetricsService {
    private static final TypeReference<List<ExternalReportRecord>> EXTERNAL_REPORT_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String externalReportsFile;

    public FileExternalReportMetricsService(ObjectMapper objectMapper,
                                            @Value("${shopops.connector.external-reports.file:}") String externalReportsFile) {
        this.objectMapper = objectMapper;
        this.externalReportsFile = externalReportsFile;
    }

    @Override
    public Map<String, Object> queryMetrics(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        return fileSummary(tenantId, shopId, startDate, endDate)
                .map(summary -> withSource(summary, "file.external-reports"))
                .orElseGet(() -> withSource(defaultSummary(), "memory.default"));
    }

    private Optional<Map<String, Object>> fileSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        if (externalReportsFile == null || externalReportsFile.isBlank()) {
            return Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(externalReportsFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("外部报表数据文件不存在: " + path);
        }
        try {
            List<ExternalReportRecord> records = objectMapper.readValue(path.toFile(), EXTERNAL_REPORT_RECORDS);
            return records.stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(ExternalReportRecord::summary);
        } catch (IOException ex) {
            throw new IllegalArgumentException("外部报表数据文件读取失败: " + path, ex);
        }
    }

    private Map<String, Object> defaultSummary() {
        return Map.of(
                "visitorCount", 36520,
                "newVisitorCount", 12860,
                "conversionRate", 0.031,
                "repeatPurchaseRate", 0.184,
                "favoriteCount", 4210,
                "cartAddCount", 2980,
                "topChannels", List.of(
                        Map.of("channelName", "自然搜索", "visitorCount", 14200, "conversionRate", 0.038),
                        Map.of("channelName", "短视频引流", "visitorCount", 9600, "conversionRate", 0.027),
                        Map.of("channelName", "店铺会员", "visitorCount", 5300, "conversionRate", 0.052)
                )
        );
    }

    private Map<String, Object> withSource(Map<String, Object> summary, String source) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(summary);
        result.put("connectorCode", source);
        return result;
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private record ExternalReportRecord(Long tenantId,
                                        Long shopId,
                                        String startDate,
                                        String endDate,
                                        Map<String, Object> summary) {
    }
}
