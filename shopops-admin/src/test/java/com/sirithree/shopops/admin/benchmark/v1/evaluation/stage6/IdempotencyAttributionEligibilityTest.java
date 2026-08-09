package com.sirithree.shopops.admin.benchmark.v1.evaluation.stage6;

import static org.assertj.core.api.Assertions.assertThat;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyAttemptAttribution;
import com.sirithree.shopops.admin.benchmark.v1.idempotency.IdempotencyAttributionEligibility;
import java.util.List;
import org.junit.jupiter.api.Test;

class IdempotencyAttributionEligibilityTest {
    @Test void allIntendedAttemptsMustReachBoundary() {
        var ok = attempt(1, false, true, "IDEMPOTENCY_BOUNDARY_REACHED");
        var replay = attempt(2, true, true, "IDEMPOTENCY_REPLAY_DEDUPED");
        var result = new IdempotencyAttributionEligibility().evaluate(List.of(ok, replay));
        assertThat(result.eligible()).isTrue();
        assertThat(result.boundaryReachedAttempts()).isEqualTo(2);
    }
    @Test void approvalBlockInvalidatesAttribution() {
        var ok = attempt(1, false, true, "IDEMPOTENCY_BOUNDARY_REACHED");
        var blocked = attempt(2, true, false, "ATTRIBUTION_INVALID_APPROVAL_BLOCK");
        var result = new IdempotencyAttributionEligibility().evaluate(List.of(ok, blocked));
        assertThat(result.eligible()).isFalse();
        assertThat(result.preIdempotencyBlockedAttempts()).isEqualTo(1);
    }
    private IdempotencyAttemptAttribution attempt(int no, boolean replay, boolean reached, String code) {
        return new IdempotencyAttemptAttribution(no, replay ? "REPLAY" : "INITIAL", replay, (long) no,
                true, reached, reached, reached, reached, reached, reached, false, !reached, code, reached ? "SUCCESS" : "FAILED", reached ? "" : "APPROVAL_NOT_APPROVED");
    }
}
