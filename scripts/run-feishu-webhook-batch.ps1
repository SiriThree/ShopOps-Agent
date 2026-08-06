param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateRange(1, 500)]
    [int]$Count = 20,
    [int]$DelayMs = 300,
    [string]$OutputDir = "docs/evaluation",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function New-DirectoryIfMissing {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function ConvertTo-CompactJson {
    param($Value)
    return ($Value | ConvertTo-Json -Depth 20 -Compress)
}

function Get-ResponseValue {
    param($Object, [string]$Name)
    if ($null -eq $Object) {
        return $null
    }
    if ($Object -is [System.Collections.IDictionary] -and $Object.Contains($Name)) {
        return $Object[$Name]
    }
    return $Object.$Name
}

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $workspaceRoot $OutputDir
$summaryPath = Join-Path $outputPath "feishu-webhook-batch-summary.json"
$detailPath = Join-Path $outputPath "feishu-webhook-batch-details.csv"
$startedAt = (Get-Date).ToString("o")

New-DirectoryIfMissing -Path $outputPath

Write-Host "ShopOps Feishu webhook batch verification"
Write-Host "BaseUrl: $BaseUrl"
Write-Host "Count: $Count"
Write-Host "Output: $OutputDir"

if ($DryRun) {
    Write-Host "Dry run only. No request will be sent."
    exit 0
}

if ($env:SHOPOPS_FEISHU_SYNC_ENABLED -ne "true") {
    Write-Warning "Current shell does not have SHOPOPS_FEISHU_SYNC_ENABLED=true. Make sure the backend was started with Feishu webhook mode enabled."
}
if ([string]::IsNullOrWhiteSpace($env:SHOPOPS_FEISHU_SYNC_WEBHOOK_URL)) {
    Write-Warning "Current shell does not have SHOPOPS_FEISHU_SYNC_WEBHOOK_URL. Do not paste the webhook into Git; set it as a local environment variable before starting the backend."
}

$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

$records = New-Object System.Collections.Generic.List[object]
$successCount = 0
$webhookModeCount = 0
$http200Count = 0
$latencies = New-Object System.Collections.Generic.List[double]

for ($i = 1; $i -le $Count; $i++) {
    $reportId = 990000 + $i
    $body = @{
        shopId = 1
        reportId = $reportId
        documentUrl = "$BaseUrl/admin/reports.html?batch=feishu-webhook&reportId=$reportId"
    } | ConvertTo-Json -Depth 10

    $attemptStarted = Get-Date
    $ok = $false
    $mode = ""
    $status = ""
    $webhookStatusCode = ""
    $errorMessage = ""

    try {
        $response = Invoke-RestMethod `
            -Uri "$BaseUrl/api/tools/feishu.sync_report/invoke" `
            -Method Post `
            -Headers $headers `
            -Body $body `
            -TimeoutSec 15

        $data = Get-ResponseValue -Object $response -Name "data"
        $toolResult = Get-ResponseValue -Object $data -Name "data"
        $success = Get-ResponseValue -Object $data -Name "success"
        $status = [string](Get-ResponseValue -Object $data -Name "status")
        $mode = [string](Get-ResponseValue -Object $toolResult -Name "mode")
        $webhookStatusCode = [string](Get-ResponseValue -Object $toolResult -Name "webhookStatusCode")
        $ok = ($success -eq $true -and $status -eq "SUCCESS" -and $mode -eq "feishu-webhook" -and $webhookStatusCode -eq "200")
    } catch {
        $errorMessage = $_.Exception.Message
    }

    $durationMs = ((Get-Date) - $attemptStarted).TotalMilliseconds
    $latencies.Add([Math]::Round($durationMs, 2))

    if ($ok) {
        $successCount++
    }
    if ($mode -eq "feishu-webhook") {
        $webhookModeCount++
    }
    if ($webhookStatusCode -eq "200") {
        $http200Count++
    }

    $record = [PSCustomObject]@{
        index = $i
        reportId = $reportId
        success = $ok
        status = $status
        mode = $mode
        webhookStatusCode = $webhookStatusCode
        durationMs = [Math]::Round($durationMs, 2)
        errorMessage = $errorMessage
        requestedAt = $attemptStarted.ToString("o")
    }
    $records.Add($record)

    Write-Host ("[{0}/{1}] success={2} mode={3} webhookStatus={4} durationMs={5}" -f $i, $Count, $ok, $mode, $webhookStatusCode, [Math]::Round($durationMs, 2))

    if ($DelayMs -gt 0 -and $i -lt $Count) {
        Start-Sleep -Milliseconds $DelayMs
    }
}

$avgLatency = if ($latencies.Count -gt 0) { [Math]::Round(($latencies | Measure-Object -Average).Average, 2) } else { 0 }
$minLatency = if ($latencies.Count -gt 0) { [Math]::Round(($latencies | Measure-Object -Minimum).Minimum, 2) } else { 0 }
$maxLatency = if ($latencies.Count -gt 0) { [Math]::Round(($latencies | Measure-Object -Maximum).Maximum, 2) } else { 0 }
$successRate = [Math]::Round($successCount * 100.0 / $Count, 2)
$webhookModeRate = [Math]::Round($webhookModeCount * 100.0 / $Count, 2)
$http200Rate = [Math]::Round($http200Count * 100.0 / $Count, 2)

$summary = [ordered]@{
    evidenceName = "shopops-feishu-webhook-batch-v1"
    generatedAt = (Get-Date).ToString("o")
    startedAt = $startedAt
    baseUrl = $BaseUrl
    requestCount = $Count
    successCount = $successCount
    successRate = $successRate
    webhookModeCount = $webhookModeCount
    webhookModeRate = $webhookModeRate
    http200Count = $http200Count
    http200Rate = $http200Rate
    avgLatencyMs = $avgLatency
    minLatencyMs = $minLatency
    maxLatencyMs = $maxLatency
    detailFile = "docs/evaluation/feishu-webhook-batch-details.csv"
    claimBoundary = "This verifies configured Feishu webhook HTTP delivery through ShopOps. Keep the Feishu group screenshot and backend logs as external evidence."
}

$records | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $detailPath
Set-Content -Encoding UTF8 -Path $summaryPath -Value (ConvertTo-CompactJson $summary)

Write-Host ""
Write-Host "Batch verification completed."
Write-Host "Success rate: $successRate% ($successCount/$Count)"
Write-Host "Webhook mode rate: $webhookModeRate% ($webhookModeCount/$Count)"
Write-Host "HTTP 200 rate: $http200Rate% ($http200Count/$Count)"
Write-Host "Average latency: $avgLatency ms"
Write-Host "Summary: $summaryPath"
Write-Host "Details: $detailPath"
