package com.sirithree.shopops.admin.business.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.business.service.AdPerformanceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FileAdPerformanceService implements AdPerformanceService {
    private static final TypeReference<List<AdPerformanceRecord>> AD_PERFORMANCE_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String adPerformanceFile;

    public FileAdPerformanceService(ObjectMapper objectMapper,
                                    @Value("${shopops.connector.ad-performance.file:}") String adPerformanceFile) {
        this.objectMapper = objectMapper;
        this.adPerformanceFile = adPerformanceFile;
    }

    @Override
    public Map<String, Object> queryPerformance(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        return fileSummary(tenantId, shopId, startDate, endDate)
                .map(summary -> withSource(summary, "file.ad-performance"))
                .orElseGet(() -> withSource(defaultSummary(), "memory.default"));
    }

    private Optional<Map<String, Object>> fileSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        if (adPerformanceFile == null || adPerformanceFile.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(adPerformanceFile.trim());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("广告投放数据文件不存在: " + path);
        }
        try {
            List<AdPerformanceRecord> records = objectMapper.readValue(path.toFile(), AD_PERFORMANCE_RECORDS);
            return records.stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(AdPerformanceRecord::summary);
        } catch (IOException ex) {
            throw new IllegalArgumentException("广告投放数据文件读取失败: " + path, ex);
        }
    }

    private Map<String, Object> defaultSummary() {
        return Map.of(
                "spend", 18600.00,
                "impressions", 420000,
                "clicks", 18600,
                "ctr", 0.0443,
                "cpc", 1.00,
                "conversionRate", 0.086,
                "roi", 3.72,
                "campaigns", List.of(
                        Map.of("campaignName", "夏季补水主推", "spend", 9200.00, "roi", 4.18, "conversionRate", 0.094),
                        Map.of("campaignName", "收纳好物拉新", "spend", 6100.00, "roi", 2.86, "conversionRate", 0.071),
                        Map.of("campaignName", "运动毛巾复购", "spend", 3300.00, "roi", 3.31, "conversionRate", 0.083)
                )
        );
    }

    private Map<String, Object> withSource(Map<String, Object> summary, String source) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>(summary);
        result.put("connectorCode", source);
        return result;
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private record AdPerformanceRecord(Long tenantId,
                                       Long shopId,
                                       String startDate,
                                       String endDate,
                                       Map<String, Object> summary) {
    }
}
