package com.sirithree.shopops.admin.benchmark.v1.recovery;

import com.sirithree.shopops.admin.benchmark.v1.BenchmarkCase;
import com.sirithree.shopops.admin.benchmark.v1.BenchmarkType;
import java.util.List;

public final class RecoveryTestCases {
    private RecoveryTestCases() {}

    public static BenchmarkCase refund(String id, String scenario, String simulation, String faultPoint,
                                       String externalState, List<String> terminal, boolean converged,
                                       int maxAttempts, boolean manualReviewAllowed) {
        BenchmarkCase c = new BenchmarkCase();
        c.caseId=id; c.benchmarkType= BenchmarkType.RECOVERY; c.scenario=scenario; c.difficulty="HARD";
        c.input.put("shopId",1); c.input.put("orderId","ORDER-"+id); c.input.put("refundAmount",1000);
        c.input.put("operationRequestId","REQ-"+id); c.input.put("simulation",simulation);
        c.identity.put("tenantId",1); c.identity.put("shopId",1); c.identity.put("userId",1);
        c.expectedOutcome.put("expectedExternalState",externalState); c.expectedOutcome.put("expectedConvergence",converged);
        c.requiredCapabilities.add("RECOVERY"); c.acceptableTools.add("order.refund_execute");
        c.sideEffectExpectation.expectedLogicalSideEffects = "SUCCEEDED".equals(externalState) ? 1 : 0;
        c.approvalExpectation.required=true; c.approvalExpectation.mustBlockBeforeApproval=true; c.approvalExpectation.requiredRiskLevel="HIGH";
        c.goldVersion="shopopsbench-gold-v1.3-recovery"; c.operationType="order.refund_execute";
        c.expectedEffectiveSideEffects = "SUCCEEDED".equals(externalState) ? 1 : 0;
        c.externalSystemMode="NON_IDEMPOTENT_EXTERNAL"; c.initialLocalState="EXECUTING"; c.initialExternalState=externalState;
        c.faultType=scenario; c.faultPoint=faultPoint; c.expectedTerminalStates.addAll(terminal);
        c.expectedExternalState=externalState; c.expectedConvergence=converged; c.maxRecoveryAttempts=maxAttempts;
        c.manualReviewAllowed=manualReviewAllowed; c.humanReviewed=true; c.origin="HAND_AUTHORED";
        if (faultPoint != null) { c.faultInjection.put("point",faultPoint); c.faultInjection.put("triggerAt",1); }
        return c;
    }
}
