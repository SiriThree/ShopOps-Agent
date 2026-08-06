package com.sirithree.shopops.admin.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sirithree.shopops.admin.agent.domain.AgentPlan;
import com.sirithree.shopops.admin.agent.domain.AgentPlanStep;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskCreateParam;
import com.sirithree.shopops.admin.agent.domain.AgentTaskSpec;
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
            "report.query_external_metrics",
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
        String intent = resolveIntent(context);
        if (hasText(intent) && !"daily_review".equals(intent)) {
            return rulePlan(intent);
        }
        if (plannerProperties.isEnabled()) {
            return modelPlanOrFallback(context);
        }
        return rulePlan("daily_review");
    }

    @Override
    public AgentPlan previewPlan(AgentTaskSpec taskSpec) {
        return rulePlan(taskSpec == null ? "daily_review" : taskSpec.getIntent());
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
                    "allowedToolCodes", DAILY_REVIEW_TOOL_CODES,
                    "taskSpec", context.getCreateParam().getTaskSpec() == null ? Map.of() : context.getCreateParam().getTaskSpec()
            ));

            ModelInvokeResult result = modelGatewayService.invoke(
                    context.getTenantId(),
                    context.getShopId(),
                    context.getUserId(),
                    "agent-planner",
                    param
            );
            if (!ModelCallStatus.SUCCESS.equals(result.getStatus()) || result.getOutputText() == null || result.getOutputText().isBlank()) {
                return rulePlan("daily_review");
            }
            AgentPlan plan = parsePlan(result.getOutputText());
            return isSafeDailyReviewPlan(plan) ? plan : rulePlan("daily_review");
        } catch (RuntimeException ex) {
            return rulePlan("daily_review");
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
                5. report.query_external_metrics
                6. report.generate_daily_review

                输出格式：
                {"taskType":"daily_review","rationale":"说明整体规划依据","steps":[{"stepNo":1,"stepName":"查询订单核心指标","toolCode":"order.query_summary","reason":"说明选择该工具的原因"}]}

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
            plan.setRationale(text(root, "rationale"));
            List<AgentPlanStep> steps = new ArrayList<>();
            JsonNode stepNodes = root.get("steps");
            if (stepNodes != null && stepNodes.isArray()) {
                for (JsonNode stepNode : stepNodes) {
                    steps.add(new AgentPlanStep(
                            stepNode.path("stepNo").isInt() ? stepNode.path("stepNo").asInt() : null,
                            text(stepNode, "stepName"),
                            text(stepNode, "toolCode"),
                            text(stepNode, "reason")
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

    static List<AgentPlanStep> ruleSteps(String intent) {
        if ("comment_risk".equals(intent)) {
            return List.of(
                    new AgentPlanStep(1, "分析订单基线", "order.query_summary", "建立差评数量与整体交易规模的对照基线"),
                    new AgentPlanStep(2, "聚类差评风险", "comment.query_negative", "提取低星评价、风险原因和类目分布"),
                    new AgentPlanStep(3, "检查受影响商品", "product.query_candidates", "关联需要优先处理的商品候选"),
                    new AgentPlanStep(4, "生成差评风险报告", "report.generate_daily_review", "汇总证据并生成可追溯专项报告")
            );
        }
        if ("product_optimization".equals(intent)) {
            return List.of(
                    new AgentPlanStep(1, "分析订单基线", "order.query_summary", "建立商品表现判断所需的经营基线"),
                    new AgentPlanStep(2, "识别低点击商品", "product.query_candidates", "筛选低点击或高风险商品候选"),
                    new AgentPlanStep(3, "检查关联评价信号", "comment.query_negative", "避免只依据点击指标给出片面建议"),
                    new AgentPlanStep(4, "生成商品优化报告", "report.generate_daily_review", "生成商品级优化清单和证据")
            );
        }
        if ("ad_anomaly".equals(intent)) {
            return List.of(
                    new AgentPlanStep(1, "分析订单基线", "order.query_summary", "建立转化与收入对照基线"),
                    new AgentPlanStep(2, "检查投放异常", "ad.query_performance", "识别高消耗低转化计划"),
                    new AgentPlanStep(3, "对比平台外部指标", "report.query_external_metrics", "排除整体流量波动带来的误判"),
                    new AgentPlanStep(4, "生成投放异常报告", "report.generate_daily_review", "输出预算调整建议及依据")
            );
        }
        return List.of(
                new AgentPlanStep(1, "查询订单核心指标", "order.query_summary", "获取 GMV、订单和售后基线"),
                new AgentPlanStep(2, "查询差评风险", "comment.query_negative", "识别评价异常和主要风险原因"),
                new AgentPlanStep(3, "查询待优化商品", "product.query_candidates", "定位商品层面的优化候选"),
                new AgentPlanStep(4, "查询广告投放指标", "ad.query_performance", "检查投放消耗和转化效率"),
                new AgentPlanStep(5, "查询外部报表指标", "report.query_external_metrics", "补充平台环境与整体趋势"),
                new AgentPlanStep(6, "生成经营复盘报告", "report.generate_daily_review", "融合全部证据生成结构化结论")
        );
    }

    static List<AgentPlanStep> ruleSteps(AgentTaskCreateParam param) {
        if (param != null && param.getTaskSpec() != null && hasTextValue(param.getTaskSpec().getIntent())) {
            return ruleSteps(param.getTaskSpec().getIntent());
        }
        return ruleSteps(param == null ? null : param.getIntent());
    }

    static String taskResultSummary(String intent, boolean degraded) {
        String suffix = degraded ? " with degraded evidence" : "";
        if ("comment_risk".equals(intent)) {
            return "Comment risk analysis report generated" + suffix;
        }
        if ("product_optimization".equals(intent)) {
            return "Product optimization report generated" + suffix;
        }
        if ("ad_anomaly".equals(intent)) {
            return "Ad anomaly report generated" + suffix;
        }
        return "Daily review report generated" + suffix;
    }

    private AgentPlan rulePlan(String intent) {
        AgentPlan plan = new AgentPlan();
        plan.setTaskType("daily_review");
        plan.setRationale(planRationale(intent));
        plan.getSteps().addAll(ruleSteps(intent));
        return plan;
    }

    private String resolveIntent(AgentTaskContext context) {
        AgentTaskSpec taskSpec = context.getCreateParam().getTaskSpec();
        if (taskSpec != null && hasText(taskSpec.getIntent())) {
            return taskSpec.getIntent();
        }
        return context.getCreateParam().getIntent();
    }

    private String planRationale(String intent) {
        return switch (intent) {
            case "comment_risk" -> "围绕差评原因与受影响商品选择必要证据，跳过无关投放步骤。";
            case "product_optimization" -> "围绕商品候选与评价信号规划，避免执行无关投放查询。";
            case "ad_anomaly" -> "围绕投放效率与外部环境规划，跳过无关评价和商品步骤。";
            default -> "经营日报需要覆盖订单、评价、商品、投放和外部环境的完整证据。";
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasTextValue(String value) {
        return value != null && !value.isBlank();
    }
}
