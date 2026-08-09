# Phase 3 Fault Matrix

| Fault / scenario | Injection point / mechanism | Expected production behavior | Expected external effects for one logical refund | Implemented | Runtime verified in Spring/JUnit |
|---|---|---|---:|---|---|
| Baseline single delivery | none | execute once, confirm local | 1 | YES | NOT RUN |
| Client/sequential retry | repeated delivery reaches Tool Gateway | first write executes; later replay/in-progress guard prevents second external call | 1 | YES | NOT RUN |
| Concurrent retry | 2–5 simultaneous deliveries, no harness lock | production memory atomic prepare / JDBC unique key decides winner | 1 | YES | NOT RUN |
| Same key + same payload | same operationRequestId and semantic payload | replay or in-progress result, no new external effect | 1 | YES | NOT RUN |
| Same key + different payload | same key, changed refund amount | `IDEMPOTENCY_PAYLOAD_MISMATCH`; no second effect | 1 | YES | NOT RUN |
| Timeout before external acceptance | `simulation=timeout_before_success` | external returns UNKNOWN with no accepted effect; current retry remains blocked by unknown state | 0 unless recovery later succeeds | YES | NOT RUN |
| Timeout after external success | `simulation=timeout_after_success` | ledger records accepted effect; ShopOps enters unknown; retry must not issue another refund | 1 | YES | NOT RUN |
| External success + local failure | `AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM` | external effect exists; local stays pre-confirm; immediate replay is blocked | 1 | YES | NOT RUN |
| Response loss after local confirm | `AFTER_LOCAL_CONFIRM_BEFORE_ACK` | local write already SUCCEEDED; retry returns idempotent replay | 1 | YES | NOT RUN |
| Before external call | `BEFORE_EXTERNAL_CALL` | fail before external transport | 0 | Instrumented | NOT RUN |
| Reconciliation query failure | `BEFORE_RECONCILIATION_QUERY` | reconciliation attempt fails before query | unchanged | Instrumented | NOT RUN |
| Outbox publish succeeded, local mark failed | `BEFORE_OUTBOX_MARK_PUBLISHED` | event may be published again; this is an outbox-delivery issue, not refund-effect count | refund count unchanged | Instrumented | NOT RUN |
| Real Rabbit ACK failure for refund write | no current queued refund-write consumer boundary | current refund path is direct Tool Gateway execution | N/A | NOT SUPPORTED | NOT SUPPORTED |
| Real Rabbit message redelivery for refund write | no current queued refund-write message path | cannot truthfully call deterministic repeated Tool invocations Rabbit redelivery | N/A | NOT SUPPORTED | NOT SUPPORTED |
| Worker restart in refund write pipeline | no distinct persisted refund worker ownership/claim boundary | method-twice is not called worker restart | N/A | NOT SUPPORTED | NOT SUPPORTED |

## Deterministic fault controller

`ReliabilityFaultController` is a production-safe no-op SPI. Tests use `DeterministicReliabilityFaultController`, configured by point + occurrence number. It has no case-id switch and does not alter success semantics unless explicitly armed by the test infrastructure.

## Reuse for Phase 4

The `AFTER_EXTERNAL_SUCCESS_BEFORE_LOCAL_CONFIRM`, `BEFORE_RECONCILIATION_QUERY`, and external `UNKNOWN` semantics are deliberately reusable for state-convergence testing. Phase 3 only counts effects; Phase 4 must judge whether local state eventually converges to external reality.
