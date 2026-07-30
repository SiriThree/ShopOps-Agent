param(
    [string]$OutputDir = "docs/evaluation",
    [int]$TimeoutSeconds = 60,
    [switch]$SkipCompile
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$outputPath = Join-Path $workspaceRoot $OutputDir
$summaryPath = Join-Path $outputPath "mcp-stdio-client-verification-summary.json"
$markdownPath = Join-Path $outputPath "mcp-stdio-client-verification-summary.md"
$tempPath = Join-Path $workspaceRoot "shopops-admin\target\mcp-stdio-client-verification"
$inputPath = Join-Path $tempPath "stdin.jsonl"
$stdoutPath = Join-Path $tempPath "stdout.jsonl"
$stderrPath = Join-Path $tempPath "stderr.log"

if (-not (Test-Path $outputPath)) {
    New-Item -ItemType Directory -Path $outputPath | Out-Null
}
if (-not (Test-Path $tempPath)) {
    New-Item -ItemType Directory -Path $tempPath | Out-Null
}

function ConvertTo-LineJson {
    param($Value)
    return ($Value | ConvertTo-Json -Depth 30 -Compress)
}

function New-Check {
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

function Join-ProcessArguments {
    param([string[]]$Arguments)
    return (($Arguments | ForEach-Object {
        if ($_ -match '^[A-Za-z0-9_./:=\\-]+$') {
            $_
        } else {
            '"' + ($_.Replace('\', '\\').Replace('"', '\"')) + '"'
        }
    }) -join " ")
}

$requests = @(
    [ordered]@{ jsonrpc = "2.0"; id = "stdio-init-1"; method = "initialize" },
    [ordered]@{ jsonrpc = "2.0"; id = "stdio-tools-1"; method = "tools/list" },
    [ordered]@{
        jsonrpc = "2.0"
        id = "stdio-call-order-1"
        method = "tools/call"
        params = @{
            name = "order.query_summary"
            arguments = @{
                startDate = "2018-08-07"
                endDate = "2018-08-07"
            }
        }
    },
    [ordered]@{
        jsonrpc = "2.0"
        id = "stdio-call-risk-1"
        method = "tools/call"
        params = @{
            name = "ad.suggest_budget"
            arguments = @{
                campaignId = "AD-LOW-001"
                changePercent = -20
            }
        }
    }
)

($requests | ForEach-Object { ConvertTo-LineJson $_ }) | Set-Content -Encoding UTF8 -Path $inputPath
Remove-Item -Path $stdoutPath, $stderrPath -ErrorAction SilentlyContinue

$arguments = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", (Join-Path $workspaceRoot "scripts\run-mcp-stdio-server.ps1"))
if ($SkipCompile) {
    $arguments += "-SkipCompile"
}

Write-Host "ShopOps stdio MCP client verification"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Output: $OutputDir"
Write-Host ""

$startedAt = Get-Date
$process = [System.Diagnostics.Process]::new()
$process.StartInfo.FileName = "powershell"
$process.StartInfo.WorkingDirectory = $workspaceRoot
$process.StartInfo.UseShellExecute = $false
$process.StartInfo.RedirectStandardInput = $true
$process.StartInfo.RedirectStandardOutput = $true
$process.StartInfo.RedirectStandardError = $true
$process.StartInfo.Arguments = Join-ProcessArguments -Arguments $arguments
$process.Start() | Out-Null

Get-Content -Path $inputPath -Encoding UTF8 | ForEach-Object {
    $process.StandardInput.WriteLine($_)
}
$process.StandardInput.Close()

$stdoutTask = $process.StandardOutput.ReadToEndAsync()
$stderrTask = $process.StandardError.ReadToEndAsync()
$completed = $process.WaitForExit($TimeoutSeconds * 1000)
if (-not $completed) {
    try {
        $process.Kill($true)
    } catch {
        $process.Kill()
    }
}
$stdout = $stdoutTask.GetAwaiter().GetResult()
$stderr = $stderrTask.GetAwaiter().GetResult()
$stdout | Set-Content -Encoding UTF8 -Path $stdoutPath
$stderr | Set-Content -Encoding UTF8 -Path $stderrPath

$responses = New-Object System.Collections.Generic.List[object]
foreach ($line in ($stdout -split "`r?`n")) {
    if ([string]::IsNullOrWhiteSpace($line)) {
        continue
    }
    try {
        $responses.Add(($line | ConvertFrom-Json)) | Out-Null
    } catch {
        # Keep non-JSON stdout visible in the generated evidence.
    }
}

$responseById = @{}
foreach ($response in $responses) {
    $responseById[[string]$response.id] = $response
}

$initialize = $responseById["stdio-init-1"]
$toolsList = $responseById["stdio-tools-1"]
$orderCall = $responseById["stdio-call-order-1"]
$riskCall = $responseById["stdio-call-risk-1"]
$toolCount = if ($toolsList -and $toolsList.result -and $toolsList.result.tools) { @($toolsList.result.tools).Count } else { 0 }

$checks = @(
    (New-Check `
        -Name "stdio process exited" `
        -Passed ($completed -and $process.ExitCode -eq 0) `
        -Detail "completed=$completed, exitCode=$($process.ExitCode)"),
    (New-Check `
        -Name "initialize response" `
        -Passed ($initialize.result.protocolVersion -eq "2026-07-28") `
        -Detail "protocol=$($initialize.result.protocolVersion)"),
    (New-Check `
        -Name "tools/list response" `
        -Passed ($toolCount -ge 18) `
        -Detail "toolCount=$toolCount"),
    (New-Check `
        -Name "read-only tools/call" `
        -Passed ($orderCall.result.isError -eq $false -and $orderCall.result.structuredContent.success -eq $true) `
        -Detail "status=$($orderCall.result.structuredContent.status)"),
    (New-Check `
        -Name "high-risk approval response" `
        -Passed ($riskCall.result.isError -eq $true -and $riskCall.result.structuredContent.status -eq "APPROVAL_REQUIRED") `
        -Detail "status=$($riskCall.result.structuredContent.status)")
)

$passedCount = @($checks | Where-Object { $_.passed }).Count
$durationMs = [math]::Round(((Get-Date) - $startedAt).TotalMilliseconds, 2)
$summary = [ordered]@{
    evidenceName = "shopops-mcp-stdio-client-verification-v1"
    generatedAt = (Get-Date).ToString("o")
    transport = "stdio"
    command = "powershell"
    args = @("-ExecutionPolicy", "Bypass", "-File", "scripts/run-mcp-stdio-server.ps1")
    requestCount = $requests.Count
    responseCount = $responses.Count
    totalChecks = $checks.Count
    passedChecks = $passedCount
    successRate = [math]::Round($passedCount * 100.0 / $checks.Count, 2)
    toolCount = $toolCount
    durationMs = $durationMs
    stdoutFile = "shopops-admin/target/mcp-stdio-client-verification/stdout.jsonl"
    stderrFile = "shopops-admin/target/mcp-stdio-client-verification/stderr.log"
    checks = $checks
    claimBoundary = "This simulates an external MCP client launching ShopOps over stdio and exchanging line-delimited JSON-RPC messages."
}

$summary | ConvertTo-Json -Depth 30 | Set-Content -Encoding UTF8 -Path $summaryPath

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# ShopOps stdio MCP Client Verification")
$lines.Add("")
$lines.Add("| Metric | Value |")
$lines.Add("| --- | --- |")
$lines.Add("| Transport | stdio |")
$lines.Add("| Requests | $($requests.Count) |")
$lines.Add("| Responses | $($responses.Count) |")
$lines.Add("| Tool count | $toolCount |")
$lines.Add("| Passed checks | $passedCount / $($checks.Count) |")
$lines.Add("| Success rate | $($summary.successRate)% |")
$lines.Add("| Duration | $durationMs ms |")
$lines.Add("")
$lines.Add("## Checks")
$lines.Add("")
$lines.Add("| Check | Result | Detail |")
$lines.Add("| --- | --- | --- |")
foreach ($check in $checks) {
    $result = if ($check.passed) { "PASS" } else { "FAIL" }
    $lines.Add("| $($check.name) | $result | $($check.detail) |")
}
$lines.Add("")
$lines.Add("## Boundary")
$lines.Add("")
$lines.Add($summary.claimBoundary)
$lines | Set-Content -Encoding UTF8 -Path $markdownPath

foreach ($check in $checks) {
    $prefix = if ($check.passed) { "[PASS]" } else { "[FAIL]" }
    Write-Host "$prefix $($check.name) - $($check.detail)"
}
Write-Host ""
Write-Host "Summary: $summaryPath"
Write-Host "Markdown: $markdownPath"

if ($passedCount -ne $checks.Count) {
    throw "stdio MCP client verification failed: $passedCount/$($checks.Count) checks passed."
}
