param(
    [ValidateSet("task", "idempotency", "recovery", "governance", "all")]
    [string]$BenchmarkType = "task",
    [ValidateSet("smoke", "dev", "validation", "test")]
    [string]$Split = "smoke",
    [string]$CaseId = "",
    [string]$Scenario = "",
    [string]$Tag = "",
    [switch]$Formal,
    [switch]$FormalTest,
    [long]$Seed = 6101,
    [string]$OutputDir = "target/benchmark",
    [string]$AuthorizationMode = "",
    [string]$ExternalSystemMode = "NON_IDEMPOTENT_EXTERNAL",
    [string]$ModelMode = "DISABLED"
)

$ErrorActionPreference = "Stop"
if ($FormalTest) { $Formal = $true } # backward-compatible alias
if ($Formal) { $Split = "test" }

function Resolve-Maven {
    $root = Split-Path -Parent $PSScriptRoot
    $wrapperCmd = Join-Path $root "mvnw.cmd"
    $wrapperSh = Join-Path $root "mvnw"
    if ($IsWindows -and (Test-Path $wrapperCmd)) { return $wrapperCmd }
    if (-not $IsWindows -and (Test-Path $wrapperSh)) { return $wrapperSh }
    $mvn = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvn) { return $mvn.Source }
    return $null
}

if ($BenchmarkType -eq "all") {
    $failed = $false
    foreach ($type in @("task", "idempotency", "recovery", "governance")) {
        Write-Host "=== ShopOpsBench: $type ==="
        & $PSCommandPath -BenchmarkType $type -Split $Split -CaseId $CaseId -Scenario $Scenario -Tag $Tag `
            -Formal:$Formal -Seed $Seed -OutputDir $OutputDir -AuthorizationMode $AuthorizationMode `
            -ExternalSystemMode $ExternalSystemMode -ModelMode $ModelMode
        if ($LASTEXITCODE -ne 0) { $failed = $true }
    }
    if ($failed) { exit 1 }
    exit 0
}

if (($BenchmarkType -eq "idempotency" -or $BenchmarkType -eq "recovery" -or $BenchmarkType -eq "governance") -and $Split -eq "smoke") {
    $Split = "dev"
}
if ($Split -eq "test" -and -not $Formal) {
    Write-Host "HELD_OUT_BLOCKED: test split requires -Formal. Held-out Gold must not be used for routine development."
    exit 4
}

$maven = Resolve-Maven
if (-not $maven) {
    Write-Host "NOT_RUN: Maven/Maven Wrapper is unavailable. See docs/evaluation-rebuild/BENCHMARK_RUNBOOK.md."
    exit 3
}

$test = "BenchmarkRunnerLifecycleTest"
if ($Formal) {
    switch ($BenchmarkType) {
        "task"        { $test = "FormalTaskBenchmarkIntegrationTest" }
        "idempotency" { $test = "FormalIdempotencyBenchmarkIntegrationTest" }
        "recovery"    { $test = "FormalRecoveryBenchmarkIntegrationTest" }
        "governance"  { $test = "FormalGovernanceBenchmarkIntegrationTest" }
    }
} elseif ($BenchmarkType -eq "idempotency") {
    $test = "Phase3IdempotencyBenchmarkIntegrationTest"
} elseif ($BenchmarkType -eq "recovery") {
    $test = "Phase4RecoveryBenchmarkIntegrationTest"
} elseif ($BenchmarkType -eq "governance") {
    $test = "Phase5GovernanceBenchmarkIntegrationTest"
} elseif ($CaseId -eq "smoke-task-degraded-ad-001" -or $Tag -eq "DEGRADED") {
    $test = "BenchmarkDegradedSmokeIntegrationTest"
}

$mvnArgs = @(
    "--batch-mode", "--no-transfer-progress",
    "-pl", "shopops-admin", "-am",
    "-Dtest=$test",
    "-Dsurefire.failIfNoSpecifiedTests=false",
    "-Dshopops.benchmark.split=$Split",
    "-Dshopops.benchmark.type=$BenchmarkType",
    "-Dshopops.benchmark.seed=$Seed",
    "-Dshopops.benchmark.output=$OutputDir",
    "-Dshopops.benchmark.authorizationMode=$AuthorizationMode",
    "-Dshopops.benchmark.externalSystemMode=$ExternalSystemMode",
    "-Dshopops.benchmark.modelMode=$ModelMode"
)
if ($Formal) { $mvnArgs += "-Dshopops.formal.it=true" }
if ($CaseId) { $mvnArgs += "-Dshopops.benchmark.caseId=$CaseId" }
if ($Scenario) { $mvnArgs += "-Dshopops.benchmark.scenario=$Scenario" }
if ($Tag) { $mvnArgs += "-Dshopops.benchmark.tag=$Tag" }
$mvnArgs += "test"

Write-Host "Running ShopOpsBench type=$BenchmarkType split=$Split formal=$Formal test=$test"
& $maven @mvnArgs
exit $LASTEXITCODE
