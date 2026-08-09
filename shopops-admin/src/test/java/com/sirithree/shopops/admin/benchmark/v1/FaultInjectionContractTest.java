package com.sirithree.shopops.admin.benchmark.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sirithree.shopops.admin.benchmark.v1.fault.DeterministicReliabilityFaultController;
import com.sirithree.shopops.admin.benchmark.v1.fault.InjectedReliabilityFaultException;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultContext;
import com.sirithree.shopops.admin.reliability.fault.ReliabilityFaultPoint;
import org.junit.jupiter.api.Test;

class FaultInjectionContractTest {
    @Test
    void faultMustTriggerAtConfiguredProductionBoundaryOccurrence() {
        DeterministicReliabilityFaultController faults = new DeterministicReliabilityFaultController();
        faults.arm(ReliabilityFaultPoint.AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM, 2);
        ReliabilityFaultContext context = new ReliabilityFaultContext("order.refund_execute", "REQ-1", "ORDER-1", "EXT-1");
        faults.hit(ReliabilityFaultPoint.AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM, context);
        assertThatThrownBy(() -> faults.hit(ReliabilityFaultPoint.AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM, context))
                .isInstanceOf(InjectedReliabilityFaultException.class);
        assertThat(faults.events()).hasSize(2);
        assertThat(faults.events().get(1)).containsEntry("injected", true);
    }
}
