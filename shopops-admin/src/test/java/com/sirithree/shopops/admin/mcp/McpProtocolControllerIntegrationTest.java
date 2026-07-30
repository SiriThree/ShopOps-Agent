package com.sirithree.shopops.admin.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.mcp.sse.McpSseSession;
import com.sirithree.shopops.admin.mcp.sse.McpSseSessionRegistry;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class McpProtocolControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private McpSseSessionRegistry sseSessionRegistry;

    @Test
    @SuppressWarnings("unchecked")
    void shouldDiscoverServerAndListToolsOverJsonRpc() {
        Map<String, Object> discovery = post(Map.of(
                "jsonrpc", "2.0",
                "id", "discover-1",
                "method", "server/discover"
        ));
        Map<String, Object> discoveryResult = (Map<String, Object>) discovery.get("result");
        assertThat(discoveryResult.get("protocolVersions")).asList().contains("2026-07-28");
        assertThat((Map<String, Object>) discoveryResult.get("capabilities")).containsKey("tools");

        Map<String, Object> response = post(Map.of(
                "jsonrpc", "2.0",
                "id", "tools-1",
                "method", "tools/list"
        ));
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        List<Map<String, Object>> tools = (List<Map<String, Object>>) result.get("tools");
        assertThat(tools).hasSize(18);
        assertThat(tools)
                .extracting(tool -> tool.get("name"))
                .contains("order.query_summary", "report.generate_daily_review", "feishu.sync_report");
        Map<String, Object> orderSummary = tools.stream()
                .filter(tool -> "order.query_summary".equals(tool.get("name")))
                .findFirst()
                .orElseThrow();
        assertThat(orderSummary).containsKeys("description", "inputSchema", "annotations");
        assertThat((Map<String, Object>) orderSummary.get("inputSchema")).containsEntry("type", "object");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCallToolOverMcpToolsCall() {
        Map<String, Object> response = post(Map.of(
                "jsonrpc", "2.0",
                "id", "call-1",
                "method", "tools/call",
                "params", Map.of(
                        "name", "order.query_summary",
                        "arguments", Map.of("startDate", "2018-08-07", "endDate", "2018-08-07")
                )
        ));

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsEntry("isError", false);
        assertThat((List<Map<String, Object>>) result.get("content"))
                .extracting(item -> item.get("type"))
                .containsExactly("text");
        Map<String, Object> structured = (Map<String, Object>) result.get("structuredContent");
        assertThat(structured)
                .containsEntry("success", true)
                .containsEntry("status", "SUCCESS");
        assertThat((Map<String, Object>) structured.get("data"))
                .containsKeys("gmv", "orderCount", "refundRate");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnMcpToolErrorForApprovalRequiredTool() {
        Map<String, Object> response = post(Map.of(
                "jsonrpc", "2.0",
                "id", "call-risk-1",
                "method", "tools/call",
                "params", Map.of(
                        "name", "ad.suggest_budget",
                        "arguments", Map.of("campaignId", "AD-LOW-001", "changePercent", -20)
                )
        ));

        Map<String, Object> result = (Map<String, Object>) response.get("result");
        assertThat(result).containsEntry("isError", true);
        Map<String, Object> structured = (Map<String, Object>) result.get("structuredContent");
        assertThat(structured)
                .containsEntry("success", false)
                .containsEntry("status", "APPROVAL_REQUIRED")
                .containsEntry("errorCode", "APPROVAL_REQUIRED");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAcceptMcpMessageOverSseSession() {
        McpSseSession session = sseSessionRegistry.createSession();
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/mcp/messages?sessionId=" + session.getSessionId(),
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "jsonrpc", "2.0",
                        "id", "sse-tools-1",
                        "method", "tools/list"
                ), adminHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody)
                .containsEntry("accepted", true)
                .containsEntry("sessionId", session.getSessionId())
                .containsEntry("id", "sse-tools-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldRejectMcpSseMessageForMissingSession() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/mcp/messages?sessionId=missing",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "jsonrpc", "2.0",
                        "id", "sse-missing-1",
                        "method", "initialize"
                ), adminHeaders()),
                Map.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("accepted", false);
    }

    private Map<String, Object> post(Map<String, Object> body) {
        ResponseEntity<Map> response = restTemplate.exchange(
                "http://localhost:" + port + "/mcp",
                HttpMethod.POST,
                new HttpEntity<>(body, adminHeaders()),
                Map.class
        );
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst("MCP-Protocol-Version")).isEqualTo("2026-07-28");
        Map<String, Object> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).containsEntry("jsonrpc", "2.0");
        assertThat(responseBody)
                .as("JSON-RPC error response: %s", responseBody.get("error"))
                .doesNotContainKey("error");
        return responseBody;
    }

    private HttpHeaders adminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", "1");
        headers.set("X-Shop-Id", "1");
        headers.set("X-User-Id", "1");
        headers.set("X-User-Name", "admin");
        headers.set("X-User-Roles", "ADMIN");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
