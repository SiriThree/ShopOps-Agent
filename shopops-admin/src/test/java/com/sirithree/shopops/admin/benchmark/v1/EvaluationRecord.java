package com.sirithree.shopops.admin.benchmark.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.sirithree.shopops.admin.benchmark.v1.evidence.EvidenceRef;
import com.sirithree.shopops.admin.benchmark.v1.runtime.CaseExecutionStatus;

/** Auditable observation schema populated by the Phase 1 ShopOpsBench runtime. */
public class EvaluationRecord {
    public String caseId;
    public String scenario;
    public String difficulty;
    public List<String> tags = new ArrayList<>();
    public String semanticTaskId;
    public String origin;
    public CaseExecutionStatus executionStatus;
    public String evaluationRunId;
    public Long taskId;
    public Map<String, Object> input = new LinkedHashMap<>();
    public Map<String, Object> observedIntent = new LinkedHashMap<>();
    public Map<String, Object> observedPlan = new LinkedHashMap<>();
    public Map<String, Object> runtimeMetadata = new LinkedHashMap<>();
    public Map<String, Object> observedFacts = new LinkedHashMap<>();
    public List<Map<String, Object>> agentSteps = new ArrayList<>();
    public List<Map<String, Object>> toolAttempts = new ArrayList<>();
    public List<Map<String, Object>> toolResults = new ArrayList<>();
    public List<Map<String, Object>> approvalEvents = new ArrayList<>();
    public List<Map<String, Object>> writeOperations = new ArrayList<>();
    public List<Map<String, Object>> sideEffects = new ArrayList<>();
    public List<Map<String, Object>> stateTransitions = new ArrayList<>();
    public List<Map<String, Object>> faultEvents = new ArrayList<>();
    public String finalState;
    public String governanceDecision;
    public Map<String, Object> authorizationSnapshot = new LinkedHashMap<>();
    public Map<String, Object> businessOutcome = new LinkedHashMap<>();
    public MetricBreakdown metricBreakdown = new MetricBreakdown();
    public List<String> failureReasons = new ArrayList<>();
    public List<EvidenceRef> evidenceRefs = new ArrayList<>();

    public static class MetricBreakdown {
        public Boolean businessOutcomeCorrect;
        public Boolean toolExecutionValid;
        public Boolean governanceSatisfied;
        public Boolean noUnexpectedSideEffect;
        public Boolean finalStateCorrect;
        public Boolean taskSuccess;
        public Integer redundantToolCallCount;
        public Integer optionalToolFailureCount;
        public Integer toolValidationFailureCount;
        public Boolean incorrectSuccess;
        public Boolean plannerFallback;
        public Integer expectedLogicalSideEffects;
        public Integer actualEffectiveSideEffects;
        public Integer duplicateSideEffects;
        public Integer missingSideEffects;
        public Integer logicalWriteRequests;
        public Integer deliveryAttempts;
        public Integer executionAttempts;
        public Integer externalAttempts;
        public Integer intendedReplayAttempts;
        public Integer idempotencyBoundaryReachedAttempts;
        public Integer preIdempotencyBlockedAttempts;
        public Boolean attributionEligible;
        public Boolean terminalStateReached;
        public Boolean localStateConsistentWithExternalReality;
        public Boolean converged;
        public Integer recoveryAttempts;
        public Integer reconciliationAttempts;
        public Integer manualReviewCount;
        public Integer permanentStuckCount;
        public Integer incorrectTerminalStateCount;
        public Boolean unauthorizedBlocked;
        public Boolean falseRejected;
        public Integer unauthorizedWriteCount;
        public Integer approvalBypassCount;
        public Integer crossTenantViolationCount;
        public Integer crossShopViolationCount;
        public Boolean unauthorizedCase;
        public Boolean legitimateCase;
    }
}
