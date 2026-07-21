package com.sirithree.shopops.admin.business.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.business.service.CommentRiskService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "shopops.persistence", havingValue = "memory", matchIfMissing = true)
public class InMemoryCommentRiskService implements CommentRiskService {
    private static final TypeReference<List<NegativeCommentRecord>> NEGATIVE_COMMENT_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String negativeCommentsFile;

    public InMemoryCommentRiskService(ObjectMapper objectMapper,
                                      @Value("${shopops.connector.negative-comments.file:}") String negativeCommentsFile) {
        this.objectMapper = objectMapper;
        this.negativeCommentsFile = negativeCommentsFile;
    }

    @Override
    public Map<String, Object> queryNegativeComments(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer minStar) {
        return fileSummary(tenantId, shopId, startDate, endDate, minStar)
                .map(summary -> withSource(summary, "file.negative-comments"))
                .orElseGet(() -> withSource(defaultSummary(), "memory.default"));
    }

    private Optional<Map<String, Object>> fileSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate, Integer minStar) {
        if (negativeCommentsFile == null || negativeCommentsFile.isBlank()) {
            return Optional.empty();
        }
        Path path = Path.of(negativeCommentsFile.trim());
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("差评风险数据文件不存在: " + path);
        }
        try {
            List<NegativeCommentRecord> records = objectMapper.readValue(path.toFile(), NEGATIVE_COMMENT_RECORDS);
            return records.stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .filter(record -> record.minStar() == null || record.minStar() >= safeMinStar(minStar))
                    .findFirst()
                    .map(NegativeCommentRecord::summary);
        } catch (IOException ex) {
            throw new IllegalArgumentException("差评风险数据文件读取失败: " + path, ex);
        }
    }

    private Map<String, Object> defaultSummary() {
        return Map.of(
                "negativeCount", 7,
                "riskComments", List.of(
                        Map.of("commentId", 50101, "productId", 1001, "productName", "轻量保温杯 500ml", "star", 2, "content", "物流慢，包装有破损", "riskKeywords", List.of("物流慢", "破损")),
                        Map.of("commentId", 50102, "productId", 1008, "productName", "便携收纳箱", "star", 1, "content", "描述不符，申请退款", "riskKeywords", List.of("描述不符", "退款"))
                ),
                "categoryStats", Map.of("物流慢", 3, "描述不符", 2, "包装破损", 2)
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

    private int safeMinStar(Integer minStar) {
        return minStar == null || minStar <= 0 ? 3 : minStar;
    }

    private record NegativeCommentRecord(Long tenantId,
                                         Long shopId,
                                         String startDate,
                                         String endDate,
                                         Integer minStar,
                                         Map<String, Object> summary) {
    }
}
