package com.sirithree.shopops.admin.business.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BusinessFileSummaryReader {
    private static final TypeReference<List<OrderSummaryRecord>> ORDER_SUMMARY_RECORDS = new TypeReference<>() {
    };
    private static final TypeReference<List<NegativeCommentRecord>> NEGATIVE_COMMENT_RECORDS = new TypeReference<>() {
    };
    private static final TypeReference<List<ProductCandidateRecord>> PRODUCT_CANDIDATE_RECORDS = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String orderSummaryFile;
    private final String negativeCommentsFile;
    private final String productCandidatesFile;

    public BusinessFileSummaryReader(ObjectMapper objectMapper,
                                     @Value("${shopops.connector.order-summary.file:}") String orderSummaryFile,
                                     @Value("${shopops.connector.negative-comments.file:}") String negativeCommentsFile,
                                     @Value("${shopops.connector.product-candidates.file:}") String productCandidatesFile) {
        this.objectMapper = objectMapper;
        this.orderSummaryFile = orderSummaryFile;
        this.negativeCommentsFile = negativeCommentsFile;
        this.productCandidatesFile = productCandidatesFile;
    }

    public Optional<Map<String, Object>> orderSummary(Long tenantId, Long shopId, LocalDate startDate, LocalDate endDate) {
        if (orderSummaryFile == null || orderSummaryFile.isBlank()) {
            return Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(orderSummaryFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Order summary data file does not exist: " + path);
        }
        try {
            return objectMapper.readValue(path.toFile(), ORDER_SUMMARY_RECORDS).stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(record -> withSource(record.summary(), "file.order-summary"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read order summary data file: " + path, ex);
        }
    }

    public Optional<Map<String, Object>> negativeComments(Long tenantId,
                                                          Long shopId,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          Integer minStar) {
        if (negativeCommentsFile == null || negativeCommentsFile.isBlank()) {
            return Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(negativeCommentsFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Negative comment data file does not exist: " + path);
        }
        try {
            return objectMapper.readValue(path.toFile(), NEGATIVE_COMMENT_RECORDS).stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .filter(record -> record.minStar() == null || record.minStar() >= safeMinStar(minStar))
                    .findFirst()
                    .map(record -> withSource(record.summary(), "file.negative-comments"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read negative comment data file: " + path, ex);
        }
    }

    public Optional<Map<String, Object>> productCandidates(Long tenantId,
                                                           Long shopId,
                                                           LocalDate startDate,
                                                           LocalDate endDate,
                                                           Integer limit) {
        if (productCandidatesFile == null || productCandidatesFile.isBlank()) {
            return Optional.empty();
        }
        Path path = ConfiguredFilePathResolver.resolve(productCandidatesFile);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Product candidate data file does not exist: " + path);
        }
        try {
            return objectMapper.readValue(path.toFile(), PRODUCT_CANDIDATE_RECORDS).stream()
                    .filter(record -> same(record.tenantId(), tenantId))
                    .filter(record -> same(record.shopId(), shopId))
                    .filter(record -> startDate.equals(LocalDate.parse(record.startDate())))
                    .filter(record -> endDate.equals(LocalDate.parse(record.endDate())))
                    .findFirst()
                    .map(record -> withSource(limitSummary(record.summary(), limit), "file.product-candidates"));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read product candidate data file: " + path, ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> limitSummary(Map<String, Object> summary, Integer limit) {
        Object value = summary.get("products");
        if (!(value instanceof List<?> products)) {
            return summary;
        }
        int safeLimit = limit == null || limit <= 0 ? products.size() : Math.min(limit, products.size());
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(summary);
        result.put("candidateCount", safeLimit);
        result.put("products", ((List<Object>) products).subList(0, safeLimit));
        return result;
    }

    private Map<String, Object> withSource(Map<String, Object> summary, String source) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(summary);
        result.put("connectorCode", source);
        return result;
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private int safeMinStar(Integer minStar) {
        return minStar == null || minStar <= 0 ? 3 : minStar;
    }

    private record OrderSummaryRecord(Long tenantId,
                                      Long shopId,
                                      String startDate,
                                      String endDate,
                                      Map<String, Object> summary) {
    }

    private record NegativeCommentRecord(Long tenantId,
                                         Long shopId,
                                         String startDate,
                                         String endDate,
                                         Integer minStar,
                                         Map<String, Object> summary) {
    }

    private record ProductCandidateRecord(Long tenantId,
                                          Long shopId,
                                          String startDate,
                                          String endDate,
                                          Map<String, Object> summary) {
    }
}
