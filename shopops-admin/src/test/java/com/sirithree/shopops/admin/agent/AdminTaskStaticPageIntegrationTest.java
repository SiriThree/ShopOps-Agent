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
class AdminTaskStaticPageIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldServeAdminTaskStaticPage() {
        TestRestTemplate restTemplate = new TestRestTemplate();

        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/admin/tasks.html",
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
                .contains("ShopOps 任务队列")
                .contains("任务队列")
                .contains("创建任务")
                .contains("任务详情")
                .contains("id=\"configSnapshotBox\"")
                .contains("renderConfigSnapshot")
                .contains("退款率预警阈值")
                .contains("高风险工具审批")
                .contains("最近事件")
                .contains("/api/agent/tasks")
                .contains("/api/agent/tasks/${encodeURIComponent(state.selectedTaskId)}/retry")
                .contains("/api/agent/tasks/stale/requeue")
                .contains("applyInitialQuery")
                .contains("syncUrl")
                .contains("window.history.replaceState")
                .contains("positiveInt(params.get(\"pageNum\"), 1)")
                .contains("positiveInt(params.get(\"pageSize\"), 20)")
                .contains("empty-state")
                .contains("data-retry-list")
                .contains("errorRow")
                .contains("id=\"createTaskSubmit\"")
                .contains("id=\"taskFilterSubmit\"")
                .contains("withBusy")
                .contains("查询中")
                .contains("重置中")
                .contains("加载中")
                .contains("刷新中")
                .contains("重排中")
                .contains("id=\"copyDetail\"")
                .contains("copyText")
                .contains("navigator.clipboard")
                .contains("fallbackCopy")
                .contains("shopops.auth.token")
                .contains("shopops.auth.user")
                .contains("Authorization")
                .contains("applyStoredSession")
                .contains("id=\"sessionLine\"")
                .contains("/admin/reports.html?reportId=")
                .contains("/admin/audit.html?source=TASK&taskId=")
                .contains("/admin/tools.html?taskId=")
                .contains("/api/admin/agent/tasks")
                .contains("/api/admin/agent/tasks/metrics")
                .contains("/api/admin/agent/tasks/events")
                .contains("/api/admin/agent/tasks/${encodeURIComponent(taskId)}/detail")
                .contains("data-quick-filter=\"failed\"")
                .contains("/admin/dashboard.html")
                .contains("/admin/audit.html");
    }
}
