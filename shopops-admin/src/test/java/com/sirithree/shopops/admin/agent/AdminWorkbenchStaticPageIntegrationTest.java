package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "shopops.persistence=memory"
)
class AdminWorkbenchStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminWorkbenchStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/workbench.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .contains("text/html")
                .contains("charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
                .contains("no-store")
                .contains("no-cache");
        assertThat(response.getBody())
                .contains("ShopOps Agent 工作台")
                .contains("Agent 工作台")
                .contains("一句话发起任务")
                .contains("演示任务")
                .contains("data-demo-run=\"true\"")
                .contains("经营日报")
                .contains("差评专项")
                .contains("商品优化")
                .contains("投放异常")
                .contains("演示链路")
                .contains("demoFlow")
                .contains("renderDemoFlow")
                .contains("自然语言发起")
                .contains("审计链路追踪")
                .contains("工具编排")
                .contains("关键结论")
                .contains("运营报告")
                .contains("Agent 理解结果")
                .contains("focusAreas")
                .contains("dataSources")
                .contains("recommendedActions")
                .contains("intentLabel")
                .contains("renderUnderstanding")
                .contains("renderInsights")
                .contains("pollingTimer")
                .contains("startTaskTracking")
                .contains("stopTaskTracking")
                .contains("isTerminalStatus")
                .contains("window.setInterval")
                .contains("quantPanel")
                .contains("renderQuantPanel")
                .contains("outputByTool")
                .contains("resultTable")
                .contains("numberText")
                .contains("data-grid")
                .contains("intentTitle")
                .contains("evidenceSummary")
                .contains("toolChainSummary")
                .contains("generationSummary")
                .contains("configSummary")
                .contains("report?.summary")
                .contains("task.resultSummary")
                .contains("/api/agent/tasks/natural-language")
                .contains("/api/agent/tasks/${encodeURIComponent(state.selectedTaskId)}/steps")
                .contains("/api/admin/agent/tasks?pageNum=1&pageSize=5")
                .contains("/admin/audit.html?source=TASK&taskId=")
                .contains("shopops.auth.token")
                .contains("Authorization");
    }

    @Test
    void shouldServeAdminIndexAsWorkbenchEntry() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/index.html",
                String.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody())
                .contains("/admin/workbench.html")
                .contains("打开 ShopOps Agent 工作台");
    }
}
