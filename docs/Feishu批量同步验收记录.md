# Feishu Batch Sync Evidence

This document describes how to run a repeatable batch verification for `feishu.sync_report`.

## What It Measures

The batch runner invokes the ShopOps tool API multiple times and records:

| Metric | Meaning |
| --- | --- |
| `requestCount` | Number of webhook sync attempts |
| `successRate` | Share of calls where ShopOps returned `SUCCESS`, `feishu-webhook`, and webhook HTTP `200` |
| `webhookModeRate` | Share of calls that actually used configured webhook mode instead of demo mode |
| `http200Rate` | Share of calls where the webhook endpoint returned HTTP `200` |
| `avgLatencyMs` | Average end-to-end API latency from the local runner |

## Preparation

Create a Feishu custom bot webhook in a test group. If keyword verification is enabled, include `ShopOps` as an allowed keyword.

Set environment variables in the same PowerShell window that starts the backend:

```powershell
$env:SHOPOPS_FEISHU_SYNC_ENABLED="true"
$env:SHOPOPS_FEISHU_SYNC_WEBHOOK_URL="https://open.feishu.cn/open-apis/bot/v2/hook/<token>"
$env:SHOPOPS_FEISHU_SYNC_TIMEOUT_MS="3000"
```

Start or restart the backend from that same shell:

```powershell
mvn -pl shopops-admin -am spring-boot:run
```

## Run 20 Requests

Open another PowerShell window and run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-feishu-webhook-batch.ps1 -Count 20 -DelayMs 300
```

For a stronger success-rate claim, run 50 requests:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-feishu-webhook-batch.ps1 -Count 50 -DelayMs 300
```

## Evidence Files

The runner writes:

| File | Purpose |
| --- | --- |
| `docs/evaluation/feishu-webhook-batch-summary.json` | Summary metrics |
| `docs/evaluation/feishu-webhook-batch-details.csv` | One row per request |

Keep an additional screenshot of the Feishu group receiving messages and a snippet of backend logs for `/api/tools/feishu.sync_report/invoke`.

## Resume-Safe Wording

After a successful 20-request run:

> Verified configurable Feishu webhook sync with 20 repeated ShopOps tool invocations, recording HTTP delivery status, latency, and success-rate evidence.

Only write a numeric success rate after the batch file exists and the number comes from `feishu-webhook-batch-summary.json`.
