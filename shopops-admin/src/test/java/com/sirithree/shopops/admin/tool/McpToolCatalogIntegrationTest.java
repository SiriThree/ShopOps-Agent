package com.sirithree.shopops.admin.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class McpToolCatalogIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void shouldExposeEighteenPortfolioMcpTools() {
        List<Map<String, Object>> tools = (List<Map<String, Object>>) dataOf(get("/api/tools", adminHeaders()));

        assertThat(tools).hasSize(18);
        assertThat(tools)
                .extracting(tool -> tool.get("toolCode"))
                .containsExactly(
                        "order.query_summary",
                        "order.query_detail",
                        "order.query_refund_risk",
                        "order.refund_execute",
                        "comment.query_negative",
                        "comment.analyze_sentiment",
                        "comment.create_reply_draft",
                        "product.query_candidates",
                        "product.query_low_click",
                        "product.optimize_title",
                        "product.update_title",
                        "ad.query_performance",
                        "ad.query_low_roi",
                        "ad.suggest_budget",
                        "report.query_external_metrics",
                        "report.generate_daily_review",
                        "report.export_excel",
                        "feishu.sync_report"
                );

        assertHighRiskApprovalTool(tools, "order.refund_execute");
        assertHighRiskApprovalTool(tools, "product.update_title");
        assertHighRiskApprovalTool(tools, "ad.suggest_budget");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldInvokeNewAnalysisAndCollaborationTools() {
        Map<String, Object> productResult = dataOf(post(
                "/api/tools/product.optimize_title/invoke",
                Map.of("shopId", 1, "productId", "PRD-LOW-001"),
                adminHeaders()
        ));
        assertThat(productResult)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS");
        Map<String, Object> productData = (Map<String, Object>) productResult.get("data");
        assertThat((List<Map<String, Object>>) productData.get("titleSuggestions")).isNotEmpty();

        Map<String, Object> excelResult = dataOf(post(
                "/api/tools/report.export_excel/invoke",
                Map.of("shopId", 1, "reportId", 90001),
                adminHeaders()
        ));
        assertThat(excelResult)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS");
        assertThat((Map<String, Object>) excelResult.get("data"))
                .containsEntry("status", "EXPORTED")
                .containsEntry("fileName", "shopops-operation-report-demo.xlsx");

        Map<String, Object> feishuResult = dataOf(post(
                "/api/tools/feishu.sync_report/invoke",
                Map.of("shopId", 1, "reportId", 90001),
                adminHeaders()
        ));
        assertThat(feishuResult)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS");
        assertThat((Map<String, Object>) feishuResult.get("data"))
                .containsEntry("status", "SYNCED")
                .containsEntry("mode", "demo-connector");
    }

    @Test
    void shouldRequireApprovalForProductAndAdRiskTools() {
        Map<String, Object> productResult = dataOf(post(
                "/api/tools/product.update_title/invoke",
                Map.of("shopId", 1, "productId", "PRD-LOW-001", "newTitle", "Risk controlled title update"),
                adminHeaders()
        ));
        assertThat(productResult)
                .containsEntry("success", false)
                .containsEntry("status", "APPROVAL_REQUIRED")
                .containsEntry("errorCode", "APPROVAL_REQUIRED");

        Map<String, Object> adResult = dataOf(post(
                "/api/tools/ad.suggest_budget/invoke",
                Map.of("shopId", 1, "campaignId", "AD-LOW-001", "changePercent", -20),
                adminHeaders()
        ));
        assertThat(adResult)
                .containsEntry("success", false)
                .containsEntry("status", "APPROVAL_REQUIRED")
                .containsEntry("errorCode", "APPROVAL_REQUIRED");
    }

    private void assertHighRiskApprovalTool(List<Map<String, Object>> tools, String toolCode) {
        Map<String, Object> tool = tools.stream()
                .filter(item -> toolCode.equals(item.get("toolCode")))
                .findFirst()
                .orElseThrow();
        assertThat(tool)
                .containsEntry("riskLevel", "high")
                .containsEntry("needApproval", true);
    }

    private Map<String, Object> get(String path, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<String, Object> post(String path, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(
                url(path),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private <T> T dataOf(Map<String, Object> response) {
        assertThat(response).isNotNull();
        assertThat(response.get("code")).isEqualTo(200);
        return (T) response.get("data");
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");
        headers.set("X-User-Roles", "ADMIN");
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
