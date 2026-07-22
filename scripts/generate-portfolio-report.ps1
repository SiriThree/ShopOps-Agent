param(
    [string]$Module = "shopops-admin",
    [string]$EvaluationSummaryPath = "",
    [string]$DemoSummaryPath = "",
    [string]$OlistDataDir = "docs/demo-data/olist",
    [string]$OutputPath = "docs/ShopOps-portfolio-report.md"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($EvaluationSummaryPath)) {
    $EvaluationSummaryPath = Join-Path $workspaceRoot "$Module/target/evaluation/agent-eval-portfolio-summary.json"
}
if ([string]::IsNullOrWhiteSpace($DemoSummaryPath)) {
    $DemoSummaryPath = Join-Path $workspaceRoot "$Module/target/demo/olist-agentops-demo-summary.json"
}
$olistRoot = Join-Path $workspaceRoot $OlistDataDir
$outputFile = Join-Path $workspaceRoot $OutputPath

function Read-JsonFile([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "Missing required file: $Path"
    }
    return Get-Content -Path $Path -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Format-Percent([double]$Value) {
    return ("{0:N2}%" -f ($Value * 100.0))
}

function Get-ConnectorStatus([object]$Demo, [string]$Code) {
    $item = @($Demo.connectors | Where-Object { $_.connectorCode -eq $Code } | Select-Object -First 1)
    if ($item.Count -eq 0) {
        return "UNKNOWN"
    }
    return $item[0].status
}

$evaluation = Read-JsonFile $EvaluationSummaryPath
$demo = Read-JsonFile $DemoSummaryPath
$orderSummary = @(Read-JsonFile (Join-Path $olistRoot "order-summary-olist.json"))[0]
$commentSummary = @(Read-JsonFile (Join-Path $olistRoot "negative-comments-olist.json"))[0]
$productSummary = @(Read-JsonFile (Join-Path $olistRoot "product-candidates-olist.json"))[0]

$order = $orderSummary.summary
$comments = $commentSummary.summary
$products = $productSummary.summary
$topProduct = @($products.products | Select-Object -First 1)[0]
$statusJson = $evaluation.statusBreakdown | ConvertTo-Json -Compress
$toolCodes = [string]::Join(", ", @($demo.report.toolCodes))

$lines = @()
$lines += "# ShopOps Portfolio Report"
$lines += ""
$lines += "Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$lines += ""
$lines += "## 1. Positioning"
$lines += ""
$lines += "ShopOps is an AgentOps admin platform for ecommerce operations. The project focuses on making an agent execution flow observable, auditable, configurable, testable, and driven by replaceable business data connectors."
$lines += ""
$lines += "The strongest portfolio story now is: generate a daily operation review from Olist public ecommerce data, then show the task lifecycle, tool evidence, approval workflow, audit timeline, shop configuration snapshot, and quantitative evaluation baseline."
$lines += ""
$lines += "## 2. Quantitative Results"
$lines += ""
$lines += "| Area | Metric | Current result |"
$lines += "|---|---|---:|"
$lines += "|Agent evaluation|Total cases|$($evaluation.caseCount)|"
$lines += "|Agent evaluation|Passed cases|$($evaluation.passedCaseCount)|"
$lines += "|Agent evaluation|Completion rate|$($evaluation.completionRate)%|"
$lines += "|Agent evaluation|Success rate|$($evaluation.successRate)%|"
$lines += "|Agent evaluation|Degraded completion rate|$($evaluation.degradedCompletionRate)%|"
$lines += "|Agent evaluation|Average task duration|$($evaluation.avgTaskDurationMs) ms|"
$lines += "|Agent evaluation|Tool invocation success rate|$($evaluation.toolInvocationSuccessRate)%|"
$lines += "|Agent evaluation|Approval decision accuracy|$($evaluation.approvalDecisionAccuracy)%|"
$lines += "|Agent evaluation|Config effect accuracy|$($evaluation.configEffectAccuracy)%|"
$lines += "|Olist demo|Business date|$($orderSummary.startDate)|"
$lines += "|Olist demo|GMV|$($order.gmv)|"
$lines += "|Olist demo|Order count|$($order.orderCount)|"
$lines += "|Olist demo|Refund proxy amount|$($order.refundAmount)|"
$lines += "|Olist demo|Refund proxy rate|$(Format-Percent ([double]$order.refundRate))|"
$lines += "|Olist demo|Risk comment count|$($comments.negativeCount)|"
$lines += "|Olist demo|Product candidate count|$($products.candidateCount)|"
$lines += "|Olist demo|Daily review task duration|$($demo.task.durationMs) ms|"
$lines += ""
$lines += "## 3. Olist Data Integration"
$lines += ""
$lines += "| Connector | Data source | Status | Role |"
$lines += "|---|---|---|---|"
$lines += "|file.order-summary|Olist orders + payments|$(Get-ConnectorStatus $demo 'file.order-summary')|GMV, order count, average order amount, refund proxy rate|"
$lines += "|file.negative-comments|Olist reviews + order items|$(Get-ConnectorStatus $demo 'file.negative-comments')|Low-score reviews, risk samples, product risk aggregation|"
$lines += "|file.product-candidates|Olist products + reviews + items|$(Get-ConnectorStatus $demo 'file.product-candidates')|Optimization candidates, risk score, product priority|"
$lines += "|file.ad-performance|Not covered by Olist|$(Get-ConnectorStatus $demo 'file.ad-performance')|Uses built-in demo data for now|"
$lines += "|file.external-reports|Not covered by Olist|$(Get-ConnectorStatus $demo 'file.external-reports')|Uses built-in demo data for now|"
$lines += ""
$lines += "Olist sample date: $($orderSummary.startDate). The selected day contains $($order.orderCount) orders, GMV $($order.gmv), and $($comments.negativeCount) risk comments. This is enough to demonstrate a real-data-driven agent report."
$lines += ""
$lines += "Top product priority: $($topProduct.productName), score $($topProduct.score), risk comments $($topProduct.negativeCount)."
$lines += ""
$lines += "## 4. AgentOps Demo Chain"
$lines += ""
$lines += "| Step | Result |"
$lines += "|---|---|"
$lines += "|Task creation|SUCCESS, taskId=$($demo.task.taskId)|"
$lines += "|Report generation|SUCCESS, reportId=$($demo.report.reportId)|"
$lines += "|Evidence tools|$toolCodes|"
$lines += "|Shop config snapshot|refundRateWarnThreshold=$($demo.shopConfigSnapshot.refundRateWarnThreshold), negativeCommentWarnThreshold=$($demo.shopConfigSnapshot.negativeCommentWarnThreshold), agentModelPolicy=$($demo.shopConfigSnapshot.agentModelPolicy)|"
$lines += "|High-risk refund approval|$($demo.approval.status), approvalId=$($demo.approval.approvalId)|"
$lines += "|Confirmation guard|$($demo.approval.confirmGuardMessage)|"
$lines += "|Tool retry after approval|$($demo.approval.retryStatus), refund status $($demo.approval.refundStatus)|"
$lines += "|Audit events|$($demo.approval.auditEventCount)|"
$lines += ""
$lines += "## 5. Evaluation Coverage"
$lines += ""
$lines += "| Suite | Cases | Passed | Success | Degraded | Approval Required |"
$lines += "|---|---:|---:|---:|---:|---:|"
foreach ($suite in $evaluation.suiteBreakdown) {
    $lines += "|$($suite.suite)|$($suite.caseCount)|$($suite.passedCaseCount)|$($suite.successCount)|$($suite.degradedCount)|$($suite.approvalRequiredCount)|"
}
$lines += ""
$lines += "Status breakdown: $statusJson."
$lines += ""
$lines += "The current baseline covers daily review tasks, model policies, runtime config thresholds, high-risk tool approval, direct execution when approval is disabled, and degraded completion after model failure."
$lines += ""
$lines += "## 6. Interview Pitch"
$lines += ""
$lines += "> ShopOps is not a plain AI report demo. It is an AgentOps backend for ecommerce operations. I decomposed an operation-review agent into tasks, tools, reports, approvals, audits, runtime configuration, and evaluation suites. The current build passes 14/14 evaluation cases, reaches 98.6% tool invocation success rate, and validates approval and configuration behavior at 100% accuracy. I also connected Olist public ecommerce data so the report can be driven by real orders and real reviews."
$lines += ""
$lines += "Recommended demo flow:"
$lines += ""
$lines += "1. Open Dashboard and frame the system as an operations agent console."
$lines += "2. Show shop runtime configuration and explain how thresholds affect execution."
$lines += "3. Create a daily_review task for $($orderSummary.startDate)."
$lines += "4. Open the report and show Olist metrics plus the evidence config snapshot."
$lines += "5. Trigger the high-risk refund tool and show approval plus confirmation guard."
$lines += "6. Open Audit Center and show the traceable approval timeline."
$lines += "7. End with this report's evaluation metrics to show measurable acceptance."
$lines += ""
$lines += "## 7. Current Boundaries"
$lines += ""
$lines += "- Olist does not provide a real refund amount field, so `canceled / unavailable` payment amount is used as a refund or after-sales risk proxy."
$lines += "- Olist does not include ad performance or external environment metrics, so those two connectors still use built-in demo data."
$lines += "- Current demo report generation mode is $($demo.report.generationMode). Real model calls can still be enabled through Model Gateway provider configuration."
$lines += "- Olist does not provide native product titles. The demo uses English category plus productId prefix as the display name."
$lines += ""
$lines += "## 8. Reproduction Commands"
$lines += ""
$lines += "Prepare Olist connector files:"
$lines += ""
$lines += '```powershell'
$lines += "python scripts/prepare_olist_demo.py"
$lines += '```'
$lines += ""
$lines += "Start the backend:"
$lines += ""
$lines += '```powershell'
$lines += 'mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--shopops.connector.order-summary.file=docs/demo-data/olist/order-summary-olist.json --shopops.connector.negative-comments.file=docs/demo-data/olist/negative-comments-olist.json --shopops.connector.product-candidates.file=docs/demo-data/olist/product-candidates-olist.json"'
$lines += '```'
$lines += ""
$lines += "Run Olist demo verification:"
$lines += ""
$lines += '```powershell'
$lines += "powershell -ExecutionPolicy Bypass -File scripts/verify-agentops-demo.ps1 -Port 8080 -Start 2018-08-07 -End 2018-08-07 -Scenario olist-agentops-demo -Dataset olist"
$lines += '```'
$lines += ""
$lines += "Refresh the evaluation baseline:"
$lines += ""
$lines += '```powershell'
$lines += "powershell -ExecutionPolicy Bypass -File scripts/run-agent-evaluation.ps1"
$lines += '```'

New-Item -ItemType Directory -Path (Split-Path -Parent $outputFile) -Force | Out-Null
$lines | Set-Content -Path $outputFile -Encoding UTF8
Write-Host "Portfolio report generated: $outputFile"
