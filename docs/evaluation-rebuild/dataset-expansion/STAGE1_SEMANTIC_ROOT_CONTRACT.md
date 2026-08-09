# Stage 1 Semantic Root Contract

## Purpose

`semanticRootId` is the group-split identity for dataset independence. It represents underlying semantic information, not a case filename.

The invariant for future datasets is:

```text
one semanticRootId -> exactly one of dev / validation / test
```

Language rewrites, low-level parameter perturbations, different random seeds and repeated operation IDs do not automatically create new roots.

Stage 1 stores the root assignment in:

`shopops-admin/src/test/resources/benchmark/v1/audit/stage1-semantic-root-map.json`

This is an **audit overlay**. It does not enter Agent runtime input and does not mutate Phase 6 Gold.

## Task root

A Task root represents:

```text
business goal
+ business fixture/state
+ material constraints
```

Current examples:

```text
task:daily_review:date:2018-08-01:2018-08-01
  dev-task-daily-review-001
  dev-task-daily-review-002
  smoke-task-daily-review-001

task:daily_review:safe-default
  dev-task-daily-missing-date-001
  test-task-daily-missing-date-001
```

The second example is a cross-split leak: Chinese/English wording does not create an independent business problem when the missing-date condition and Gold resolution are the same.

## Governance root

A Governance root represents:

```text
attack/control boundary
+ principal/authorization relation
+ business operation
+ security policy
```

Examples:

```text
governance:viewer_refund_permission
  gov-dev-viewer-refund
  gov-test-viewer-refund
  Phase 0 unauthorized-refund contract example

governance:approval_payload_mutation
  gov-dev-approval-payload-mutation
```

Different refund amounts or request IDs do not automatically create new permission semantics.

## Recovery root

A Recovery root represents:

```text
causal failure pattern
+ fault boundary
+ external reality
+ expected recovery behavior
```

Example:

```text
recovery:external_success_local_failure
  recovery-dev-r1-external-success-local-failure
  recovery-test-r1-request-correlation
```

Different seeds or operationRequestIds do not create new roots.

## Idempotency root

An Idempotency root represents:

```text
repeat pattern
+ payload relation
+ concurrency/fault semantics
```

Example:

```text
idempotency:concurrent_retry
  idem-dev-concurrent-retry-001
  idem-test-concurrent-retry-001
```

Five different logical operation IDs would still be one semantic scenario if the repeat/payload/concurrency semantics are unchanged.

## Relationship to existing fields

`semanticTaskId` is retained as historical provenance, but Stage 1 does not use it as the authoritative split identity because it contains both false merges and fragmented roots.

`parentCaseId` remains lineage information and must never cross split after future migration.

## Runtime isolation

The semantic-root map is Gold-side audit metadata only. `BenchmarkRuntimeRequest` remains the runtime isolation boundary; semanticRootId, reviewStatus and Gold provenance are not copied into the Agent request.
