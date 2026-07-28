package com.sirithree.shopops.admin.business.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sirithree.shopops.admin.business.support.BusinessFileSummaryReader;
import com.sirithree.shopops.admin.persistence.mapper.BusinessCommentMapper;
import com.sirithree.shopops.admin.persistence.mapper.BusinessOrderMapper;
import com.sirithree.shopops.admin.persistence.mapper.BusinessProductMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JdbcBusinessFileConnectorPreferenceTest {
    private final LocalDate demoDate = LocalDate.parse("2018-08-07");

    @Test
    void jdbcOrderMetricsShouldPreferConfiguredFileSummary() {
        BusinessOrderMapper mapper = mock(BusinessOrderMapper.class);
        BusinessFileSummaryReader reader = mock(BusinessFileSummaryReader.class);
        Map<String, Object> fileSummary = Map.of(
                "connectorCode", "file.order-summary",
                "gmv", 62057.77,
                "orderCount", 370
        );
        when(reader.orderSummary(1L, 1L, demoDate, demoDate)).thenReturn(Optional.of(fileSummary));

        Map<String, Object> result = new JdbcOrderMetricsService(mapper, reader)
                .querySummary(1L, 1L, demoDate, demoDate);

        assertThat(result).containsEntry("connectorCode", "file.order-summary")
                .containsEntry("orderCount", 370);
        verifyNoInteractions(mapper);
    }

    @Test
    void jdbcCommentRiskShouldPreferConfiguredFileSummary() {
        BusinessCommentMapper mapper = mock(BusinessCommentMapper.class);
        BusinessFileSummaryReader reader = mock(BusinessFileSummaryReader.class);
        Map<String, Object> fileSummary = Map.of(
                "connectorCode", "file.negative-comments",
                "negativeCount", 51,
                "riskComments", List.of(Map.of("commentId", "review-1"))
        );
        when(reader.negativeComments(1L, 1L, demoDate, demoDate, 3)).thenReturn(Optional.of(fileSummary));

        Map<String, Object> result = new JdbcCommentRiskService(mapper, reader)
                .queryNegativeComments(1L, 1L, demoDate, demoDate, 3);

        assertThat(result).containsEntry("connectorCode", "file.negative-comments")
                .containsEntry("negativeCount", 51);
        verifyNoInteractions(mapper);
    }

    @Test
    void jdbcProductOptimizationShouldPreferConfiguredFileSummary() {
        BusinessProductMapper mapper = mock(BusinessProductMapper.class);
        BusinessFileSummaryReader reader = mock(BusinessFileSummaryReader.class);
        Map<String, Object> fileSummary = Map.of(
                "connectorCode", "file.product-candidates",
                "candidateCount", 10,
                "products", List.of(Map.of("productName", "Furniture Bedroom / 4f18ca98"))
        );
        when(reader.productCandidates(1L, 1L, demoDate, demoDate, 10)).thenReturn(Optional.of(fileSummary));

        Map<String, Object> result = new JdbcProductOptimizationService(mapper, reader)
                .queryCandidates(1L, 1L, demoDate, demoDate, 10);

        assertThat(result).containsEntry("connectorCode", "file.product-candidates")
                .containsEntry("candidateCount", 10);
        verifyNoInteractions(mapper);
    }
}
