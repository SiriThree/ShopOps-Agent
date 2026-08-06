param(
    [int]$Port = 8080,
    [switch]$NoSeed,
    [switch]$NoOpenBrowser,
    [switch]$UseChinaMirror,
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $workspaceRoot "deploy\docker-compose.demo.yml"
$projectName = "shopops-demo"
$env:SHOPOPS_DEMO_PORT = [string]$Port

if ($UseChinaMirror) {
    $mirrorPrefix = "swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library"
    $env:SHOPOPS_DOCKER_BUILDER_IMAGE = "$mirrorPrefix/eclipse-temurin:17-jdk"
    $env:SHOPOPS_DOCKER_RUNTIME_IMAGE = "$mirrorPrefix/eclipse-temurin:17-jre"
}

Set-Location $workspaceRoot

if (-not (Get-Command "docker" -ErrorAction SilentlyContinue)) {
    throw "Docker is not installed or is not available in PATH."
}

Write-Host "ShopOps Docker demo launcher"
Write-Host "Port: $Port"
Write-Host "Workbench: http://localhost:$Port/admin/workbench.html"
if ($UseChinaMirror) {
    Write-Host "Base images: China mirror"
}
Write-Host ""

if ($DryRun) {
    Write-Host "[DRY RUN] docker compose -p $projectName -f $composeFile up -d --build"
    if ($NoSeed) {
        Write-Host "[DRY RUN] scripts/check-shopops.ps1 -Port $Port"
    } else {
        Write-Host "[DRY RUN] scripts/seed-shopops-demo.ps1 -Port $Port -Scenario docker-interview-demo"
    }
    exit 0
}

docker info | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker daemon is not available. Start Docker Desktop and retry."
}

Write-Host "1. Building and starting ShopOps..."
docker compose -p $projectName -f $composeFile up -d --build
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed to start ShopOps."
}

try {
    Write-Host ""
    if ($NoSeed) {
        Write-Host "2. Waiting for health checks..."
        & powershell -NoProfile -ExecutionPolicy Bypass -File "$PSScriptRoot\check-shopops.ps1" -Port $Port
    } else {
        Write-Host "2. Preparing the complete interview demo chain..."
        $seedArgs = @(
            "-NoProfile",
            "-ExecutionPolicy", "Bypass",
            "-File", "$PSScriptRoot\seed-shopops-demo.ps1",
            "-Port", [string]$Port,
            "-Scenario", "docker-interview-demo"
        )
        if ($NoOpenBrowser) {
            $seedArgs += "-NoOpenBrowser"
        }
        & powershell @seedArgs
    }
    if ($LASTEXITCODE -ne 0) {
        throw "ShopOps started, but demo preparation failed."
    }
} catch {
    Write-Host ""
    Write-Host "Recent container logs:"
    docker compose -p $projectName -f $composeFile logs --tail 100 shopops-admin
    throw
}

Write-Host ""
Write-Host "ShopOps Docker demo is ready."
Write-Host "Workbench: http://localhost:$Port/admin/workbench.html"
Write-Host "Stop: docker compose -p $projectName -f deploy/docker-compose.demo.yml down"
