package com.sirithree.shopops.admin.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.service.PlannerService;
import com.sirithree.shopops.admin.model.config.ModelGatewayPlannerProperties;
import com.sirithree.shopops.admin.model.domain.ModelCallStatus;
import com.sirithree.shopops.admin.model.domain.ModelInvokeParam;
import com.sirithree.shopops.admin.model.domain.ModelInvokeResult;
import com.sirithree.shopops.admin.model.service.ModelGatewayService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class RulePlannerService implements PlannerService {
    private static final List<String> DAILY_REVIEW_TOOL_CODES = List.of(
            "order.query_summary",
            "comment.query_negative",
            "product.query_candidates",
            "ad.query_performance",
            "report.generate_daily_review"
    );

    private final ModelGatewayPlannerProperties plannerProperties;
    private final ModelGatewayService modelGatewayService;
    private final ObjectMapper objectMapper;

    public RulePlannerService(ModelGatewayPlannerProperties plannerProperties,
                              ModelGatewayService modelGatewayService,
                              ObjectMapper objectMapper) {
        this.plannerProperties = plannerProperties;
        this.modelGatewayService = modelGatewayService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentPlan createPlan(AgentTaskContext context) {
        if (!"daily_review".equals(context.getCreateParam().getTaskType())) {
            throw new IllegalArgumentException("P0 仅支持 daily_review 任务");
        }
        if (plannerProperties.isEnabled()) {
            return modelPlanOrFallback(context);
        }
        return rulePlan();
    }

    private AgentPlan modelPlanOrFallback(AgentTaskContext context) {
        try {
            ModelInvokeParam param = new ModelInvokeParam();
            param.setProviderCode(plannerProperties.getProviderCode());
            param.setModelName(plannerProperties.getModelName());
            param.setPromptCode(plannerProperties.getPromptCode());
            param.setPromptVersion(plannerProperties.getPromptVersion());
            param.setTraceId(context.getTraceId());
            param.setTaskId(context.getTaskId());
            param.setTimeoutMs(plannerProperties.getTimeoutMs());
            param.setPrompt(buildPlannerPrompt(context));
            param.setMetadata(Map.of(
                    "taskType", context.getCreateParam().getTaskType(),
                    "userInput", context.getCreateParam().getUserInput(),
                    "dateRange", context.getCreateParam().getDateRange(),
                    "allowedToolCodes", DAILY_REVIEW_TOOL_CODES
            ));

            ModelInvokeResult result = modelGatewayService.invoke(
                    context.getTenantId(),
                    context.getShopId(),
                    context.getUserId(),
                    "agent-planner",
                    param
            );
            if (!ModelCallStatus.SUCCESS.equals(result.getStatus()) || result.getOutputText() == null || result.getOutputText().isBlank()) {
                return rulePlan();
            }
            AgentPlan plan = parsePlan(result.getOutputText());
            return isSafeDailyReviewPlan(plan) ? plan : rulePlan();
        } catch (RuntimeException ex) {
            return rulePlan();
        }
    }

    private String buildPlannerPrompt(AgentTaskContext context) {
        return """
                你是 ShopOps Agent 的任务规划器。请只输出 JSON，不要输出 Markdown 或解释。
                任务类型必须是 daily_review。只能使用以下工具，并保持顺序不变：
                1. order.query_summary
                2. comment.query_negative
                3. product.query_candidates
                4. ad.query_performance
                5. report.generate_daily_review

                输出格式：
                {"taskType":"daily_review","steps":[{"stepNo":1,"stepName":"查询订单核心指标","toolCode":"order.query_summary"}]}

                用户输入：%s
                日期范围：%s 至 %s
                """.formatted(
                context.getCreateParam().getUserInput(),
                context.getCreateParam().getDateRange().getStart(),
                context.getCreateParam().getDateRange().getEnd()
        );
    }

    private AgentPlan parsePlan(String outputText) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(outputText));
            AgentPlan plan = new AgentPlan();
            plan.setTaskType(text(root, "taskType"));
            List<AgentPlanStep> steps = new ArrayList<>();
            JsonNode stepNodes = root.get("steps");
            if (stepNodes != null && stepNodes.isArray()) {
                for (JsonNode stepNode : stepNodes) {
                    steps.add(new AgentPlanStep(
                            stepNode.path("stepNo").isInt() ? stepNode.path("stepNo").asInt() : null,
                            text(stepNode, "stepName"),
                            text(stepNode, "toolCode")
                    ));
                }
            }
            plan.setSteps(steps);
            return plan;
        } catch (Exception ex) {
            throw new IllegalArgumentException("模型规划结果不是合法 JSON", ex);
        }
    }

    private String extractJson(String outputText) {
        String text = outputText.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("模型规划结果缺少 JSON 对象");
        }
        return text.substring(start, end + 1);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private boolean isSafeDailyReviewPlan(AgentPlan plan) {
        if (plan == null || !"daily_review".equals(plan.getTaskType()) || plan.getSteps().size() != DAILY_REVIEW_TOOL_CODES.size()) {
            return false;
        }
        for (int i = 0; i < DAILY_REVIEW_TOOL_CODES.size(); i++) {
            AgentPlanStep step = plan.getSteps().get(i);
            if (!Integer.valueOf(i + 1).equals(step.getStepNo())) {
                return false;
            }
            if (step.getStepName() == null || step.getStepName().isBlank()) {
                return false;
            }
            if (!DAILY_REVIEW_TOOL_CODES.get(i).equals(step.getToolCode())) {
                return false;
            }
        }
        return true;
    }

    private AgentPlan rulePlan() {
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.getSteps().add(new AgentPlanStep(1, "查询订单核心指标", "order.query_summary"));
        plan.getSteps().add(new AgentPlanStep(2, "查询差评风险", "comment.query_negative"));
        plan.getSteps().add(new AgentPlanStep(3, "查询待优化商品", "product.query_candidates"));
        plan.getSteps().add(new AgentPlanStep(4, "查询广告投放指标", "ad.query_performance"));
        plan.getSteps().add(new AgentPlanStep(5, "生成经营复盘报告", "report.generate_daily_review"));
        return plan;
    }
}
