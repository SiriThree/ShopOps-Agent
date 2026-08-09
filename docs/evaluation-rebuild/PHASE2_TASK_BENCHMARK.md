# Phase 2 — End-to-End Agent Task Benchmark

## Purpose

The Task Benchmark evaluates whether a user-level ShopOps business objective is actually completed by the production Agent runtime. It does not use intent accuracy or exact Tool sequence equality as the primary success definition.

## Binary task definition

```text
TaskSuccess =
    BusinessOutcomeCorrect
AND ToolExecutionValid
AND GovernanceSatisfied
AND NoUnexpectedSideEffect
AND FinalStateCorrect
```

The result is binary per task. There is no weighted score threshold.

## Outcome evaluator architecture

```text
BusinessOutcomeEvaluator
├── OrderReviewOutcomeEvaluator
├── CommentHandlingOutcomeEvaluator
├── ProductOptimizationOutcomeEvaluator
└── AdAnalysisOutcomeEvaluator
```

`REPORT_SYNC` has no evaluator in the formal Task Benchmark because the current NL Agent cannot reach `feishu.sync_report`.

### Source-of-Truth priority

The benchmark contract keeps the Phase 2 priority order:

1. persistent business state, when the task has a persistent business outcome;
2. external/test-adapter ground truth, when an external outcome exists;
3. `WriteOperation` / domain write state;
4. structured successful Tool results;
5. `OperationReport` / report text.

The four task families currently reachable from the NL Agent are read-oriented analysis/report tasks, so they do not have a post-write persistent business object to inspect. For those cases the highest available deterministic evidence is the structured output produced by the real business-service/MCP Tool path, and `OperationReport.evidence` is checked only for consistency against that source. Report text never overrides structured business evidence.

`task.status == SUCCESS` and `report != null` are never sufficient by themselves.

## Business outcome rules

### Daily / order review

Requires evidence from:

- orders;
- comments;
- products;
- ads;
- external metrics.

The evaluator checks requested date propagation and verifies report evidence metrics against the structured Tool outputs.

### Comment risk

Checks:

- negative-comment output exists;
- candidate-product output exists;
- order baseline exists;
- report `negativeCount` / `candidateCount` match source;
- report comment IDs are a subset of actual risk comments;
- report product IDs are a subset of actual candidate products;
- at least one comment-affected product overlaps the candidate set when risk comments exist.

### Product optimization

Checks:

- product candidates exist;
- supporting comment signal and order baseline were actually queried;
- report candidate counts match source;
- reported product IDs come from actual candidates;
- an actual nonblank recommendation/report is produced.

This is an analysis capability. It does not claim the product was mutated.

### Ad analysis

Distinguishes:

- `NO_DATA`;
- `NORMAL`;
- `RISK_FOUND`.

Classification is derived from structured ad data, not a keyword in the report. A production bug discovered while implementing this evaluator was fixed: a valid empty ad response now receives structured `adDataStatus=NO_DATA` and no longer generates an “ROI < 3” recommendation from missing values.

## ToolExecutionValid

`ToolLegalityEvaluator` does not compare one exact Tool list.

It evaluates:

- known vs unknown Tool;
- forbidden Tool use;
- non-accepted write Tool use;
- trusted shop identity vs Tool input shop scope;
- schema/permission/approval failures;
- required capability satisfaction using successful tools.

Extra non-forbidden read Tools are counted as `redundantToolCallCount`; they do not automatically fail a task.

Optional Tool failures can be diagnostic when a required capability remains satisfied through another successful Tool.

## Alternative valid plans

The evaluator is sequence-insensitive unless a real business requirement is encoded elsewhere. `AlternativePlanAcceptanceTest` evaluates the same valid evidence in a different Tool-log order and requires both to pass.

This demonstrates that the new Task Benchmark does not restore the legacy `actualToolCodes == expectedToolCodes` list equality.

The current production report assembler still consumes named evidence slots (`product.query_candidates`, `comment.query_negative`, etc.), so Phase 2 does not falsely claim that arbitrary alternative Tool *codes* are already interchangeable in production.

## Missing parameters

Cases may specify:

```json
"parameterResolution": "SAFE_DEFAULT"
```

For these cases the evaluator accepts a safe, non-empty, consistent date range selected by production runtime. It does not force clarification when current business logic legitimately defines a default.

Multi-turn clarification is not implemented in this runtime and is reported as NOT_SUPPORTED rather than simulated by the benchmark.

## Empty result and degraded behavior

Empty business data is distinct from Tool failure.

The Phase 2 ad no-data path uses a real file-backed business adapter returning a valid matching record with an empty summary. The report generator and outcome evaluator represent it as `NO_DATA`.

Tool failure remains represented by failed Tool evidence and the relevant failure reason. A degraded task is judged against the business goal rather than being globally mapped to pass or fail.

## Failure taxonomy additions

Phase 2 adds:

- `BUSINESS_DATA_MISSING`
- `BUSINESS_TARGET_INCORRECT`
- `BUSINESS_RESULT_MISMATCH`

while retaining `REPORT_INCONSISTENT`, `REQUIRED_CAPABILITY_MISSING`, `INVALID_TOOL_ARGUMENT`, `PLANNING_ERROR`, `TOOL_EXECUTION_ERROR`, and the Phase 1 governance/runtime reason codes.

## Incorrect Success

The record now derives an auxiliary `incorrectSuccess` indicator:

```text
runtime final state says SUCCESS
AND BusinessOutcomeCorrect != true
```

It is diagnostic only. It does not replace Task Success.

## Formal report behavior

`BenchmarkReportWriter` now distinguishes:

- `Task Success Diagnostic (NON-FORMAL)` for smoke/dev/validation;
- `End-to-End Agent Task Success (FORMAL_TEST)` only for `test` split output.

The PowerShell runner additionally refuses routine `test` execution unless `-FormalTest` is supplied.
