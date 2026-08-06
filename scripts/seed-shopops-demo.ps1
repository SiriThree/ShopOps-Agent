param(
    [int]$Port = 8080,
    [string]$Scenario = "interview-demo",
    [switch]$NoOpenBrowser,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$baseUrl = "http://localhost:$Port"
$demoDate = "2018-08-07"
$summaryPath = Join-Path $workspaceRoot "shopops-admin\target\demo\$Scenario-summary.json"

Set-Location $workspaceRoot

Write-Host "ShopOps interview demo seeder"
Write-Host "BaseUrl: $baseUrl"
Write-Host "Scenario: $Scenario"
Write-Host "Demo date: $demoDate"
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN] Wait for $baseUrl/api/system/health"
    Write-Host "[DRY RUN] Run scripts/check-shopops.ps1 -Port $Port"
    Write-Host "[DRY RUN] Run scripts/verify-agentops-demo.ps1 -Port $Port -Start $demoDate -End $demoDate -Scenario $Scenario -Dataset olist"
    Write-Host "[DRY RUN] Validate $summaryPath"
    exit 0
}

$headers = @{
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

Write-Host "1. Waiting for ShopOps..."
$ready = $false
for ($attempt = 1; $attempt -le 30; $attempt++) {
    try {
        $response = Invoke-WebRequest -Uri "$baseUrl/api/system/health" -Headers $headers -UseBasicParsing -TimeoutSec 2
        if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
            $ready = $true
            break
        }
    } catch {
        if ($attempt -eq 30) {
            break
        }
    }
    Start-Sleep -Seconds 1
}
if (-not $ready) {
    throw "ShopOps is not ready at $baseUrl. Start it with scripts/start-shopops.ps1 first."
}
Write-Host "   ShopOps is ready."

Write-Host ""
Write-Host "2. Running health check..."
& powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\check-shopops.ps1" -Port $Port
if ($LASTEXITCODE -ne 0) {
    throw "ShopOps health check failed."
}

Write-Host ""
Write-Host "3. Creating the complete Agent demo chain..."
& powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\verify-agentops-demo.ps1" `
    -Port $Port `
    -Start $demoDate `
    -End $demoDate `
    -Scenario $Scenario `
    -Dataset "olist"
if ($LASTEXITCODE -ne 0) {
    throw "AgentOps demo verification failed."
}

if (-not (Test-Path -LiteralPath $summaryPath)) {
    throw "Demo summary was not generated: $summaryPath"
}

$summary = Get-Content -LiteralPath $summaryPath -Raw -Encoding UTF8 | ConvertFrom-Json
$validTaskStatus = @("SUCCESS", "DEGRADED") -contains [string]$summary.task.status
if (-not $validTaskStatus) {
    throw "Unexpected task status: $($summary.task.status)"
}
if ([string]$summary.report.status -ne "SUCCESS") {
    throw "Unexpected report status: $($summary.report.status)"
}
if ([string]$summary.approval.status -ne "APPROVED") {
    throw "Unexpected approval status: $($summary.approval.status)"
}
if ([string]$summary.approval.retryStatus -ne "SUCCESS") {
    throw "Unexpected tool retry status: $($summary.approval.retryStatus)"
}

Write-Host ""
Write-Host "Demo chain is ready."
Write-Host "Task:      $($summary.task.status) (#$($summary.task.taskId))"
Write-Host "Report:    $($summary.report.status) (#$($summary.report.reportId))"
Write-Host "Approval:  $($summary.approval.status) (#$($summary.approval.approvalId))"
Write-Host "Workbench: $($summary.links.workbench)"
Write-Host "Tasks:     $($summary.links.tasks)"
Write-Host "Reports:   $($summary.links.reports)"
Write-Host "Tools:     $($summary.links.tools)"
Write-Host "Approvals: $($summary.links.approvals)"
Write-Host "Audit:     $($summary.links.audit)"
Write-Host "Summary:   $summaryPath"

if (-not $NoOpenBrowser) {
    Start-Process $summary.links.workbench
}
