package com.sirithree.shopops.admin.business.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InMemoryProductOptimizationServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void shouldLoadProductCandidatesFromConfiguredJsonFileAndApplyLimit() throws Exception {
        Path file = tempDir.resolve("product-candidates.json");
        Files.writeString(file, """
                [
                  {
                    "tenantId": 1,
                    "shopId": 1,
                    "startDate": "2026-07-18",
                    "endDate": "2026-07-18",
                    "summary": {
                      "candidateCount": 2,
                      "products": [
                        {
                          "productId": 2001,
                          "productName": "智能恒温水杯",
                          "reason": "差评集中",
                          "score": 91.2
                        },
                        {
                          "productId": 2008,
                          "productName": "厨房分格收纳盒",
                          "reason": "描述不符",
                          "score": 87.5
                        }
                      ]
                    }
                  }
                ]
                """);

        InMemoryProductOptimizationService service = new InMemoryProductOptimizationService(new ObjectMapper(), file.toString());

        Map<String, Object> summary = service.queryCandidates(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"), 1);

        assertThat(summary)
                .containsEntry("candidateCount", 1)
                .containsEntry("connectorCode", "file.product-candidates");
        assertThat((List<Map<String, Object>>) summary.get("products"))
                .extracting(item -> item.get("productId"))
                .containsExactly(2001);
    }

    @Test
    void shouldFallbackToDefaultProductCandidatesWhenFileIsNotConfigured() {
        InMemoryProductOptimizationService service = new InMemoryProductOptimizationService(new ObjectMapper(), "");

        Map<String, Object> summary = service.queryCandidates(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"), 2);

        assertThat(summary)
                .containsEntry("candidateCount", 2)
                .containsEntry("connectorCode", "memory.default");
    }
}
