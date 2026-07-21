package com.sirithree.shopops.admin.agent.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.DateRangeParam;
import com.sirithree.shopops.admin.model.config.ModelGatewayPlannerProperties;
import com.sirithree.shopops.admin.model.domain.ModelCallLogDto;
import com.sirithree.shopops.admin.model.domain.ModelCallLogQueryParam;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import com.sirithree.shopops.common.api.CommonPage;
import org.junit.jupiter.api.Test;

class RulePlannerServiceTest {
    @Test
    void shouldCreateRulePlanByDefault() {
        RulePlannerService planner = new RulePlannerService(new ModelGatewayPlannerProperties(), new FakeModelGatewayService(""), new ObjectMapper());

        AgentPlan plan = planner.createPlan(context());

        assertThat(plan.getTaskType()).isEqualTo("daily_review");
        assertThat(plan.getSteps()).hasSize(6);
        assertThat(plan.getSteps()).extracting(step -> step.getToolCode())
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");
    }

    @Test
    void shouldCreatePlanThroughModelGatewayWhenEnabled() {
        ModelGatewayPlannerProperties properties = new ModelGatewayPlannerProperties();
        properties.setEnabled(true);
        properties.setProviderCode("fake-planner");
        properties.setPromptCode("daily_review.plan");
        properties.setPromptVersion("v1");
        FakeModelGatewayService modelGatewayService = new FakeModelGatewayService("""
                {"taskType":"daily_review","steps":[
                  {"stepNo":1,"stepName":"模型规划：订单指标","toolCode":"order.query_summary"},
                  {"stepNo":2,"stepName":"模型规划：差评风险","toolCode":"comment.query_negative"},
                  {"stepNo":3,"stepName":"模型规划：商品优化","toolCode":"product.query_candidates"},
                  {"stepNo":4,"stepName":"模型规划：广告投放","toolCode":"ad.query_performance"},
                  {"stepNo":5,"stepName":"模型规划：外部报表","toolCode":"report.query_external_metrics"},
                  {"stepNo":6,"stepName":"模型规划：生成报告","toolCode":"report.generate_daily_review"}
                ]}
                """);
        RulePlannerService planner = new RulePlannerService(properties, modelGatewayService, new ObjectMapper());

        AgentPlan plan = planner.createPlan(context());

        assertThat(plan.getSteps()).extracting(step -> step.getStepName())
                .containsExactly("模型规划：订单指标", "模型规划：差评风险", "模型规划：商品优化",
                        "模型规划：广告投放", "模型规划：外部报表", "模型规划：生成报告");
        assertThat(modelGatewayService.capturedParam.getPromptCode()).isEqualTo("daily_review.plan");
        assertThat(modelGatewayService.capturedParam.getPromptVersion()).isEqualTo("v1");
        assertThat(modelGatewayService.capturedParam.getTraceId()).isEqualTo("tr_planner");
        assertThat(modelGatewayService.capturedParam.getTaskId()).isEqualTo(10001L);
        assertThat(modelGatewayService.capturedParam.getPrompt())
                .contains("任务规划器")
                .contains("report.query_external_metrics")
                .contains("帮我生成今天店铺运营复盘");
    }

    @Test
    void shouldFallbackToRulePlanWhenModelPlanIsUnsafe() {
        ModelGatewayPlannerProperties properties = new ModelGatewayPlannerProperties();
        properties.setEnabled(true);
        FakeModelGatewayService modelGatewayService = new FakeModelGatewayService("""
                {"taskType":"daily_review","steps":[
                  {"stepNo":1,"stepName":"危险步骤","toolCode":"order.refund_execute"}
                ]}
                """);
        RulePlannerService planner = new RulePlannerService(properties, modelGatewayService, new ObjectMapper());

        AgentPlan plan = planner.createPlan(context());

        assertThat(plan.getSteps()).extracting(step -> step.getStepName())
                .containsExactly("查询订单核心指标", "查询差评风险", "查询待优化商品",
                        "查询广告投放指标", "查询外部报表指标", "生成经营复盘报告");
        assertThat(plan.getSteps()).extracting(step -> step.getToolCode())
                .containsExactly("order.query_summary", "comment.query_negative", "product.query_candidates",
                        "ad.query_performance", "report.query_external_metrics", "report.generate_daily_review");
    }

    private AgentTaskContext context() {
        DateRangeParam dateRange = new DateRangeParam();
        dateRange.setStart("2026-07-18");
        dateRange.setEnd("2026-07-18");

        AgentTaskCreateParam createParam = new AgentTaskCreateParam();
        createParam.setTaskType("daily_review");
        createParam.setUserInput("帮我生成今天店铺运营复盘");
        createParam.setDateRange(dateRange);

        AgentTaskContext context = new AgentTaskContext();
        context.setTenantId(1L);
        context.setShopId(1L);
        context.setUserId(1L);
        context.setTaskId(10001L);
        context.setTraceId("tr_planner");
        context.setCreateParam(createParam);
        return context;
    }

    private static class FakeModelGatewayService implements ModelGatewayService {
        private final String outputText;
        private ModelInvokeParam capturedParam;

        private FakeModelGatewayService(String outputText) {
            this.outputText = outputText;
        }

        @Override
        public ModelInvokeResult invoke(Long tenantId, Long shopId, Long userId, String username, ModelInvokeParam param) {
            this.capturedParam = param;
            ModelInvokeResult result = new ModelInvokeResult();
            result.setProviderCode("fake-planner");
            result.setModelName("fake-planner-001");
            result.setStatus(ModelCallStatus.SUCCESS);
            result.setOutputText(outputText);
            result.setCallId(99L);
            return result;
        }

        @Override
        public CommonPage<ModelCallLogDto> listLogs(Long tenantId, Long shopId, ModelCallLogQueryParam query) {
            return CommonPage.of(java.util.List.of(), 1, 10, 0L);
        }
    }
}
