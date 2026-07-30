param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$OutputDir = "docs/evaluation"
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $workspaceRoot $OutputDir
$summaryPath = Join-Path $outputPath "mcp-server-verification-summary.json"
$markdownPath = Join-Path $outputPath "mcp-server-verification-summary.md"
$BaseUrl = $BaseUrl.TrimEnd("/")

if (-not (Test-Path $outputPath)) {
    New-Item -ItemType Directory -Path $outputPath | Out-Null
}

$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

function ConvertTo-CompactJson {
    param($Value)
    return ($Value | ConvertTo-Json -Depth 30 -Compress)
}

function Invoke-Mcp {
    param(
        [string]$Id,
        [string]$Method,
        [object]$Params = $null
    )

    $payload = [ordered]@{
        jsonrpc = "2.0"
        id = $Id
        method = $Method
    }
    if ($null -ne $Params) {
        $payload.params = $Params
    }

    $body = $payload | ConvertTo-Json -Depth 30
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest `
            -Uri "$BaseUrl/mcp" `
            -Method Post `
            -Headers $headers `
            -Body ([System.Text.Encoding]::UTF8.GetBytes($body)) `
            -UseBasicParsing `
            -TimeoutSec 20
        $sw.Stop()
        $json = $response.Content | ConvertFrom-Json
        return [PSCustomObject]@{
            id = $Id
            method = $Method
            ok = ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300 -and $null -eq $json.error)
            statusCode = $response.StatusCode
            protocolVersion = $response.Headers["MCP-Protocol-Version"]
            durationMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            response = $json
            errorMessage = $null
        }
    } catch {
        $sw.Stop()
        return [PSCustomObject]@{
            id = $Id
            method = $Method
            ok = $false
            statusCode = $null
            protocolVersion = $null
            durationMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
            response = $null
            errorMessage = $_.Exception.Message
        }
    }
}

function Assert-Step {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Detail
    )
    return [PSCustomObject]@{
        name = $Name
        passed = $Passed
        detail = $Detail
    }
}

Write-Host "ShopOps MCP Server verification"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "Output: $OutputDir"
Write-Host ""

$initialize = Invoke-Mcp -Id "init-1" -Method "initialize"
$toolsList = Invoke-Mcp -Id "tools-1" -Method "tools/list"
$normalCall = Invoke-Mcp -Id "call-order-1" -Method "tools/call" -Params @{
    name = "order.query_summary"
    arguments = @{
        startDate = "2018-08-07"
        endDate = "2018-08-07"
    }
}
$approvalCall = Invoke-Mcp -Id "call-risk-1" -Method "tools/call" -Params @{
    name = "ad.suggest_budget"
    arguments = @{
        campaignId = "AD-LOW-001"
        changePercent = -20
    }
}

$tools = @()
if ($toolsList.ok -and $toolsList.response.result.tools) {
    $tools = @($toolsList.response.result.tools)
}

$normalStructured = $null
if ($normalCall.ok -and $normalCall.response.result.structuredContent) {
    $normalStructured = $normalCall.response.result.structuredContent
}

$approvalStructured = $null
if ($approvalCall.ok -and $approvalCall.response.result.structuredContent) {
    $approvalStructured = $approvalCall.response.result.structuredContent
}

$steps = @(
    (Assert-Step `
        -Name "initialize" `
        -Passed ($initialize.ok -and $initialize.response.result.protocolVersion -eq "2026-07-28" -and $initialize.response.result.capabilities.tools) `
        -Detail "protocol=$($initialize.response.result.protocolVersion), latency=$($initialize.durationMs)ms"),
    (Assert-Step `
        -Name "tools/list" `
        -Passed ($toolsList.ok -and $tools.Count -ge 18 -and @($tools | Where-Object { $_.name -eq "order.query_summary" }).Count -eq 1) `
        -Detail "toolCount=$($tools.Count), latency=$($toolsList.durationMs)ms"),
    (Assert-Step `
        -Name "tools/call read-only tool" `
        -Passed ($normalCall.ok -and $normalCall.response.result.isError -eq $false -and $normalStructured.success -eq $true -and $normalStructured.status -eq "SUCCESS") `
        -Detail "tool=order.query_summary, status=$($normalStructured.status), latency=$($normalCall.durationMs)ms"),
    (Assert-Step `
        -Name "tools/call high-risk approval path" `
        -Passed ($approvalCall.ok -and $approvalCall.response.result.isError -eq $true -and $approvalStructured.status -eq "APPROVAL_REQUIRED") `
        -Detail "tool=ad.suggest_budget, status=$($approvalStructured.status), latency=$($approvalCall.durationMs)ms")
)

$passedCount = @($steps | Where-Object { $_.passed }).Count
$requestDurations = @($initialize.durationMs, $toolsList.durationMs, $normalCall.durationMs, $approvalCall.durationMs)
$avgLatency = [math]::Round(($requestDurations | Measure-Object -Average).Average, 2)
$protocolVersions = @($initialize.protocolVersion, $toolsList.protocolVersion, $normalCall.protocolVersion, $approvalCall.protocolVersion) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

$summary = [ordered]@{
    evidenceName = "shopops-mcp-server-verification-v1"
    generatedAt = (Get-Date).ToString("o")
    baseUrl = $BaseUrl
    endpoint = "/mcp"
    transport = "HTTP JSON-RPC"
    protocolVersions = @($protocolVersions)
    totalChecks = $steps.Count
    passedChecks = $passedCount
    successRate = [math]::Round($passedCount * 100.0 / $steps.Count, 2)
    toolCount = $tools.Count
    avgLatencyMs = $avgLatency
    checkedMethods = @("initialize", "tools/list", "tools/call")
    checkedTools = @("order.query_summary", "ad.suggest_budget")
    checks = $steps
    claimBoundary = "This verifies the Spring Boot embedded HTTP JSON-RPC MCP endpoint. It does not verify stdio or SSE transport."
}

$summary | ConvertTo-Json -Depth 30 | Set-Content -Encoding UTF8 -Path $summaryPath

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# ShopOps MCP Server Verification")
$lines.Add("")
$lines.Add("| Metric | Value |")
$lines.Add("| --- | --- |")
$lines.Add("| Endpoint | `/mcp` |")
$lines.Add("| Transport | HTTP JSON-RPC |")
$lines.Add("| Protocol versions | $(@($protocolVersions) -join ', ') |")
$lines.Add("| Tool count | $($tools.Count) |")
$lines.Add("| Passed checks | $passedCount / $($steps.Count) |")
$lines.Add("| Success rate | $($summary.successRate)% |")
$lines.Add("| Average latency | $avgLatency ms |")
$lines.Add("")
$lines.Add("## Checks")
$lines.Add("")
$lines.Add("| Check | Result | Detail |")
$lines.Add("| --- | --- | --- |")
foreach ($step in $steps) {
    $result = if ($step.passed) { "PASS" } else { "FAIL" }
    $lines.Add("| $($step.name) | $result | $($step.detail) |")
}
$lines.Add("")
$lines.Add("## Boundary")
$lines.Add("")
$lines.Add($summary.claimBoundary)
$lines | Set-Content -Encoding UTF8 -Path $markdownPath

foreach ($step in $steps) {
    $prefix = if ($step.passed) { "[PASS]" } else { "[FAIL]" }
    Write-Host "$prefix $($step.name) - $($step.detail)"
}

Write-Host ""
Write-Host "Summary: $summaryPath"
Write-Host "Markdown: $markdownPath"

if ($passedCount -ne $steps.Count) {
    throw "MCP verification failed: $passedCount/$($steps.Count) checks passed."
}
