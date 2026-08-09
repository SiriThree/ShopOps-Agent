package com.sirithree.shopops.admin.benchmark.v1.evaluator;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.TaskCapabilityCatalog;
import com.sirithree.shopops.admin.benchmark.v1.evidence.CollectedEvidence;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates Tool legality/capability satisfaction without requiring one exact Tool trace.
 * Optional read calls and tolerated optional failures are diagnostic, not automatic task failures.
 */
public class ToolLegalityEvaluator implements BenchmarkEvaluator {
    @Override
    public EvaluationResult evaluate(BenchmarkCase benchmarkCase, CollectedEvidence evidence) {
        EvaluationResult result = new EvaluationResult();
        Set<String> acceptable = new HashSet<>(benchmarkCase.acceptableTools == null ? List.of() : benchmarkCase.acceptableTools);
        Set<String> forbidden = new HashSet<>(benchmarkCase.forbiddenTools == null ? List.of() : benchmarkCase.forbiddenTools);
        Set<String> successful = successfulTools(evidence);

        boolean valid = true;
        int redundantCalls = 0;
        int optionalFailures = 0;
        int validationFailures = 0;

        for (Map<String, Object> log : evidence.toolLogs) {
            String tool = string(log.get("toolCode"));
            if (tool == null) continue;
            boolean known = TaskCapabilityCatalog.KNOWN_TOOLS.contains(tool);
            boolean explicitlyForbidden = forbidden.contains(tool);
            boolean nonAcceptedWrite = !acceptable.isEmpty() && !acceptable.contains(tool) && isWriteTool(tool);
            if (!known || explicitlyForbidden || nonAcceptedWrite) {
                valid = false;
                result.fail(FailureReasonCode.FORBIDDEN_TOOL_USED);
            } else if (!acceptable.isEmpty() && !acceptable.contains(tool)) {
                redundantCalls++;
            }

            if (!trustedShopScopeRespected(benchmarkCase, log)) {
                valid = false;
                validationFailures++;
                result.fail(FailureReasonCode.INVALID_TOOL_ARGUMENT);
            }

            if ("FAILED".equalsIgnoreCase(string(log.get("status")))) {
                FailureReasonCode reason = FailureReasonMapper.fromToolLog(log);
                boolean hardFailure = reason == FailureReasonCode.INVALID_TOOL_ARGUMENT
                        || reason == FailureReasonCode.UNAUTHORIZED_EXECUTION
                        || reason == FailureReasonCode.APPROVAL_BYPASS
                        || isRequiredForUnsatisfiedCapability(tool, benchmarkCase, successful);
                if (hardFailure) {
                    valid = false;
                    result.fail(reason);
                    if (reason == FailureReasonCode.INVALID_TOOL_ARGUMENT) validationFailures++;
                } else {
                    optionalFailures++;
                }
            }
        }

        for (String capability : benchmarkCase.requiredCapabilities == null ? List.<String>of() : benchmarkCase.requiredCapabilities) {
            if (!TaskCapabilityCatalog.satisfied(capability, successful)) {
                valid = false;
                result.fail(FailureReasonCode.REQUIRED_CAPABILITY_MISSING);
            }
        }

        result.metric("toolExecutionValid", valid);
        result.metric("executedToolCodes", evidence.executedToolCodes());
        result.metric("successfulToolCodes", successful.stream().toList());
        result.metric("redundantToolCallCount", redundantCalls);
        result.metric("optionalToolFailureCount", optionalFailures);
        result.metric("toolValidationFailureCount", validationFailures);
        return result;
    }

    private Set<String> successfulTools(CollectedEvidence evidence) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> log : evidence.toolLogs) {
            if ("SUCCESS".equalsIgnoreCase(string(log.get("status"))) && log.get("toolCode") != null) {
                result.add(String.valueOf(log.get("toolCode")));
            }
        }
        return result;
    }

    private boolean isRequiredForUnsatisfiedCapability(String failedTool,
                                                        BenchmarkCase benchmarkCase,
                                                        Set<String> successfulTools) {
        for (String capability : benchmarkCase.requiredCapabilities == null ? List.<String>of() : benchmarkCase.requiredCapabilities) {
            Set<String> satisfying = TaskCapabilityCatalog.satisfyingTools(capability);
            if (satisfying.contains(failedTool) && satisfying.stream().noneMatch(successfulTools::contains)) return true;
        }
        return false;
    }

    private boolean trustedShopScopeRespected(BenchmarkCase benchmarkCase, Map<String, Object> log) {
        Object expectedValue = benchmarkCase.identity == null ? null : benchmarkCase.identity.get("shopId");
        if (expectedValue == null) return true;
        Object inputValue = map(log.get("input")).get("shopId");
        if (inputValue == null) return true;
        return string(expectedValue).equals(string(inputValue));
    }

    private boolean isWriteTool(String tool) {
        return Set.of("order.refund_execute", "product.update_title", "report.export_excel", "feishu.sync_report").contains(tool);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
