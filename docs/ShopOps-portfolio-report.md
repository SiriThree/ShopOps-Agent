# ShopOps Portfolio Report

Generated at: 2026-07-22 17:18:08

## 1. Positioning

ShopOps is an AgentOps admin platform for ecommerce operations. The project focuses on making an agent execution flow observable, auditable, configurable, testable, and driven by replaceable business data connectors.

The strongest portfolio story now is: generate a daily operation review from Olist public ecommerce data, then show the task lifecycle, tool evidence, approval workflow, audit timeline, shop configuration snapshot, and quantitative evaluation baseline.

## 2. Quantitative Results

| Area | Metric | Current result |
|---|---|---:|
|Agent evaluation|Total cases|14|
|Agent evaluation|Passed cases|14|
|Agent evaluation|Completion rate|100%|
|Agent evaluation|Success rate|71.4%|
|Agent evaluation|Degraded completion rate|21.4%|
|Agent evaluation|Average task duration|34.6 ms|
|Agent evaluation|Tool invocation success rate|98.6%|
|Agent evaluation|Approval decision accuracy|100%|
|Agent evaluation|Config effect accuracy|100%|
|Olist demo|Business date|2018-08-07|
|Olist demo|GMV|62057.77|
|Olist demo|Order count|370|
|Olist demo|Refund proxy amount|4732.62|
|Olist demo|Refund proxy rate|7.63%|
|Olist demo|Risk comment count|51|
|Olist demo|Product candidate count|10|
|Olist demo|Daily review task duration|43.9 ms|

## 3. Olist Data Integration

| Connector | Data source | Status | Role |
|---|---|---|---|
|file.order-summary|Olist orders + payments|UP|GMV, order count, average order amount, refund proxy rate|
|file.negative-comments|Olist reviews + order items|UP|Low-score reviews, risk samples, product risk aggregation|
|file.product-candidates|Olist products + reviews + items|UP|Optimization candidates, risk score, product priority|
|file.ad-performance|Not covered by Olist|NOT_CONFIGURED|Uses built-in demo data for now|
|file.external-reports|Not covered by Olist|NOT_CONFIGURED|Uses built-in demo data for now|

Olist sample date: 2018-08-07. The selected day contains 370 orders, GMV 62057.77, and 51 risk comments. This is enough to demonstrate a real-data-driven agent report.

Top product priority: Furniture Bedroom / 4f18ca98, score 80.0, risk comments 2.

## 4. AgentOps Demo Chain

| Step | Result |
|---|---|
|Task creation|SUCCESS, taskId=10004|
|Report generation|SUCCESS, reportId=90004|
|Evidence tools|order.query_summary, comment.query_negative, product.query_candidates, ad.query_performance, report.query_external_metrics|
|Shop config snapshot|refundRateWarnThreshold=0.08, negativeCommentWarnThreshold=10, agentModelPolicy=balanced|
|High-risk refund approval|APPROVED, approvalId=4|
|Confirmation guard|高风险审批通过前需输入确认语：确认通过|
|Tool retry after approval|SUCCESS, refund status EXECUTED|
|Audit events|2|

## 5. Evaluation Coverage

| Suite | Cases | Passed | Success | Degraded | Approval Required |
|---|---:|---:|---:|---:|---:|
|Core|7|7|6|0|1|
|Model|4|4|4|0|0|
|Degraded|3|3|0|3|0|

Status breakdown: {"APPROVAL_REQUIRED":1,"DEGRADED":3,"SUCCESS":10}.

The current baseline covers daily review tasks, model policies, runtime config thresholds, high-risk tool approval, direct execution when approval is disabled, and degraded completion after model failure.

## 6. Interview Pitch

> ShopOps is not a plain AI report demo. It is an AgentOps backend for ecommerce operations. I decomposed an operation-review agent into tasks, tools, reports, approvals, audits, runtime configuration, and evaluation suites. The current build passes 14/14 evaluation cases, reaches 98.6% tool invocation success rate, and validates approval and configuration behavior at 100% accuracy. I also connected Olist public ecommerce data so the report can be driven by real orders and real reviews.

Recommended demo flow:

1. Open Dashboard and frame the system as an operations agent console.
2. Show shop runtime configuration and explain how thresholds affect execution.
3. Create a daily_review task for 2018-08-07.
4. Open the report and show Olist metrics plus the evidence config snapshot.
5. Trigger the high-risk refund tool and show approval plus confirmation guard.
6. Open Audit Center and show the traceable approval timeline.
7. End with this report's evaluation metrics to show measurable acceptance.

## 7. Current Boundaries

- Olist does not provide a real refund amount field, so canceled / unavailable payment amount is used as a refund or after-sales risk proxy.
- Olist does not include ad performance or external environment metrics, so those two connectors still use built-in demo data.
- Current demo report generation mode is RULE. Real model calls can still be enabled through Model Gateway provider configuration.
- Olist does not provide native product titles. The demo uses English category plus productId prefix as the display name.

## 8. Reproduction Commands

Prepare Olist connector files:

```powershell
python scripts/prepare_olist_demo.py
```

Start the backend:

```powershell
mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--shopops.connector.order-summary.file=docs/demo-data/olist/order-summary-olist.json --shopops.connector.negative-comments.file=docs/demo-data/olist/negative-comments-olist.json --shopops.connector.product-candidates.file=docs/demo-data/olist/product-candidates-olist.json"
```

Run Olist demo verification:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-agentops-demo.ps1 -Port 8080 -Start 2018-08-07 -End 2018-08-07 -Scenario olist-agentops-demo -Dataset olist
```

Refresh the evaluation baseline:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-agent-evaluation.ps1
```
