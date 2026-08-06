package com.sirithree.shopops.admin.reliability;

import com.sirithree.shopops.admin.reliability.domain.WriteOperationStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class WriteOperationStatusTest {
    @Test void allowsExpectedTransitions() {
        assertThatCode(() -> WriteOperationStatus.requireTransition(WriteOperationStatus.EXECUTING, WriteOperationStatus.EXTERNAL_UNKNOWN)).doesNotThrowAnyException();
        assertThatCode(() -> WriteOperationStatus.requireTransition(WriteOperationStatus.EXTERNAL_UNKNOWN, WriteOperationStatus.EXTERNAL_SUCCEEDED)).doesNotThrowAnyException();
    }
    @Test void rejectsIllegalTransition() {
        assertThatThrownBy(() -> WriteOperationStatus.requireTransition(WriteOperationStatus.CREATED, WriteOperationStatus.SUCCEEDED))
                .isInstanceOf(IllegalStateException.class);
    }
}
