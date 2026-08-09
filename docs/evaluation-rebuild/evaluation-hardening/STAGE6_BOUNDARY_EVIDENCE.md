# Stage 6 Boundary Evidence

## Execution chain

For each intended attempt, Stage 6 evidence models:

`Attempt`
`-> Tool Gateway`
`-> Authorization`
`-> Schema`
`-> Business Scope`
`-> Approval`
`-> Write Executor`
`-> WriteOperationService`
`-> Idempotency Decision`
`-> External Attempt or Existing Result`

## Per-attempt evidence

`IdempotencyAttemptAttribution` records:

- attempt number and kind;
- whether it is an intended replay;
- approval ID;
- gateway reached;
- authorization passed;
- schema passed;
- business scope passed;
- approval passed;
- write executor reached;
- write-operation boundary reached;
- external attempt observed;
- pre-idempotency blocked;
- attribution code;
- result status/error code.

## Classification

Boundary-reached decisions include successful first execution, idempotent replay, payload conflict, operation-in-progress, recovery-required/terminal-state decisions, and external-result outcomes that occur only after `WriteOperationService.prepare(...)`.

Pre-boundary governance failures are classified separately.

`externalAttemptObserved=false` does not imply the boundary was missed: correct replay deduplication normally reaches the boundary and deliberately skips a new external call.

## Summary evidence

The driver publishes:

- intended idempotency attempts;
- intended replay attempts;
- idempotency-boundary-reached attempts;
- pre-idempotency-blocked attempts;
- attribution-invalid reasons;
- attribution-eligible boolean;
- external attempts;
- external effects.

Approval setup activities are counted separately as governance setup and are not added to logical-operation or replay denominators.

## Evidence caveat

The per-attempt boundary classifier is benchmark/test infrastructure. It does not modify production logs or branch on benchmark case IDs.

Actual Spring contract tests are NOT RUN in this environment, so the machine-observed gateway lifecycle has not yet been promoted beyond static/isolated compilation evidence.
