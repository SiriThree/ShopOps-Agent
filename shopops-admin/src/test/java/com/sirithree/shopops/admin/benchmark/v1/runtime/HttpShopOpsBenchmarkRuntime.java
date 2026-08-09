package com.sirithree.shopops.admin.benchmark.v1.runtime;

import com.sirithree.shopops.admin.benchmark.v1.evaluator.FailureReasonCode;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class HttpShopOpsBenchmarkRuntime implements BenchmarkRuntimeGateway {
    private final TestRestTemplate restTemplate;
    private final String baseUrl;

    public HttpShopOpsBenchmarkRuntime(TestRestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public BenchmarkRuntimeResult execute(BenchmarkRuntimeRequest request, BenchmarkRunRequest runRequest) {
        BenchmarkRuntimeResult result = new BenchmarkRuntimeResult();
        HttpHeaders headers = headers(request.identity);
        Map<String, Object> body = Map.of(
                "userInput", String.valueOf(request.input.getOrDefault("userInput", "")),
                "dateRange", request.input.getOrDefault("dateRange", Map.of())
        );

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    baseUrl + "/api/agent/tasks/natural-language",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
        } catch (RuntimeException ex) {
            result.executionStatus = CaseExecutionStatus.ERROR;
            result.infrastructureError = "Agent HTTP entry unavailable: " + safe(ex.getMessage());
            result.runtimeFailureReason = FailureReasonCode.INFRASTRUCTURE_ERROR;
            return result;
        }

        if (response.getStatusCode().value() == 401 || response.getStatusCode().value() == 403) {
            result.executionStatus = CaseExecutionStatus.FAILED;
            result.runtimeFailureReason = FailureReasonCode.UNAUTHORIZED_EXECUTION;
            result.infrastructureError = "Request rejected by HTTP authorization: " + response.getStatusCode().value();
            return result;
        }
        if (response.getStatusCode().is5xxServerError()) {
            result.executionStatus = CaseExecutionStatus.ERROR;
            result.runtimeFailureReason = FailureReasonCode.INFRASTRUCTURE_ERROR;
            result.infrastructureError = "Agent HTTP entry returned " + response.getStatusCode().value();
            return result;
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            result.executionStatus = CaseExecutionStatus.FAILED;
            result.runtimeFailureReason = FailureReasonCode.INTERPRETATION_ERROR;
            result.infrastructureError = "Agent request rejected: HTTP " + response.getStatusCode().value();
            return result;
        }

        Map<String, Object> envelope = response.getBody();
        if (!Integer.valueOf(200).equals(intValue(envelope.get("code")))) {
            result.executionStatus = CaseExecutionStatus.FAILED;
            result.runtimeFailureReason = FailureReasonCode.INTERPRETATION_ERROR;
            result.infrastructureError = "Agent request rejected by CommonResult: " + envelope.get("message");
            return result;
        }

        Map<String, Object> data = map(envelope.get("data"));
        result.observedInterpretation.put("intent", data.get("intent"));
        result.observedInterpretation.put("intentLabel", data.get("intentLabel"));
        result.observedInterpretation.put("confidence", data.get("confidence"));
        result.observedInterpretation.put("routedReason", data.get("routedReason"));
        if (data.get("plan") instanceof Map<?, ?> plan) result.endpointPlanPreview.putAll((Map<String, Object>) plan);

        Map<String, Object> task = map(data.get("task"));
        result.taskId = longValue(task.get("taskId"));
        result.traceId = string(task.get("traceId"));
        result.finalState = string(task.get("status"));
        if (result.taskId == null) {
            result.executionStatus = CaseExecutionStatus.ERROR;
            result.runtimeFailureReason = FailureReasonCode.EVALUATION_ERROR;
            result.infrastructureError = "Natural-language endpoint returned no taskId";
            return result;
        }

        Duration timeout = runRequest.completionTimeout == null ? Duration.ofSeconds(10) : runRequest.completionTimeout;
        Duration pollInterval = runRequest.pollInterval == null ? Duration.ofMillis(50) : runRequest.pollInterval;
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!isTerminal(result.finalState) && System.nanoTime() < deadline) {
            LockSupport.parkNanos(Math.max(Duration.ofMillis(5).toNanos(), pollInterval.toNanos()));
            Map<String, Object> taskEnvelope = getTask(result.taskId, headers);
            if (taskEnvelope == null) {
                result.executionStatus = CaseExecutionStatus.ERROR;
                result.runtimeFailureReason = FailureReasonCode.INFRASTRUCTURE_ERROR;
                result.infrastructureError = "Task polling failed for taskId=" + result.taskId;
                return result;
            }
            Map<String, Object> taskData = map(taskEnvelope.get("data"));
            result.finalState = string(taskData.get("status"));
            if (result.traceId == null) result.traceId = string(taskData.get("traceId"));
        }

        if (!isTerminal(result.finalState)) {
            result.executionStatus = CaseExecutionStatus.EXECUTED;
            result.runtimeFailureReason = FailureReasonCode.STATE_NOT_CONVERGED;
            return result;
        }
        result.executionStatus = CaseExecutionStatus.EXECUTED;
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getTask(Long taskId, HttpHeaders headers) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    baseUrl + "/api/agent/tasks/" + taskId,
                    HttpMethod.GET,
                    new HttpEntity<>(null, headers),
                    Map.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) return null;
            return response.getBody();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private HttpHeaders headers(Map<String, Object> identity) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-Id", String.valueOf(identity.getOrDefault("tenantId", 1)));
        headers.set("X-Shop-Id", String.valueOf(identity.getOrDefault("shopId", 1)));
        headers.set("X-User-Id", String.valueOf(identity.getOrDefault("userId", 1)));
        Object roles = identity.get("roles");
        if (roles instanceof Collection<?> collection && !collection.isEmpty()) {
            headers.set("X-User-Roles", String.join(",", collection.stream().map(String::valueOf).toList()));
        }
        return headers;
    }

    private boolean isTerminal(String state) {
        if (state == null) return false;
        String normalized = state.toUpperCase(Locale.ROOT);
        return List.of("SUCCESS", "SUCCEEDED", "FAILED", "DEGRADED", "NEEDS_MANUAL_ACTION", "CANCELLED").contains(normalized);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }

    private Integer intValue(Object value) {
        if (value instanceof Number number) return number.intValue();
        if (value == null) return null;
        try { return Integer.parseInt(String.valueOf(value)); } catch (NumberFormatException ex) { return null; }
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String safe(String value) {
        if (value == null) return "unavailable";
        return value.length() <= 300 ? value : value.substring(0, 300);
    }
}
