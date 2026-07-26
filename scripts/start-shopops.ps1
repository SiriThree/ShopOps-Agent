param(
    [int]$Port = 8080,
    [switch]$SkipPrepareData,
    [switch]$SkipInstallCommon,
    [switch]$OpenBrowser,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$workbenchUrl = "http://localhost:$Port/admin/workbench.html"

function Require-Command {
    param(
        [string]$Name,
        [string]$InstallHint
    )
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing command '$Name'. $InstallHint"
    }
}

function Invoke-Step {
    param(
        [string]$Title,
        [scriptblock]$Action
    )
    Write-Host ""
    Write-Host "==> $Title"
    if (-not $DryRun) {
        & $Action
    }
}

Set-Location $workspaceRoot

Write-Host "ShopOps one-command launcher"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Workbench: $workbenchUrl"

Require-Command -Name "mvn" -InstallHint "Install Maven 3.9+ and make sure it is available in PATH."
Require-Command -Name "python" -InstallHint "Install Python 3.10+ and make sure it is available in PATH."

if (-not $SkipPrepareData) {
    Invoke-Step -Title "Prepare Olist demo data" -Action {
        python scripts/prepare_olist_demo.py
    }
}

if (-not $SkipInstallCommon) {
    Invoke-Step -Title "Install shopops-common locally" -Action {
        mvn -pl shopops-common install -DskipTests
    }
}

if ($OpenBrowser -and -not $DryRun) {
    Start-Job -ScriptBlock {
        param($Url)
        Start-Sleep -Seconds 8
        Start-Process $Url
    } -ArgumentList $workbenchUrl | Out-Null
}

Write-Host ""
Write-Host "Starting ShopOps Admin..."
Write-Host "Open after startup: $workbenchUrl"
Write-Host "Recommended demo date: 2018-08-07"
Write-Host "Press Ctrl+C to stop the server."
Write-Host ""

if (-not $DryRun) {
    mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
}
