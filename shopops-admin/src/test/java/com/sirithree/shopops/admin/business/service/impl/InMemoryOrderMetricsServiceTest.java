package com.sirithree.shopops.admin.business.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InMemoryOrderMetricsServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void shouldLoadOrderSummaryFromConfiguredJsonFile() throws Exception {
        Path file = tempDir.resolve("order-summary.json");
        Files.writeString(file, """
                [
                  {
                    "tenantId": 1,
                    "shopId": 1,
                    "startDate": "2026-07-18",
                    "endDate": "2026-07-18",
                    "summary": {
                      "gmv": 3210.50,
                      "orderCount": 21,
                      "refundAmount": 88.00,
                      "refundRate": 0.0274,
                      "avgOrderAmount": 152.88,
                      "compareYesterday": {
                        "gmvGrowth": 0.1200,
                        "orderGrowth": 0.0500
                      },
                      "compareSevenDayAvg": {
                        "gmvGrowth": 0.0800,
                        "refundRateDelta": -0.0100
                      }
                    }
                  }
                ]
                """);

        InMemoryOrderMetricsService service = new InMemoryOrderMetricsService(new ObjectMapper(), file.toString());

        Map<String, Object> summary = service.querySummary(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"));

        assertThat(summary)
                .containsEntry("gmv", 3210.50)
                .containsEntry("orderCount", 21)
                .containsEntry("connectorCode", "file.order-summary");
        assertThat((Map<String, Object>) summary.get("compareYesterday"))
                .containsEntry("gmvGrowth", 0.1200);
    }

    @Test
    void shouldFallbackToDefaultSummaryWhenFileIsNotConfigured() {
        InMemoryOrderMetricsService service = new InMemoryOrderMetricsService(new ObjectMapper(), "");

        Map<String, Object> summary = service.querySummary(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"));

        assertThat(summary)
                .containsEntry("gmv", 128936.50)
                .containsEntry("orderCount", 842)
                .containsEntry("connectorCode", "memory.default");
    }
}
