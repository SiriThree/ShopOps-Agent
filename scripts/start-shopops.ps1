param(
    [int]$Port = 8080,
    [switch]$SkipPrepareData,
    [switch]$SkipInstallCommon,
    [switch]$OpenBrowser,
    [switch]$NoOpenBrowser,
    [switch]$StrictPort,
    [switch]$Jdbc,
    [switch]$Memory,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$demoDate = "2018-08-07"
$demoTask = "Generate the 2018-08-07 Olist shop operation daily report"
$defaultDatasourceUrl = "jdbc:mysql://localhost:3306/shopops_agent?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$devComposeFile = Join-Path $workspaceRoot "deploy\docker-compose.dev.yml"

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

function Test-TcpConnection {
    param(
        [string]$HostName,
        [int]$PortNumber
    )
    $client = $null
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $connection = $client.BeginConnect($HostName, $PortNumber, $null, $null)
        if (-not $connection.AsyncWaitHandle.WaitOne(1000, $false)) {
            return $false
        }
        $client.EndConnect($connection)
        return $true
    } catch {
        return $false
    } finally {
        if ($null -ne $client) {
            $client.Close()
        }
    }
}

function Wait-TcpConnection {
    param(
        [string]$HostName,
        [int]$PortNumber,
        [int]$Seconds
    )
    for ($i = 0; $i -lt $Seconds; $i++) {
        if (Test-TcpConnection -HostName $HostName -PortNumber $PortNumber) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Ensure-JdbcDependency {
    if ($env:SHOPOPS_DATASOURCE_URL -and -not $env:SHOPOPS_DATASOURCE_URL.Contains("localhost:3306")) {
        Write-Host "Custom datasource configured. Skipping local MySQL auto-start."
        return
    }
    if (Test-TcpConnection -HostName "localhost" -PortNumber 3306) {
        Write-Host "Local MySQL is reachable at localhost:3306."
        return
    }
    if (-not (Get-Command "docker" -ErrorAction SilentlyContinue)) {
        throw "JDBC persistence requires MySQL at localhost:3306. Install/start MySQL, or rerun with -Memory for temporary in-memory mode."
    }

    Write-Host "Local MySQL is not reachable. Trying to start deploy/docker-compose.dev.yml mysql..."
    docker info | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "JDBC persistence requires MySQL, but Docker Desktop is not running. Start Docker Desktop and rerun, or rerun with -Memory for temporary in-memory mode."
    }

    docker compose -p shopops-dev -f $devComposeFile up -d mysql
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start local MySQL container. Start MySQL manually or rerun with -Memory."
    }
    if (-not (Wait-TcpConnection -HostName "localhost" -PortNumber 3306 -Seconds 60)) {
        throw "MySQL container was started but localhost:3306 is still unreachable after 60 seconds."
    }
    Write-Host "Local MySQL is ready at localhost:3306."
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
if ($Jdbc -and $Memory) {
    throw "Use either -Jdbc or -Memory, not both."
}
$useJdbc = -not $Memory
$Port = Resolve-Port -RequestedPort $Port
$workbenchUrl = "http://localhost:$Port/admin/workbench.html"

Write-Host "ShopOps one-command launcher"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Workbench: $workbenchUrl"
Write-Host "Demo date: $demoDate"
Write-Host "Demo task: $demoTask"
if ($useJdbc) {
    $datasourceUrl = if ($env:SHOPOPS_DATASOURCE_URL) { $env:SHOPOPS_DATASOURCE_URL } else { $defaultDatasourceUrl }
    Write-Host "Persistence: JDBC / MySQL"
    Write-Host "Database URL: $datasourceUrl"
} else {
    Write-Host "Persistence: memory (records are cleared after backend restart)"
}

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

if ($useJdbc) {
    Invoke-Step -Title "Ensure persistent MySQL storage" -Action {
        Ensure-JdbcDependency
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
Write-Host "Seed a complete demo chain in another terminal:"
Write-Host "powershell -ExecutionPolicy Bypass -File scripts/seed-shopops-demo.ps1 -Port $Port"
if (-not $useJdbc) {
    Write-Host "Need records after restart? Rerun this launcher without -Memory and use a reachable MySQL database."
}
Write-Host "Press Ctrl+C to stop the server."
Write-Host ""

if (-not $DryRun) {
    if ($useJdbc) {
        mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.arguments=--server.port=$Port"
    } else {
        mvn -pl shopops-admin spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port --shopops.persistence=memory --shopops.flyway.enabled=false"
    }
}
