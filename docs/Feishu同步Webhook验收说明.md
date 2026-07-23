# Feishu Sync Webhook Acceptance

This document records the current verification boundary for `feishu.sync_report`.

## Current Capability

`feishu.sync_report` now supports two modes:

| Mode | Trigger | Behavior |
| --- | --- | --- |
| `demo-connector` | default, no webhook configured | Returns a demo Feishu document result without external network dependency. |
| `feishu-webhook` | `shopops.feishu.sync.enabled=true` and `shopops.feishu.sync.webhook-url` is configured | Sends a real HTTP POST message to the configured webhook URL. |

The webhook URL is masked in tool output. The executor does not return the full token or secret URL.

## Runtime Configuration

Use environment variables:

```powershell
$env:SHOPOPS_FEISHU_SYNC_ENABLED="true"
$env:SHOPOPS_FEISHU_SYNC_WEBHOOK_URL="https://open.feishu.cn/open-apis/bot/v2/hook/<token>"
$env:SHOPOPS_FEISHU_SYNC_TIMEOUT_MS="3000"
```

Then start the backend and invoke:

```http
POST /api/tools/feishu.sync_report/invoke
Content-Type: application/json
X-Tenant-Id: 1
X-Shop-Id: 1
X-User-Id: 1
X-User-Roles: ADMIN

{
  "shopId": 1,
  "reportId": 90001,
  "documentUrl": "https://example.com/reports/90001"
}
```

Expected successful data fields in webhook mode:

```json
{
  "status": "SYNCED",
  "mode": "feishu-webhook",
  "webhookStatusCode": 200,
  "webhookUrlMasked": "https://open.feishu.cn/open-apis/bot/v2/hook/***"
}
```

## Automated Evidence

The automated test `FeishuSyncReportExecutorTest` starts a local HTTP server, configures `feishu.sync_report` with its webhook URL, executes the tool, and verifies that:

| Evidence | Status |
| --- | --- |
| The executor sends an HTTP POST request | verified by local mock webhook |
| The request body includes tenant, shop, report, task, and trace context | verified |
| Demo mode remains available when no webhook is configured | verified |
| The returned webhook URL is masked | verified |

Verification command:

```powershell
mvn -pl shopops-admin "-Dtest=FeishuSyncReportExecutorTest,McpToolCatalogIntegrationTest" test
```

Latest local result:

```text
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

## Resume Claim Boundary

This implementation verifies a real HTTP webhook connector path. It does not by itself prove a production Feishu success rate.

Resume-safe wording:

> Implemented a configurable Feishu webhook sync connector and verified the HTTP delivery path with an automated mock-webhook test.

Do not claim:

> Feishu sync success rate is 100%.

That claim requires running against a real Feishu webhook or Feishu Open Platform API and preserving the real response logs.
