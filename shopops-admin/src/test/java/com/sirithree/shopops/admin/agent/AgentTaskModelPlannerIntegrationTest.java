package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelProviderClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "shopops.persistence=memory",
                "shopops.model-gateway.planner.enabled=true",
                "shopops.model-gateway.planner.provider-code=plan-json",
                "shopops.model-gateway.planner.prompt-code=daily_review.plan"
        }
)
class AgentTaskModelPlannerIntegrationTest extends AbstractAgentTaskFlowIntegrationTest {
    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateDailyReviewTaskWithModelGatewayPlannerWhenEnabled() {
        Map<String, Object> createData = createDailyReviewTask();
        assertThat(createData.get("status")).isEqualTo("SUCCESS");

        Integer taskId = ((Number) createData.get("taskId")).intValue();
        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps"));
        assertThat(steps).extracting(step -> step.get("toolCode"))
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates", "report.generate_daily_review");

        Map<String, Object> modelCallLogs = dataOf(get("/api/admin/model-gateway/call-logs?taskId=" + taskId));
        assertThat(modelCallLogs.get("total")).isEqualTo(1);
        Map<String, Object> modelCall = ((List<Map<String, Object>>) modelCallLogs.get("list")).get(0);
        assertThat(modelCall)
                .containsEntry("providerCode", "plan-json")
                .containsEntry("promptCode", "daily_review.plan")
                .containsEntry("status", "SUCCESS");
    }

    @TestConfiguration
    static class PlannerProviderConfiguration {
        @Bean
        ModelProviderClient planJsonModelProviderClient() {
            return new ModelProviderClient() {
                @Override
                public String providerCode() {
                    return "plan-json";
                }

                @Override
                public String defaultModelName() {
                    return "plan-json-001";
                }

                @Override
                public ModelInvokeResult invoke(ModelInvokeParam param) {
                    ModelInvokeResult result = new ModelInvokeResult();
                    result.setProviderCode(providerCode());
                    result.setModelName(defaultModelName());
                    result.setStatus(ModelCallStatus.SUCCESS);
                    result.setOutputText("""
                            {"taskType":"daily_review","steps":[
                              {"stepNo":1,"stepName":"模型规划：订单指标","toolCode":"order.query_summary"},
                              {"stepNo":2,"stepName":"模型规划：差评风险","toolCode":"comment.query_negative"},
                              {"stepNo":3,"stepName":"模型规划：商品优化","toolCode":"product.query_candidates"},
                              {"stepNo":4,"stepName":"模型规划：生成报告","toolCode":"report.generate_daily_review"}
                            ]}
                            """);
                    return result;
                }
            };
        }
    }
}
