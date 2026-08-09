# Phase 2 Handoff

## Current phase status

Phase 2 code/data implementation is complete enough to define real business-outcome judging, but the current environment did not pass the Runtime Verification Gate.

Therefore the correct status is:

```text
Task Benchmark contract/data/evaluators: IMPLEMENTED
Spring/JUnit runtime verification:       NOT RUN
Formal held-out Task Success:            NOT AVAILABLE
```

## Runtime to reuse

Do not create a new benchmark framework in Phase 3.

Reuse:

```text
BenchmarkCase
→ BenchmarkRuntimeRequest
→ ShopOpsBenchmarkRunner
→ HttpShopOpsBenchmarkRuntime
→ real Agent HTTP/runtime chain
→ ProductionBenchmarkEvidenceCollector
→ BenchmarkEvaluator SPI
→ EvaluationRecord
→ BenchmarkReportWriter
```

## Task capability facts

Current NL Agent reachable intents:

- `daily_review`
- `comment_risk`
- `product_optimization`
- `ad_anomaly`

Not currently reachable from the NL planner:

- `feishu.sync_report`
- `order.refund_execute`
- `product.update_title`
- other standalone write/export/draft Tools.

Do not infer capability from Tool registration alone.

## Phase 2 outcome evaluators

Implemented:

- `OrderReviewOutcomeEvaluator`
- `CommentHandlingOutcomeEvaluator`
- `ProductOptimizationOutcomeEvaluator`
- `AdAnalysisOutcomeEvaluator`
- `BusinessOutcomeEvaluator` dispatcher
- `CompositeTaskBenchmarkEvaluator`

They judge structured business evidence rather than task status/report existence.

## Tool-plan semantics

`ToolLegalityEvaluator` no longer uses full-list equality.

It enforces:

- forbidden/unknown Tool constraints;
- write Tool legality;
- trusted shop argument scope;
- required capability satisfaction;
- schema/permission/approval execution errors.

It tolerates extra legal read calls and records them as redundant.

`AlternativePlanAcceptanceTest` protects sequence-insensitive evaluation. The production report assembler still has named input slots, so arbitrary alternative Tool codes are not yet interchangeable.

## Dataset facts

```text
version:                 1.1.0-phase2-task
gold:                    shopopsbench-gold-v1.1
TASK cases:              21
unique semantic tasks:   16
NL variants:             21
dev/validation/test:     9 / 6 / 6
human-reviewed:          21
near-duplicate issues:   0
```

Scenario distribution:

```text
daily_review            7
comment_risk            6
product_optimization    4
ad_anomaly              4
```

Legacy migration:

- four old prompt families migrated as reviewed LEGACY seeds;
- repeated rounds are not counted as new tasks;
- old PASS logic is deprecated for E2E Task Success.

## Held-out behavior

Routine script execution of `test` is blocked unless:

```powershell
scripts/run-shopops-benchmark.ps1 -BenchmarkType task -Split test -FormalTest
```

Gold-bearing fields remain absent from `BenchmarkRuntimeRequest`.

## Production change made in Phase 2

`DailyReviewReportExecutor` was minimally changed for a real correctness bug:

```text
valid empty ad source
old behavior: missing metrics → numeric zero → false low-ROI/low-CTR recommendation
new behavior: adDataStatus=NO_DATA → explicitly state insufficient ad data
```

No Planner/Interpreter routing behavior was changed for benchmark scoring.

## Tests added in Phase 2

- `BusinessOutcomeEvaluatorTest`
- `OrderReviewOutcomeEvaluatorTest`
- `CommentHandlingOutcomeEvaluatorTest`
- `ProductOptimizationOutcomeEvaluatorTest`
- `AdAnalysisOutcomeEvaluatorTest`
- `ReportSyncOutcomeEvaluatorTest`
- `AlternativePlanAcceptanceTest`
- `TaskSuccessCompositionTest`
- `EmptyResultOutcomeTest`
- `EmptyResultTaskIntegrationTest`
- `SchemaErrorTaskTest`
- `MultiToolTaskTest`
- `MissingParameterTaskTest`
- `PlannerFallbackEvaluationTest`
- `DatasetProvenanceTest`
- `DatasetNearDuplicateTest`
- `HeldOutGoldIsolationTest`

Plus the production regression:

- `DailyReviewReportExecutorTest.shouldNotTreatMissingAdDataAsZeroPerformanceRisk`

All JUnit tests above are **NOT RUN** in the current environment because Maven is unavailable.

## Commands to run when Maven is available

Runtime Gate / Phase 1+2 core:

```bash
mvn -pl shopops-admin -am \
  -Dtest=BenchmarkRunnerLifecycleTest,BenchmarkDegradedSmokeIntegrationTest,BenchmarkEvidenceCollectorTest,EvaluationRunMetadataTest,BenchmarkEvaluatorTest,FailureReasonMappingTest,GoldLeakageProtectionTest,SingleCaseReplayTest,BenchmarkReportSerializationTest,SequentialAgentExecutorServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Phase 2 evaluator/data tests:

```bash
mvn -pl shopops-admin -am \
  -Dtest=BusinessOutcomeEvaluatorTest,OrderReviewOutcomeEvaluatorTest,CommentHandlingOutcomeEvaluatorTest,ProductOptimizationOutcomeEvaluatorTest,AdAnalysisOutcomeEvaluatorTest,ReportSyncOutcomeEvaluatorTest,AlternativePlanAcceptanceTest,TaskSuccessCompositionTest,EmptyResultOutcomeTest,EmptyResultTaskIntegrationTest,SchemaErrorTaskTest,MultiToolTaskTest,MissingParameterTaskTest,PlannerFallbackEvaluationTest,DatasetProvenanceTest,DatasetNearDuplicateTest,HeldOutGoldIsolationTest,DailyReviewReportExecutorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Legacy/regression suites must also be run before a formal result is accepted.

## Known limitations

1. Runtime Gate is not verified in this environment.
2. Rule-based interpreter remains the primary router.
3. Specialized intents use fixed rule plans.
4. Model planner for daily review still has exact six-step safety validation and may fall back.
5. No production multi-turn clarification state exists.
6. Schema-error injection through NL planner Tool arguments is not supported by the current planner contract.
7. Current Task Benchmark contains analysis/report tasks only; no NL write/approval Task is claimed.
8. `REPORT_SYNC` is excluded because current NL planner cannot reach it.
9. External third-party side-effect truth is still outside Phase 2 Task Benchmark.
10. No formal Task Success number is available yet.

## Phase 3 real entry: Side-Effect Idempotency Benchmark

Phase 3 should reuse, not rebuild:

- `ShopOpsBenchmarkRunner` execution/run metadata/reporting;
- `BenchmarkCase` and versioned dataset conventions;
- `ProductionBenchmarkEvidenceCollector` / `EvidenceRef` model;
- `EvaluationRecord` and Failure Reason taxonomy;
- existing `WriteOperationService` / `WriteOperation` state;
- existing approval and Tool call evidence;
- external client/test-adapter boundary;
- report serialization and single-case replay patterns.

The new work should concentrate on authoritative effective-side-effect ground truth and replay/failure attempts, especially:

```text
Logical Write Requests
Execution / Delivery Attempts
Effective External Side Effects
Duplicate Side Effects
```

Do not use worker invocation count, HTTP call count, or DB insert count as a substitute for effective external business side effects.
