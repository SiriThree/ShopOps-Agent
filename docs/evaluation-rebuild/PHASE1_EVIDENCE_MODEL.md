# Phase 1 Evidence Model

## Evidence flow

```text
BenchmarkCase
    |
    | BenchmarkRuntimeRequest.from(case)
    | allowlist: caseId/scenario/input/identity/initialState only
    v
HTTP /api/agent/tasks/natural-language
    v
AgentTask
    v
AgentTaskEvent + AgentTaskStep
    v
Tool Gateway
    +--> ToolCallLog
    +--> TraceSpan
    v
ApprovalRequest (when applicable)
    v
WriteOperation (when applicable)
    v
External side effect (only if authoritative evidence exists)
    v
OperationReport
    v
ProductionBenchmarkEvidenceCollector
    v
CollectedEvidence + EvidenceRef
    v
BenchmarkEvaluator
    v
EvaluationRecord
```

## Source-of-truth table

| Evidence | Source of truth | Phase 1 collector | Classification |
|---|---|---|---|
| Task identity/final state | `AgentTaskService.getTask` over production task persistence | yes | production source |
| State transitions | `AgentTaskService.listEvents` / `AgentTaskEvent` | yes | production source |
| Step execution | `AgentTaskService.listSteps` / `AgentTaskStep` | yes | production source |
| Tool attempts/result status | `ToolCallLogService.listByTaskId` | yes, sensitive fields sanitized | production source |
| Planner mode/fallback | `TraceService.listSpans`, `agent.planner` span | yes | production instrumentation |
| Other traces | `TraceService.listSpans` | yes | production source |
| Approval | `ApprovalRequestService.list(taskId=...)` | yes | production source |
| Write operation | `WriteOperationService.listByTaskId` | yes | production source; read-only observability added in Phase 1 |
| Report | `OperationReportService.getReport` | yes | production source |
| Business facts | task/report/write/approval evidence projected by collector | yes | derived from production sources |
| Effective external side effect | third-party/external ground truth | **not generally available** | unavailable in Phase 1 |
| Outbox publish/redelivery | Outbox/MQ infrastructure | not collected by TASK smoke runner | known limitation |
| Rabbit consumer redelivery | broker/consumer metadata | not collected | known limitation |

## EvidenceRef

Large payloads are not copied wholesale into `EvaluationRecord`. The collector creates `EvidenceRef` entries containing:

```text
sourceType
sourceId
summary
hash
timestamp
```

Agent steps and Tool attempts/results in the record are compact projections (identity/status/error plus SHA-256 input/output hashes where available). Full underlying facts remain at the production evidence source. Sensitive keys such as tokens, authorization, credentials, phone, email and address are redacted before test-side evidence processing.

## Planner observation

`RulePlannerService` now records, without changing planning semantics:

```text
plannerMode
fallback
fallbackReason
plannedToolCodes
```

`DefaultAgentEngineService` writes these fields into the existing `agent.planner` Trace span. This permits later comparison of planned vs executed tools and model-vs-rule/fallback runs.

## Unknown is not zero

Phase 1 deliberately leaves the following unavailable unless a real source can prove them:

- effective external side-effect count;
- duplicate effective side-effect count;
- unauthorized effective-write count;
- cross-tenant effective-write count.

A successful Tool call, HTTP call, DB insert, or worker attempt is not substituted for external business reality.
