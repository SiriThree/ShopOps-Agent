package com.sirithree.shopops.admin.benchmark.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Benchmark-only contract. It deliberately does not reuse production AgentTask domain objects. */
public class BenchmarkCase {
    public String caseId;
    public BenchmarkType benchmarkType;
    public String scenario;
    public String difficulty;
    public Map<String, Object> input = new LinkedHashMap<>();
    public Map<String, Object> identity = new LinkedHashMap<>();
    public Map<String, Object> initialState = new LinkedHashMap<>();
    public Map<String, Object> expectedOutcome = new LinkedHashMap<>();
    public List<String> requiredCapabilities = new ArrayList<>();
    public List<String> optionalCapabilities = new ArrayList<>();
    public List<String> acceptableTools = new ArrayList<>();
    public List<String> forbiddenTools = new ArrayList<>();
    public SideEffectExpectation sideEffectExpectation = new SideEffectExpectation();
    public ApprovalExpectation approvalExpectation = new ApprovalExpectation();
    public Map<String, Object> faultInjection = new LinkedHashMap<>();
    public List<String> tags = new ArrayList<>();
    public String goldVersion;

    // Phase 2 provenance fields. Optional for Phase 0/1 backward compatibility.
    public String semanticTaskId;
    public String origin;
    public String parentCaseId;
    public String perturbationType;
    public String generationMethod;
    public Boolean humanReviewed;

    // Stage 2 dataset-engineering metadata. Gold-side only; BenchmarkRuntimeRequest never copies these.
    public String semanticRootId;
    public String goldSourceType;
    public String reviewStatus;
    public Boolean reservedForHeldOut;
    public String pairedRootId;

    // Phase 3 idempotency fields. Optional for non-IDEMPOTENCY cases.
    public String operationType;
    public Integer logicalWriteCount;
    public Integer expectedEffectiveSideEffects;
    public Map<String, Object> deliveryPattern = new LinkedHashMap<>();
    public Map<String, Object> concurrency = new LinkedHashMap<>();
    public Map<String, Object> idempotencyExpectation = new LinkedHashMap<>();
    public String externalSystemMode;
    public Long faultSeed;

    // Phase 4 recovery fields. Optional for non-RECOVERY cases.
    public String initialLocalState;
    public String initialExternalState;
    public String faultType;
    public String faultPoint;
    public List<String> expectedTerminalStates = new ArrayList<>();
    public String expectedExternalState;
    public Boolean expectedConvergence;
    public Integer maxRecoveryAttempts;
    public Boolean manualReviewAllowed;

    // Phase 5 execution-governance fields. Optional for non-GOVERNANCE cases.
    public String governanceCaseClass;
    public String attackType;
    public String toolCode;
    public Map<String, Object> arguments = new LinkedHashMap<>();
    public String expectedDecision;
    public String expectedReason;
    public Boolean externalSideEffectAllowed;
    public Long targetTenant;
    public Long targetShop;
    public String riskLevel;
    public String authorizationMode;

    public static class SideEffectExpectation {
        public int expectedLogicalSideEffects;
        public List<String> allowedEffectTypes = new ArrayList<>();
        public List<String> forbiddenEffectTypes = new ArrayList<>();
        public Map<String, Object> constraints = new LinkedHashMap<>();
    }

    public static class ApprovalExpectation {
        public Boolean required;
        public Boolean mustBlockBeforeApproval;
        public String requiredRiskLevel;
        public Map<String, Object> constraints = new LinkedHashMap<>();
    }
}
