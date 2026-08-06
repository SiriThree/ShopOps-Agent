param(
    [int]$Port = 8080,
    [string]$Start = "2026-07-18",
    [string]$End = "2026-07-18",
    [string]$Scenario = "agentops-demo",
    [string]$Dataset = "memory-default",
    [string]$Module = "shopops-admin"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$baseUrl = "http://localhost:$Port"
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$moduleRoot = Join-Path $workspaceRoot $Module
$demoOutputDir = Join-Path $moduleRoot "target\demo"
$confirmText = -join ([char]0x786E, [char]0x8BA4, [char]0x901A, [char]0x8FC7)

$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

function ConvertTo-Body($value) {
    return $value | ConvertTo-Json -Depth 8
}

function Get-PercentValue($text) {
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }
    if ($text -match '(-?\d+(\.\d+)?)%') {
        return [double]$matches[1]
    }
    return $null
}

function Get-SectionBulletLines {
    param(
        [string]$Markdown,
        [string]$SectionPrefix,
        [int]$MaxItems = 20
    )

    $lines = $Markdown -split "`r?`n"
    $items = New-Object System.Collections.Generic.List[string]
    $inSection = $false
    foreach ($line in $lines) {
        if ($line.StartsWith("## ")) {
            if ($inSection) {
                break
            }
            if ($line.StartsWith("## $SectionPrefix")) {
                $inSection = $true
            }
            continue
        }
        if (-not $inSection) {
            continue
        }
        if ($line.StartsWith("- ")) {
            $items.Add($line.Substring(2).Trim())
            if ($items.Count -ge $MaxItems) {
                break
            }
        }
    }
    return @($items)
}

function New-DemoSummaryMarkdown {
    param(
        [pscustomobject]$Summary
    )

    $lines = @()
    $lines += "# ShopOps AgentOps Demo Summary"
    $lines += ""
    $lines += "- Generated at: $($Summary.generatedAt)"
    $lines += "- Scenario: $($Summary.scenario)"
    $lines += "- Dataset: $($Summary.dataset)"
    $lines += "- Base URL: $($Summary.baseUrl)"
    $lines += "- Date range: $($Summary.dateRange.start) to $($Summary.dateRange.end)"
    $lines += "- Task status: $($Summary.task.status)"
    $lines += "- Report status: $($Summary.report.status)"
    $lines += "- Approval status: $($Summary.approval.status)"
    $lines += "- Refund retry status: $($Summary.approval.retryStatus)"
    $lines += ""
    $lines += "## Key Metrics"
    $lines += ""
    $lines += "| metric | value |"
    $lines += "|---|---|"
    foreach ($metric in $Summary.metrics.GetEnumerator()) {
        $lines += "|$($metric.Key)|$($metric.Value)|"
    }
    $lines += ""
    $lines += "## Shop Config Snapshot"
    $lines += ""
    $lines += "| key | value |"
    $lines += "|---|---|"
    foreach ($item in $Summary.shopConfigSnapshot.GetEnumerator()) {
        $lines += "|$($item.Key)|$($item.Value)|"
    }
    $lines += ""
    $lines += "## Connector Status"
    $lines += ""
    $lines += "| connector | status | configured | available | path |"
    $lines += "|---|---|---:|---:|---|"
    foreach ($item in $Summary.connectors) {
        $lines += "|$($item.connectorCode)|$($item.status)|$($item.configured)|$($item.available)|$($item.configuredPath)|"
    }
    $lines += ""
    $lines += "## Report Section 1"
    $lines += ""
    foreach ($item in $Summary.reportSections.coreMetrics) {
        $lines += "- $item"
    }
    $lines += ""
    $lines += "## Report Highlights"
    $lines += ""
    if ($Summary.reportHighlights.Count -eq 0) {
        $lines += "- None"
    } else {
        foreach ($item in $Summary.reportHighlights) {
            $lines += "- $item"
        }
    }
    $lines += ""
    $lines += "## Product Priorities"
    $lines += ""
    if ($Summary.productPriorities.Count -eq 0) {
        $lines += "- None"
    } else {
        foreach ($item in $Summary.productPriorities) {
            $lines += "- $item"
        }
    }
    $lines += ""
    $lines += "## Demo Links"
    $lines += ""
    foreach ($item in $Summary.links.GetEnumerator()) {
        $lines += "- $($item.Key): $($item.Value)"
    }
    return $lines -join "`r`n"
}

function Invoke-ShopOps {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [switch]$AllowFailure
    )

    $arguments = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        Headers = $headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) {
        $arguments.Body = [System.Text.Encoding]::UTF8.GetBytes((ConvertTo-Body $Body))
    }
    try {
        $response = Invoke-RestMethod @arguments
        if ($response.code -ne 200 -and -not $AllowFailure) {
            throw "API returned non-success payload: $($response | ConvertTo-Json -Depth 8)"
        }
        return $response
    } catch {
        if (-not $AllowFailure) {
            throw
        }
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            return $_.ErrorDetails.Message | ConvertFrom-Json
        }
        if ($_.Exception.Response) {
            $stream = $null
            try {
                $stream = $_.Exception.Response.GetResponseStream()
            } catch {
                $stream = $null
            }
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                if ($text) {
                    return $text | ConvertFrom-Json
                }
            }
        }
        return [pscustomobject]@{
            code = 500
            message = $_.Exception.Message
            data = $null
        }
    }
}

Write-Host "ShopOps AgentOps demo verification"
Write-Host "Base URL: $baseUrl"
Write-Host ""

Write-Host "1. Checking dashboard health..."
$dashboard = Invoke-ShopOps -Method Get -Path "/api/admin/dashboard/summary"
Write-Host "   Tasks: $($dashboard.data.taskMetrics.total), Reports: $($dashboard.data.reportTotal), Tool calls: $($dashboard.data.toolCallTotal)"

Write-Host "2. Creating daily_review task..."
$taskBody = @{
    taskType = "daily_review"
    userInput = "Generate a shop operation review and highlight refund, negative comment, and product optimization priorities."
    dateRange = @{
        start = $Start
        end = $End
    }
}
$createdTask = Invoke-ShopOps -Method Post -Path "/api/agent/tasks" -Body $taskBody
$taskId = $createdTask.data.taskId
Write-Host "   Task ID: $taskId, status: $($createdTask.data.status)"

$task = Invoke-ShopOps -Method Get -Path "/api/agent/tasks/$taskId"
$reportId = $task.data.reportId
Write-Host "   Report ID: $reportId"
if (-not $reportId) {
    throw "daily_review task did not generate a report."
}

$report = Invoke-ShopOps -Method Get -Path "/api/reports/$reportId"
$markdown = [string]$report.data.markdown
Write-Host "   Report markdown length: $($markdown.Length)"

Write-Host ""
Write-Host "3. Triggering high-risk refund tool..."
$approvalRequired = Invoke-ShopOps -Method Post -Path "/api/tools/order.refund_execute/invoke" -Body @{
    shopId = 1
    refundAmount = 1288
    reason = "AgentOps demo high risk refund approval"
}
$approvalId = $approvalRequired.data.approvalId
$toolCallLogId = $approvalRequired.data.toolCallLogId
Write-Host "   Approval ID: $approvalId, Tool call log ID: $toolCallLogId, status: $($approvalRequired.data.status)"

Write-Host "4. Verifying high-risk confirmation guard..."
$missingConfirm = Invoke-ShopOps -Method Post -Path "/api/admin/approvals/$approvalId/approve" -Body @{
    comment = "Approval without confirm text should fail"
} -AllowFailure
if ($missingConfirm.code -ne 400) {
    throw "High-risk confirmation guard did not take effect: $($missingConfirm | ConvertTo-Json -Depth 8)"
}
Write-Host "   Missing confirm rejected: $($missingConfirm.message)"

Write-Host "5. Approving with confirm text and retrying tool..."
$approved = Invoke-ShopOps -Method Post -Path "/api/admin/approvals/$approvalId/approve" -Body @{
    comment = "AgentOps demo approval granted"
    confirmText = $confirmText
}
Write-Host "   Approval status: $($approved.data.status), approver: $($approved.data.approverName)"

$retry = Invoke-ShopOps -Method Post -Path "/api/tools/order.refund_execute/invoke" -Body @{
    shopId = 1
    refundAmount = 1288
    reason = "AgentOps demo high risk refund approval"
    approvalId = $approvalId
}
Write-Host "   Retry status: $($retry.data.status), refund status: $($retry.data.data.status)"

Write-Host ""
Write-Host "6. Checking audit timeline..."
$auditDetail = Invoke-ShopOps -Method Get -Path "/api/admin/audit/timeline/APPROVAL/$approvalId"
Write-Host "   Approval audit detail event: $($auditDetail.data.event.eventType), status: $($auditDetail.data.event.eventStatus)"
$audit = Invoke-ShopOps -Method Get -Path "/api/admin/audit/timeline?source=APPROVAL&toolCode=order.refund_execute&pageNum=1&pageSize=10"
$currentApprovalEvents = @($audit.data.list | Where-Object { $_.resourceId -eq [string]$approvalId })
Write-Host "   Current approval events in timeline: $($currentApprovalEvents.Count)"
$currentApprovalEvents | Select-Object eventType, eventStatus, source, resourceId, riskLevel, createdAt | Format-Table -AutoSize

Write-Host ""
Write-Host "7. Collecting connector status and demo summary..."
$connectors = Invoke-ShopOps -Method Get -Path "/api/admin/connectors/status"
$connectorItems = @($connectors.data | ForEach-Object {
    [pscustomobject]@{
        connectorCode = [string]$_.connectorCode
        status = [string]$_.status
        configured = [bool]$_.configured
        available = [bool]$_.available
        configuredPath = [string]$_.configuredPath
    }
})

$section1 = Get-SectionBulletLines -Markdown $markdown -SectionPrefix "1." -MaxItems 8
$section6 = Get-SectionBulletLines -Markdown $markdown -SectionPrefix "6." -MaxItems 5
$section7 = Get-SectionBulletLines -Markdown $markdown -SectionPrefix "7." -MaxItems 5
$section4 = Get-SectionBulletLines -Markdown $markdown -SectionPrefix "4." -MaxItems 4

$metrics = [ordered]@{
    gmv = if ($section1.Count -ge 1) { $section1[0] } else { $null }
    orderCount = if ($section1.Count -ge 2) { $section1[1] } else { $null }
    refundAmount = if ($section1.Count -ge 3) { $section1[2] } else { $null }
    refundRate = if ($section1.Count -ge 4) { $section1[3] } else { $null }
    avgOrderAmount = if ($section1.Count -ge 5) { $section1[4] } else { $null }
    riskSummary = if ($section4.Count -ge 1) { $section4[0] } else { $null }
    productSummary = if ($section4.Count -ge 2) { $section4[1] } else { $null }
}

$shopConfigSnapshot = [ordered]@{}
if ($report.data.evidence -and $report.data.evidence.shopConfig) {
    foreach ($property in $report.data.evidence.shopConfig.PSObject.Properties) {
        $shopConfigSnapshot[$property.Name] = [string]$property.Value
    }
}

$taskDurationMs = $null
if ($task.data.startedAt -and $task.data.finishedAt) {
    $taskDurationMs = [math]::Round(((Get-Date $task.data.finishedAt) - (Get-Date $task.data.startedAt)).TotalMilliseconds, 1)
}

$demoLinks = [ordered]@{
    workbench = "$baseUrl/admin/workbench.html"
    dashboard = "$baseUrl/admin/dashboard.html"
    tasks = "$baseUrl/admin/tasks.html?taskId=$taskId"
    reports = "$baseUrl/admin/reports.html?reportId=$reportId"
    approvals = "$baseUrl/admin/approvals.html?approvalId=$approvalId"
    audit = "$baseUrl/admin/audit.html?source=APPROVAL&resourceId=$approvalId"
    tools = "$baseUrl/admin/tools.html?toolCode=order.refund_execute"
}

$demoSummary = [pscustomobject]@{
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    scenario = $Scenario
    dataset = $Dataset
    baseUrl = $baseUrl
    dateRange = [ordered]@{
        start = $Start
        end = $End
    }
    task = [ordered]@{
        taskId = $taskId
        taskNo = [string]$task.data.taskNo
        status = [string]$task.data.status
        traceId = [string]$task.data.traceId
        durationMs = $taskDurationMs
    }
    report = [ordered]@{
        reportId = $reportId
        reportNo = [string]$report.data.reportNo
        status = [string]$report.data.status
        markdownLength = $markdown.Length
        generationMode = [string]$report.data.evidence.generationMode
        toolCodes = @($report.data.evidence.toolCodes)
        riskCommentIds = @($report.data.evidence.riskCommentIds)
        productIds = @($report.data.evidence.productIds)
        campaignNames = @($report.data.evidence.campaignNames)
        channelNames = @($report.data.evidence.channelNames)
    }
    approval = [ordered]@{
        approvalId = $approvalId
        status = [string]$approved.data.status
        confirmGuardMessage = [string]$missingConfirm.message
        retryStatus = [string]$retry.data.status
        refundStatus = [string]$retry.data.data.status
        auditEventCount = $currentApprovalEvents.Count
    }
    metrics = $metrics
    metricNumbers = [ordered]@{
        refundRatePercent = Get-PercentValue $metrics.refundRate
    }
    shopConfigSnapshot = $shopConfigSnapshot
    reportSections = [ordered]@{
        coreMetrics = @($section1)
        anomalySummary = @($section4)
    }
    reportHighlights = @($section7)
    productPriorities = @($section6)
    connectors = $connectorItems
    links = $demoLinks
}

New-Item -ItemType Directory -Path $demoOutputDir -Force | Out-Null
$summaryJsonPath = Join-Path $demoOutputDir "$Scenario-summary.json"
$summaryMdPath = Join-Path $demoOutputDir "$Scenario-summary.md"
$demoSummary | ConvertTo-Json -Depth 10 | Set-Content -Path $summaryJsonPath -Encoding UTF8
New-DemoSummaryMarkdown -Summary $demoSummary | Set-Content -Path $summaryMdPath -Encoding UTF8
Write-Host "   Summary JSON: $summaryJsonPath"
Write-Host "   Summary MD:   $summaryMdPath"

Write-Host ""
Write-Host "Demo links:"
Write-Host "   Workbench:  $($demoLinks.workbench)"
Write-Host "   Dashboard:  $($demoLinks.dashboard)"
Write-Host "   Tasks:      $($demoLinks.tasks)"
Write-Host "   Reports:    $($demoLinks.reports)"
Write-Host "   Approvals:  $($demoLinks.approvals)"
Write-Host "   Audit:      $($demoLinks.audit)"
Write-Host "   Tool logs:  $($demoLinks.tools)"
Write-Host ""
Write-Host "AgentOps demo verification completed."
