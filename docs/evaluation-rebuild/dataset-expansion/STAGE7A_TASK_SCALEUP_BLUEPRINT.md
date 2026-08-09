# Stage 7A Task Scale-up Blueprint

## Goal
Expand the Stage 6 Task candidate from **93 cases / 52 roots / 23 true test-exclusive roots** without changing Production Runtime or Task Outcome Evaluators and without executing new held-out roots.

## Pipeline
`Capability Audit → Business-State Space → Root Blueprint → Fixture Feasibility → Independent Gold → Distinctness Review → Split Assignment → Case Generation → Author → Critic → Adjudication → Admission`.

## Candidate outcome
- Proposed roots: **100**
- Accepted: **64**
- Rejected: **36**
- Revised during model review: **16**
- Adjudicated accepted roots: **12**
- New cases: **96**
- New roots: **64**
- New case/root ratio: **1.50**

Only `daily_review`, `comment_risk`, `product_optimization`, and `ad_anomaly` are admitted as NL-reachable Task families. TOOL_FAILURE/DEGRADED proposals remain excluded because the current Task evaluator cannot reliably express those outcomes without a separate evaluation-contract change.

Machine blueprint: `benchmark/v1/task/stage7a/task-scaleup-root-blueprints.json`.
