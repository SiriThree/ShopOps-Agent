package com.sirithree.shopops.admin.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

abstract class AbstractAgentEvaluationIntegrationTestSupport extends AbstractAgentTaskFlowIntegrationTest {
    private static final DateTimeFormatter OUTPUT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldRunAgentEvaluationCasesAndWriteSummaryArtifacts() throws Exception {
        List<EvalCase> cases = loadCases();
        assertThat(cases).isNotEmpty();

        List<CaseResult> results = new ArrayList<>();
        for (EvalCase evalCase : cases) {
            applyShopConfigs(evalCase);
            results.add(runCase(evalCase));
        }

        Summary summary = buildSummary(results);
        writeArtifacts(summary);

        assertThat(summary.caseCount).isEqualTo(cases.size());
        assertThat(summary.passedCaseCount).isEqualTo(cases.size());
    }

    protected abstract String caseResource();

    protected abstract String summaryPrefix();

    private List<EvalCase> loadCases() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(caseResource())) {
            if (inputStream == null) {
                throw new IllegalStateException("Missing evaluation cases resource: " + caseResource());
            }
            return objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        }
    }

    private void applyShopConfigs(EvalCase evalCase) {
        for (ShopConfigInput config : evalCase.shopConfigs) {
            Map<String, Object> saved = dataOf(post(
                    "/api/admin/organization/shops/1/configs",
                    Map.of(
                            "configKey", config.configKey,
                            "configValue", config.configValue,
                            "valueType", config.valueType
                    ),
                    headers(evalCase.headers)
            ));
            assertThat(saved.get("configKey")).isEqualTo(config.configKey);
            assertThat(saved.get("configValue")).isEqualTo(config.configValue);
        }
    }

    private CaseResult runCase(EvalCase evalCase) {
        long startedAt = System.nanoTime();
        CaseResult result = new CaseResult();
        result.caseId = evalCase.caseId;
        result.scenario = evalCase.scenario;
        result.description = evalCase.description;

        if ("daily_review_task".equals(evalCase.scenario)) {
            runDailyReviewCase(evalCase, result);
        } else if ("manual_tool_invoke".equals(evalCase.scenario)) {
            runManualToolCase(evalCase, result);
        } else {
            throw new IllegalArgumentException("Unsupported evaluation scenario: " + evalCase.scenario);
        }

        result.durationMs = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        result.passed = result.mismatches.isEmpty();
        return result;
    }

    @SuppressWarnings("unchecked")
    private void runDailyReviewCase(EvalCase evalCase, CaseResult result) {
        Map<String, Object> created = dataOf(post("/api/agent/tasks", objectMapper.convertValue(evalCase.taskCreateParam, Map.class), headers(evalCase.headers)));
        result.actualStatus = stringValue(created.get("status"));
        result.actualApprovalCreated = false;

        Long taskId = longValue(created.get("taskId"));
        result.taskId = taskId;

        Map<String, Object> task = dataOf(get("/api/agent/tasks/" + taskId, headers(evalCase.headers)));
        result.actualStatus = stringValue(task.get("status"));
        result.actualDegraded = "DEGRADED".equalsIgnoreCase(result.actualStatus);

        List<Map<String, Object>> steps = (List<Map<String, Object>>) dataOfObject(get("/api/agent/tasks/" + taskId + "/steps", headers(evalCase.headers)));
        result.actualToolCodes = steps.stream().map(step -> stringValue(step.get("toolCode"))).toList();
        result.toolInvocationCount = steps.size();
        result.toolSuccessCount = (int) steps.stream()
                .filter(step -> "SUCCESS".equalsIgnoreCase(stringValue(step.get("status"))))
                .count();

        Long reportId = longValue(task.get("reportId"));
        result.reportId = reportId;
        if (reportId != null) {
            Map<String, Object> report = dataOf(get("/api/reports/" + reportId, headers(evalCase.headers)));
            result.reportMarkdown = stringValue(report.get("markdown"));
            Map<String, Object> evidence = mapValue(report.get("evidence"));
            result.evidenceKeys = new ArrayList<>(evidence.keySet());
            result.actualEvidence = evidence;
            Map<String, Object> shopConfig = castNestedMap(evidence.get("shopConfig"));
            result.actualConfigSnapshot = shopConfig;
        }

        applyExpectations(evalCase.expectations, result);
    }

    @SuppressWarnings("unchecked")
    private void runManualToolCase(EvalCase evalCase, CaseResult result) {
        Map<String, Object> invokeResult = dataOf(post(
                "/api/tools/" + evalCase.manualToolCode + "/invoke",
                evalCase.manualToolInput,
                headers(evalCase.headers)
        ));
        result.actualStatus = stringValue(invokeResult.get("status"));
        result.actualDegraded = false;
        result.actualApprovalCreated = invokeResult.get("approvalId") != null;
        result.approvalId = longValue(invokeResult.get("approvalId"));
        result.toolInvocationCount = 1;
        result.toolSuccessCount = Boolean.TRUE.equals(invokeResult.get("success")) ? 1 : 0;
        result.actualToolCodes = List.of(evalCase.manualToolCode);

        Long toolCallLogId = longValue(invokeResult.get("toolCallLogId"));
        result.toolCallLogId = toolCallLogId;
        if (toolCallLogId != null) {
            Map<String, Object> logPage = dataOf(get("/api/tools/call-logs?logId=" + toolCallLogId, headers(evalCase.headers)));
            List<Map<String, Object>> list = (List<Map<String, Object>>) logPage.get("list");
            if (!list.isEmpty()) {
                Map<String, Object> log = list.get(0);
                result.actualLogErrorCode = stringValue(log.get("errorCode"));
            }
        }

        applyExpectations(evalCase.expectations, result);
    }

    private void applyExpectations(Expectations expectations, CaseResult result) {
        if (expectations.finalStatusIn != null && !expectations.finalStatusIn.isEmpty()
                && expectations.finalStatusIn.stream().noneMatch(status -> status.equalsIgnoreCase(result.actualStatus))) {
            result.mismatches.add("Unexpected status: " + result.actualStatus + ", expected one of " + expectations.finalStatusIn);
        }
        result.expectApprovalCreated = expectations.expectApprovalCreated;
        if (expectations.expectedToolCodes != null && !expectations.expectedToolCodes.isEmpty()
                && !Objects.equals(expectations.expectedToolCodes, result.actualToolCodes)) {
            result.mismatches.add("Unexpected tool codes: " + result.actualToolCodes + ", expected " + expectations.expectedToolCodes);
        }
        if (expectations.expectApprovalCreated != null
                && expectations.expectApprovalCreated.booleanValue() != result.actualApprovalCreated) {
            result.mismatches.add("Approval creation mismatch: actual=" + result.actualApprovalCreated
                    + ", expected=" + expectations.expectApprovalCreated);
        }
        if (expectations.expectDegraded != null
                && expectations.expectDegraded.booleanValue() != result.actualDegraded) {
            result.mismatches.add("Degraded flag mismatch: actual=" + result.actualDegraded
                    + ", expected=" + expectations.expectDegraded);
        }
        if (expectations.expectEvidenceKeys != null) {
            for (String key : expectations.expectEvidenceKeys) {
                if (!result.evidenceKeys.contains(key)) {
                    result.mismatches.add("Missing evidence key: " + key);
                }
            }
        }
        if (expectations.expectedConfigEntries != null && !expectations.expectedConfigEntries.isEmpty()) {
            boolean matched = true;
            for (Map.Entry<String, String> entry : expectations.expectedConfigEntries.entrySet()) {
                String actual = stringValue(result.actualConfigSnapshot.get(entry.getKey()));
                if (!Objects.equals(entry.getValue(), actual)) {
                    matched = false;
                    result.mismatches.add("Config snapshot mismatch for " + entry.getKey()
                            + ": actual=" + actual + ", expected=" + entry.getValue());
                }
            }
            result.configMatched = matched;
        } else {
            result.configMatched = true;
        }
        if (expectations.expectedEvidenceEntries != null) {
            for (Map.Entry<String, String> entry : expectations.expectedEvidenceEntries.entrySet()) {
                String actual = stringValue(result.actualEvidence.get(entry.getKey()));
                if (!Objects.equals(entry.getValue(), actual)) {
                    result.mismatches.add("Evidence mismatch for " + entry.getKey()
                            + ": actual=" + actual + ", expected=" + entry.getValue());
                }
            }
        }
        if (expectations.expectedLogErrorCode != null
                && !Objects.equals(expectations.expectedLogErrorCode, result.actualLogErrorCode)) {
            result.mismatches.add("Unexpected log error code: " + result.actualLogErrorCode
                    + ", expected=" + expectations.expectedLogErrorCode);
        }
        if (expectations.expectedReportKeywords != null) {
            for (String keyword : expectations.expectedReportKeywords) {
                if (result.reportMarkdown == null || !result.reportMarkdown.contains(keyword)) {
                    result.mismatches.add("Report markdown missing keyword: " + keyword);
                }
            }
        }
    }

    private Summary buildSummary(List<CaseResult> results) {
        Summary summary = new Summary();
        summary.generatedAt = LocalDateTime.now().format(OUTPUT_TIME);
        summary.caseCount = results.size();
        summary.results = results;
        summary.passedCaseCount = (int) results.stream().filter(result -> result.passed).count();
        summary.completionRate = percentage(results.stream()
                .filter(result -> "SUCCESS".equalsIgnoreCase(result.actualStatus)
                        || "DEGRADED".equalsIgnoreCase(result.actualStatus)
                        || "APPROVAL_REQUIRED".equalsIgnoreCase(result.actualStatus))
                .count(), results.size());
        summary.successRate = percentage(results.stream()
                .filter(result -> "SUCCESS".equalsIgnoreCase(result.actualStatus))
                .count(), results.size());
        summary.degradedCompletionRate = percentage(results.stream().filter(result -> result.actualDegraded).count(), results.size());
        summary.avgTaskDurationMs = results.stream().mapToLong(result -> result.durationMs).average().orElse(0);
        int totalToolInvocations = results.stream().mapToInt(result -> result.toolInvocationCount).sum();
        int totalToolSuccess = results.stream().mapToInt(result -> result.toolSuccessCount).sum();
        summary.toolInvocationSuccessRate = percentage(totalToolSuccess, totalToolInvocations);
        List<CaseResult> approvalCases = results.stream().filter(result -> result.expectApprovalCreated != null).toList();
        summary.approvalDecisionAccuracy = percentage(
                approvalCases.stream().filter(result -> result.expectApprovalCreated.booleanValue() == result.actualApprovalCreated).count(),
                approvalCases.size()
        );
        List<CaseResult> configCases = results.stream()
                .filter(result -> !result.actualConfigSnapshot.isEmpty() || result.actualLogErrorCode != null)
                .toList();
        summary.configEffectAccuracy = percentage(
                configCases.stream().filter(result -> result.configMatched).count(),
                configCases.size()
        );
        for (CaseResult result : results) {
            summary.statusBreakdown.merge(result.actualStatus, 1L, Long::sum);
        }
        return summary;
    }

    private void writeArtifacts(Summary summary) throws IOException {
        Path outputDir = Path.of("target", "evaluation");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve(summaryPrefix() + "-summary.json"),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toCompactSummary(summary)),
                StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve(summaryPrefix() + "-summary.md"), toMarkdown(summary), StandardCharsets.UTF_8);
    }

    private Map<String, Object> toCompactSummary(Summary summary) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("generatedAt", summary.generatedAt);
        compact.put("caseCount", summary.caseCount);
        compact.put("passedCaseCount", summary.passedCaseCount);
        compact.put("completionRate", summary.completionRate);
        compact.put("successRate", summary.successRate);
        compact.put("degradedCompletionRate", summary.degradedCompletionRate);
        compact.put("avgTaskDurationMs", summary.avgTaskDurationMs);
        compact.put("toolInvocationSuccessRate", summary.toolInvocationSuccessRate);
        compact.put("approvalDecisionAccuracy", summary.approvalDecisionAccuracy);
        compact.put("configEffectAccuracy", summary.configEffectAccuracy);
        compact.put("statusBreakdown", summary.statusBreakdown);
        compact.put("results", summary.results.stream().map(this::toCompactResult).toList());
        return compact;
    }

    private Map<String, Object> toCompactResult(CaseResult result) {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("caseId", result.caseId);
        compact.put("scenario", result.scenario);
        compact.put("actualStatus", result.actualStatus);
        compact.put("actualDegraded", result.actualDegraded);
        compact.put("expectApprovalCreated", result.expectApprovalCreated);
        compact.put("actualApprovalCreated", result.actualApprovalCreated);
        compact.put("configMatched", result.configMatched);
        compact.put("passed", result.passed);
        compact.put("durationMs", result.durationMs);
        compact.put("taskId", result.taskId);
        compact.put("reportId", result.reportId);
        compact.put("approvalId", result.approvalId);
        compact.put("toolCallLogId", result.toolCallLogId);
        compact.put("toolInvocationCount", result.toolInvocationCount);
        compact.put("toolSuccessCount", result.toolSuccessCount);
        compact.put("actualToolCodes", result.actualToolCodes);
        compact.put("evidenceKeys", result.evidenceKeys);
        compact.put("actualConfigSnapshot", result.actualConfigSnapshot);
        compact.put("actualLogErrorCode", result.actualLogErrorCode);
        compact.put("mismatches", result.mismatches);
        return compact;
    }

    private String toMarkdown(Summary summary) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ShopOps Agent Evaluation Summary\n\n");
        builder.append("- Generated at: ").append(summary.generatedAt).append('\n');
        builder.append("- Case count: ").append(summary.caseCount).append('\n');
        builder.append("- Passed case count: ").append(summary.passedCaseCount).append('\n');
        builder.append("- Completion rate: ").append(formatPercentage(summary.completionRate)).append('\n');
        builder.append("- Success rate: ").append(formatPercentage(summary.successRate)).append('\n');
        builder.append("- Degraded completion rate: ").append(formatPercentage(summary.degradedCompletionRate)).append('\n');
        builder.append("- Avg task duration: ").append(String.format(Locale.ROOT, "%.1f ms", summary.avgTaskDurationMs)).append('\n');
        builder.append("- Tool invocation success rate: ").append(formatPercentage(summary.toolInvocationSuccessRate)).append('\n');
        builder.append("- Approval accuracy: ").append(formatPercentage(summary.approvalDecisionAccuracy)).append('\n');
        builder.append("- Config effect accuracy: ").append(formatPercentage(summary.configEffectAccuracy)).append('\n');
        builder.append("- Status breakdown: ").append(summary.statusBreakdown).append('\n');
        builder.append("\n## Case Results\n\n");
        builder.append("| caseId | scenario | status | passed | approval | configMatched | durationMs |\n");
        builder.append("|---|---|---|---:|---:|---:|---:|\n");
        for (CaseResult result : summary.results) {
            builder.append('|').append(result.caseId)
                    .append('|').append(result.scenario)
                    .append('|').append(result.actualStatus)
                    .append('|').append(result.passed)
                    .append('|').append(result.actualApprovalCreated)
                    .append('|').append(result.configMatched)
                    .append('|').append(result.durationMs)
                    .append("|\n");
        }
        builder.append("\n## Mismatches\n\n");
        boolean hasMismatch = false;
        for (CaseResult result : summary.results) {
            if (result.mismatches.isEmpty()) {
                continue;
            }
            hasMismatch = true;
            builder.append("- ").append(result.caseId).append(": ").append(String.join("; ", result.mismatches)).append('\n');
        }
        if (!hasMismatch) {
            builder.append("- None\n");
        }
        return builder.toString();
    }

    private double percentage(long numerator, long denominator) {
        return denominator <= 0 ? 0 : numerator * 100.0 / denominator;
    }

    private String formatPercentage(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    private Map<String, Object> get(String path, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private Map<String, Object> post(String path, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<Map> response = restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return response.getBody();
    }

    private HttpHeaders headers(Map<String, String> values) {
        HttpHeaders headers = new HttpHeaders();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            headers.set(entry.getKey(), entry.getValue());
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castNestedMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return objectMapper.convertValue(value, new TypeReference<>() {
        });
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    static class EvalCase {
        public String caseId;
        public String scenario;
        public String description;
        public Map<String, String> headers = Map.of();
        public List<ShopConfigInput> shopConfigs = List.of();
        public Map<String, Object> taskCreateParam;
        public String manualToolCode;
        public Map<String, Object> manualToolInput;
        public Expectations expectations;
    }

    static class ShopConfigInput {
        public String configKey;
        public String configValue;
        public String valueType;
    }

    static class Expectations {
        public List<String> finalStatusIn = List.of();
        public List<String> expectedToolCodes = List.of();
        public Boolean expectApprovalCreated;
        public Boolean expectDegraded;
        public List<String> expectEvidenceKeys = List.of();
        public Map<String, String> expectedConfigEntries = Map.of();
        public Map<String, String> expectedEvidenceEntries = Map.of();
        public List<String> expectedReportKeywords = List.of();
        public String expectedLogErrorCode;
    }

    static class CaseResult {
        public String caseId;
        public String scenario;
        public String description;
        public String actualStatus;
        public boolean actualDegraded;
        public Boolean expectApprovalCreated;
        public boolean actualApprovalCreated;
        public boolean configMatched = true;
        public boolean passed;
        public long durationMs;
        public Long taskId;
        public Long reportId;
        public Long approvalId;
        public Long toolCallLogId;
        public int toolInvocationCount;
        public int toolSuccessCount;
        public List<String> actualToolCodes = List.of();
        public List<String> evidenceKeys = List.of();
        public Map<String, Object> actualEvidence = new LinkedHashMap<>();
        public Map<String, Object> actualConfigSnapshot = new LinkedHashMap<>();
        public String reportMarkdown;
        public String actualLogErrorCode;
        public List<String> mismatches = new ArrayList<>();
    }

    static class Summary {
        public String generatedAt;
        public int caseCount;
        public int passedCaseCount;
        public double completionRate;
        public double successRate;
        public double degradedCompletionRate;
        public double avgTaskDurationMs;
        public double toolInvocationSuccessRate;
        public double approvalDecisionAccuracy;
        public double configEffectAccuracy;
        public Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        public List<CaseResult> results = List.of();
    }
}
