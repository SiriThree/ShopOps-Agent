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

class InMemoryCommentRiskServiceTest {
    @TempDir
    private Path tempDir;

    @Test
    void shouldLoadNegativeCommentsFromConfiguredJsonFile() throws Exception {
        Path file = tempDir.resolve("negative-comments.json");
        Files.writeString(file, """
                [
                  {
                    "tenantId": 1,
                    "shopId": 1,
                    "startDate": "2026-07-18",
                    "endDate": "2026-07-18",
                    "minStar": 3,
                    "summary": {
                      "negativeCount": 1,
                      "riskComments": [
                        {
                          "commentId": 88001,
                          "productId": 2001,
                          "productName": "智能恒温水杯",
                          "star": 2,
                          "content": "杯盖渗水，客服响应慢",
                          "riskKeywords": ["渗水", "客服慢"]
                        }
                      ],
                      "categoryStats": {
                        "客服慢": 1
                      }
                    }
                  }
                ]
                """);

        InMemoryCommentRiskService service = new InMemoryCommentRiskService(new ObjectMapper(), file.toString());

        Map<String, Object> summary = service.queryNegativeComments(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"), 3);

        assertThat(summary)
                .containsEntry("negativeCount", 1)
                .containsEntry("connectorCode", "file.negative-comments");
        assertThat((List<Map<String, Object>>) summary.get("riskComments"))
                .extracting(item -> item.get("commentId"))
                .containsExactly(88001);
    }

    @Test
    void shouldFallbackToDefaultNegativeCommentsWhenFileIsNotConfigured() {
        InMemoryCommentRiskService service = new InMemoryCommentRiskService(new ObjectMapper(), "");

        Map<String, Object> summary = service.queryNegativeComments(1L, 1L, LocalDate.parse("2026-07-18"), LocalDate.parse("2026-07-18"), 3);

        assertThat(summary)
                .containsEntry("negativeCount", 7)
                .containsEntry("connectorCode", "memory.default");
    }
}
