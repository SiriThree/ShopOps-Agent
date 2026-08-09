package com.sirithree.shopops.admin.benchmark.v1.idempotency;

import java.util.ArrayList;
import java.util.List;

/**
 * Attribution gate: application-idempotency metrics are eligible only when the first legal operation and every
 * intended replay reaches the WriteOperation idempotency decision boundary rather than being stopped by governance.
 */
public final class IdempotencyAttributionEligibility {
    public Result evaluate(List<IdempotencyAttemptAttribution> attempts) {
        if (attempts == null || attempts.isEmpty()) {
            return new Result(false, 0, 0, 0, List.of("NO_ATTEMPT_EVIDENCE"));
        }
        List<String> reasons = new ArrayList<>();
        int intended = 0;
        int reached = 0;
        int preBlocked = 0;
        for (IdempotencyAttemptAttribution attempt : attempts) {
            if (attempt.attemptNo() == 1 || attempt.intendedReplay()) {
                intended++;
                if (attempt.writeOperationBoundaryReached()) reached++;
                if (attempt.preIdempotencyBlocked()) {
                    preBlocked++;
                    reasons.add(attempt.attributionCode());
                }
            }
        }
        boolean firstReached = attempts.stream()
                .filter(a -> a.attemptNo() == 1)
                .findFirst()
                .map(IdempotencyAttemptAttribution::writeOperationBoundaryReached)
                .orElse(false);
        if (!firstReached) reasons.add("FIRST_ATTEMPT_DID_NOT_REACH_IDEMPOTENCY_BOUNDARY");
        boolean eligible = firstReached && intended > 0 && intended == reached && preBlocked == 0;
        return new Result(eligible, intended, reached, preBlocked, List.copyOf(reasons));
    }

    public record Result(
            boolean eligible,
            int intendedAttempts,
            int boundaryReachedAttempts,
            int preIdempotencyBlockedAttempts,
            List<String> reasons) {
    }
}
