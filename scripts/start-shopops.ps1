param(
    [int]$Port = 8080,
    [switch]$SkipPrepareData,
    [switch]$SkipInstallCommon,
    [switch]$OpenBrowser,
    [switch]$NoOpenBrowser,
    [switch]$StrictPort,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$demoDate = "2018-08-07"
$demoTask = "Generate the 2018-08-07 Olist shop operation daily report"

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

function Test-PortAvailable {
    param([int]$CandidatePort)
    $listener = $null
    try {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $CandidatePort)
        $listener.Start()
        return $true
    } catch {
        return $false
    } finally {
        if ($null -ne $listener) {
            $listener.Stop()
        }
    }
}

function Resolve-Port {
    param([int]$RequestedPort)
    if (Test-PortAvailable -CandidatePort $RequestedPort) {
        return $RequestedPort
    }
    if ($StrictPort) {
        throw "Port $RequestedPort is already in use. Stop the existing process or rerun with -Port <anotherPort>."
    }
    for ($candidate = $RequestedPort + 1; $candidate -le $RequestedPort + 20; $candidate++) {
        if (Test-PortAvailable -CandidatePort $candidate) {
            Write-Warning "Port $RequestedPort is already in use. Using port $candidate instead."
            return $candidate
        }
    }
    throw "No available port found from $RequestedPort to $($RequestedPort + 20)."
}

function Start-WorkbenchOpener {
    param([string]$Url)
    Start-Job -ScriptBlock {
        param($TargetUrl)
        for ($i = 0; $i -lt 60; $i++) {
            try {
                $response = Invoke-WebRequest -Uri $TargetUrl -UseBasicParsing -TimeoutSec 2
                if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                    Start-Process $TargetUrl
                    return
                }
            } catch {
                Start-Sleep -Seconds 2
            }
        }
    } -ArgumentList $Url | Out-Null
}

Set-Location $workspaceRoot
$Port = Resolve-Port -RequestedPort $Port
$workbenchUrl = "http://localhost:$Port/admin/workbench.html"

Write-Host "ShopOps one-command launcher"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Workbench: $workbenchUrl"
Write-Host "Demo date: $demoDate"
Write-Host "Demo task: $demoTask"

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

if ((($OpenBrowser -or -not $NoOpenBrowser)) -and -not $DryRun) {
    Start-WorkbenchOpener -Url $workbenchUrl
}

Write-Host ""
Write-Host "Starting ShopOps Admin..."
Write-Host "Open after startup: $workbenchUrl"
Write-Host "Recommended demo date: $demoDate"
Write-Host "Recommended demo task: $demoTask"
Write-Host "Press Ctrl+C to stop the server."
Write-Host ""

if (-not $DryRun) {
    mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
}
