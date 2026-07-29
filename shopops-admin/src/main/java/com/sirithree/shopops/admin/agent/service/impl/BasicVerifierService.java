package com.sirithree.shopops.admin.agent.service.impl;

import com.sirithree.shopops.admin.agent.domain.AgentExecutionResult;
import com.sirithree.shopops.admin.agent.domain.AgentTaskContext;
import com.sirithree.shopops.admin.agent.domain.AgentTaskSpec;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationCheck;
import com.sirithree.shopops.admin.agent.domain.AgentVerificationResult;
import com.sirithree.shopops.admin.agent.service.VerifierService;
import com.sirithree.shopops.admin.tool.domain.ToolInvokeResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class BasicVerifierService implements VerifierService {
    private static final Map<String, String> EVIDENCE_TOOL_CODES = Map.of(
            "order_summary", "order.query_summary",
            "negative_comments", "comment.query_negative",
            "product_candidates", "product.query_candidates",
            "ad_performance", "ad.query_performance",
            "external_metrics", "report.query_external_metrics"
    );

    @Override
    public AgentVerificationResult verify(AgentTaskContext context, AgentExecutionResult result) {
        List<AgentVerificationCheck> checks = new ArrayList<>();
        List<String> missingEvidence = new ArrayList<>();
        List<String> repairToolCodes = new ArrayList<>();
        checks.add(check(
                "execution_success",
                Boolean.TRUE.equals(result.getSuccess()),
                "Agent 执行链路成功",
                result.getErrorMessage() == null ? "Agent 执行链路失败" : result.getErrorMessage()
        ));
        checks.add(check(
                "report_generated",
                result.getReportId() != null,
                "已生成可追踪报告 reportId=" + result.getReportId(),
                "报告未生成"
        ));

        AgentTaskSpec taskSpec = context.getCreateParam().getTaskSpec();
        boolean allowDegraded = allowsDegradedEvidence(taskSpec);
        for (String evidence : requiredEvidence(context, taskSpec)) {
            String toolCode = EVIDENCE_TOOL_CODES.get(evidence);
            if (toolCode == null) {
                checks.add(new AgentVerificationCheck("evidence_" + evidence, "FAIL", "未知证据类型: " + evidence));
                continue;
            }
            ToolInvokeResult toolResult = result.getStepResults().get(toolCode);
            if (toolResult == null || !Boolean.TRUE.equals(toolResult.getSuccess())) {
                missingEvidence.add(evidence);
                addRepairToolCode(repairToolCodes, toolCode);
                checks.add(new AgentVerificationCheck(
                        "evidence_" + evidence,
                        allowDegraded ? "WARN" : "FAIL",
                        "缺少证据 " + evidence + "，对应工具 " + toolCode + " 未成功"
                ));
                continue;
            }
            if (toolResult.getData() == null) {
                missingEvidence.add(evidence);
                addRepairToolCode(repairToolCodes, toolCode);
            }
            checks.add(new AgentVerificationCheck(
                    "evidence_" + evidence,
                    toolResult.getData() == null ? (allowDegraded ? "WARN" : "FAIL") : "PASS",
                    toolResult.getData() == null
                            ? "工具 " + toolCode + " 未返回证据数据"
                            : "证据 " + evidence + " 已由 " + toolCode + " 提供"
            ));
        }

        AgentVerificationResult verification = new AgentVerificationResult();
        verification.setChecks(checks);
        verification.setPassed(checks.stream().noneMatch(item -> "FAIL".equals(item.getStatus())));
        verification.setMissingEvidence(missingEvidence);
        verification.setRepairToolCodes(repairToolCodes);
        verification.setRepairable(!repairToolCodes.isEmpty());
        double earned = checks.stream().mapToDouble(item -> switch (item.getStatus()) {
            case "PASS" -> 1.0;
            case "WARN" -> 0.5;
            default -> 0.0;
        }).sum();
        verification.setScore(checks.isEmpty() ? 0.0 : Math.round(earned * 10000.0 / checks.size()) / 10000.0);
        return verification;
    }

    private AgentVerificationCheck check(String code, boolean passed, String successMessage, String failureMessage) {
        return new AgentVerificationCheck(code, passed ? "PASS" : "FAIL", passed ? successMessage : failureMessage);
    }

    private void addRepairToolCode(List<String> toolCodes, String toolCode) {
        if (!toolCodes.contains(toolCode)) {
            toolCodes.add(toolCode);
        }
    }

    private boolean allowsDegradedEvidence(AgentTaskSpec taskSpec) {
        if (taskSpec == null || taskSpec.getConstraints() == null) {
            return true;
        }
        return !Boolean.FALSE.equals(taskSpec.getConstraints().get("allowDegradedEvidence"));
    }

    private List<String> requiredEvidence(AgentTaskContext context, AgentTaskSpec taskSpec) {
        if (taskSpec != null && taskSpec.getRequiredEvidence() != null && !taskSpec.getRequiredEvidence().isEmpty()) {
            return taskSpec.getRequiredEvidence();
        }
        return switch (context.getCreateParam().getIntent() == null ? "daily_review" : context.getCreateParam().getIntent()) {
            case "comment_risk" -> List.of("order_summary", "negative_comments", "product_candidates");
            case "product_optimization" -> List.of("order_summary", "product_candidates", "negative_comments");
            case "ad_anomaly" -> List.of("order_summary", "ad_performance", "external_metrics");
            default -> List.of("order_summary", "negative_comments", "product_candidates", "ad_performance", "external_metrics");
        };
    }
}
