package com.sirithree.shopops.admin.reliability;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import org.junit.jupiter.api.Test;

class TerminalStateCannotReturnToExecutingTest {
    @Test
    void terminalCannotReenterExecutionOrRecoveryIntermediateState() {
        assertThatThrownBy(() -> WriteOperationStatus.requireTransition(
                WriteOperationStatus.SUCCEEDED, WriteOperationStatus.EXECUTING))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> WriteOperationStatus.requireTransition(
                WriteOperationStatus.SUCCEEDED, WriteOperationStatus.NEEDS_RECONCILIATION))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> WriteOperationStatus.requireTransition(
                WriteOperationStatus.NEEDS_MANUAL_ACTION, WriteOperationStatus.EXECUTING))
                .isInstanceOf(IllegalStateException.class);
    }
}
