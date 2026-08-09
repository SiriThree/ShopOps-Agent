param(
    [int]$Port = 8080,
    [string]$Start = "2018-08-01",
    [string]$End = "2018-08-07",
    [int]$Rounds = 1,
    [int]$DelayMs = 100,
    [string]$OutputDir = "docs/evaluation"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Warning "LEGACY EVIDENCE: this script repeats fixed prompt templates and is not a unique-task ShopOpsBench dataset. Use scripts/run-shopops-benchmark.ps1 for benchmark runs."

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$outputRoot = Join-Path $workspaceRoot $OutputDir
$baseUrl = "http://localhost:$Port"

$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN,OPERATOR"
}

$promptTemplates = @(
    @{
        scenario = "daily_review"
        prompt = "Generate a shop daily operations report for {date}; include orders, reviews, products, ads, and platform metrics."
        expectedIntent = "daily_review"
    },
    @{
        scenario = "comment_risk"
        prompt = "Analyze negative comment reasons for {date}; identify priority risk products and service issues."
        expectedIntent = "comment_risk"
    },
    @{
        scenario = "product_optimization"
        prompt = "Find low-click or optimization candidate products for {date}; provide title and operation suggestions."
        expectedIntent = "product_optimization"
    },
    @{
        scenario = "ad_anomaly"
        prompt = "Check high-spend low-conversion ad campaigns for {date}; provide budget adjustment suggestions."
        expectedIntent = "ad_anomaly"
    }
)

function ConvertTo-Body($value) {
    return $value | ConvertTo-Json -Depth 10
}

function Invoke-ShopOps {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null
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
    return Invoke-RestMethod @arguments
}

function Get-DateValues {
    param(
        [string]$StartDate,
        [string]$EndDate
    )

    $dates = New-Object System.Collections.Generic.List[string]
    $cursor = [datetime]::ParseExact($StartDate, "yyyy-MM-dd", $null)
    $last = [datetime]::ParseExact($EndDate, "yyyy-MM-dd", $null)
    while ($cursor -le $last) {
        $dates.Add($cursor.ToString("yyyy-MM-dd"))
        $cursor = $cursor.AddDays(1)
    }
    return @($dates)
}

function Get-TaskDurationMs($task) {
    if (-not $task.startedAt -or -not $task.finishedAt) {
        return $null
    }
    return [math]::Round(((Get-Date $task.finishedAt) - (Get-Date $task.startedAt)).TotalMilliseconds, 1)
}

function Get-Percent([double]$Numerator, [double]$Denominator) {
    if ($Denominator -le 0) {
        return 0.0
    }
    return [math]::Round($Numerator * 100.0 / $Denominator, 2)
}

function Get-EvidenceMetric($evidence, [string]$sourceName, [string]$metricName) {
    if (-not $evidence -or -not $evidence.dataSources) {
        return $null
    }
    $source = $evidence.dataSources.$sourceName
    if (-not $source -or -not $source.metrics) {
        return $null
    }
    return $source.metrics.$metricName
}

function Escape-Csv([object]$value) {
    if ($null -eq $value) {
        return ""
    }
    $text = [string]$value
    if ($text.Contains('"') -or $text.Contains(',') -or $text.Contains("`n") -or $text.Contains("`r")) {
        return '"' + $text.Replace('"', '""') + '"'
    }
    return $text
}

Write-Host "ShopOps natural-language Agent batch evaluation"
Write-Host "Base URL: $baseUrl"
Write-Host "Date range: $Start to $End"
Write-Host "Rounds: $Rounds"
Write-Host "Output: $outputRoot"
Write-Host ""

try {
    $health = Invoke-ShopOps -Method Get -Path "/api/admin/connectors/status"
    $availableConnectors = @($health.data | Where-Object { $_.available }).Count
    Write-Host "Connector status reachable. Available connectors: $availableConnectors/$(@($health.data).Count)"
} catch {
    throw "ShopOps backend is not reachable at $baseUrl. Start it first, then rerun this script."
}

$dates = Get-DateValues -StartDate $Start -EndDate $End
$results = New-Object System.Collections.Generic.List[object]
$caseNo = 0

for ($round = 1; $round -le $Rounds; $round++) {
    foreach ($date in $dates) {
        foreach ($template in $promptTemplates) {
            $caseNo += 1
            $prompt = $template.prompt.Replace("{date}", $date)
            $body = @{
                userInput = $prompt
                dateRange = @{
                    start = $date
                    end = $date
                }
            }

            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            $ok = $false
            $errorMessage = $null
            $created = $null
            $task = $null
            $steps = @()
            $report = $null
            try {
                $created = Invoke-ShopOps -Method Post -Path "/api/agent/tasks/natural-language" -Body $body
                if ($created.code -ne 200) {
                    throw "create task returned code=$($created.code), message=$($created.message)"
                }
                $taskId = $created.data.task.taskId
                $task = Invoke-ShopOps -Method Get -Path "/api/agent/tasks/$taskId"
                if ($task.code -ne 200) {
                    throw "get task returned code=$($task.code), message=$($task.message)"
                }
                $stepsResponse = Invoke-ShopOps -Method Get -Path "/api/agent/tasks/$taskId/steps"
                if ($stepsResponse.code -ne 200) {
                    throw "list steps returned code=$($stepsResponse.code), message=$($stepsResponse.message)"
                }
                $steps = @($stepsResponse.data)
                if ($task.data.reportId) {
                    $report = Invoke-ShopOps -Method Get -Path "/api/reports/$($task.data.reportId)"
                    if ($report.code -ne 200) {
                        throw "get report returned code=$($report.code), message=$($report.message)"
                    }
                }
                $ok = $true
            } catch {
                $errorMessage = $_.Exception.Message
            } finally {
                $sw.Stop()
            }

            $intent = if ($created -and $created.data) { [string]$created.data.intent } else { $null }
            $expectedIntent = [string]$template.expectedIntent
            $taskStatus = if ($task -and $task.data) { [string]$task.data.status } else { "FAILED" }
            $reportStatus = if ($report -and $report.data) { [string]$report.data.status } else { $null }
            $evidence = if ($report -and $report.data) { $report.data.evidence } else { $null }
            $toolCodes = @($steps | ForEach-Object { [string]$_.toolCode } | Where-Object { $_ })
            $stepSuccessCount = @($steps | Where-Object { $_.status -eq "SUCCESS" }).Count
            $completionOk = $ok -and (@("SUCCESS", "DEGRADED", "APPROVAL_REQUIRED") -contains $taskStatus)
            $intentMatched = $intent -eq $expectedIntent
            $hasReport = $null -ne $report -and $null -ne $report.data.reportId
            $hasEvidence = $null -ne $evidence -and $null -ne $evidence.dataSources
            $passed = $completionOk -and $intentMatched -and $hasReport -and $hasEvidence

            $row = [pscustomobject]@{
                caseNo = $caseNo
                round = $round
                businessDate = $date
                scenario = [string]$template.scenario
                expectedIntent = $expectedIntent
                actualIntent = $intent
                intentMatched = $intentMatched
                taskId = if ($task -and $task.data) { $task.data.taskId } else { $null }
                reportId = if ($task -and $task.data) { $task.data.reportId } else { $null }
                traceId = if ($task -and $task.data) { [string]$task.data.traceId } else { $null }
                taskStatus = $taskStatus
                reportStatus = $reportStatus
                passed = $passed
                wallClockDurationMs = [math]::Round($sw.Elapsed.TotalMilliseconds, 1)
                taskDurationMs = if ($task -and $task.data) { Get-TaskDurationMs $task.data } else { $null }
                toolInvocationCount = $toolCodes.Count
                toolSuccessCount = $stepSuccessCount
                toolCodes = ($toolCodes -join ";")
                markdownLength = if ($report -and $report.data -and $report.data.markdown) { ([string]$report.data.markdown).Length } else { 0 }
                gmv = Get-EvidenceMetric $evidence "orderSummary" "gmv"
                orderCount = Get-EvidenceMetric $evidence "orderSummary" "orderCount"
                refundRate = Get-EvidenceMetric $evidence "orderSummary" "refundRate"
                negativeCount = Get-EvidenceMetric $evidence "negativeComments" "negativeCount"
                productCandidateCount = Get-EvidenceMetric $evidence "productCandidates" "candidateCount"
                adCost = Get-EvidenceMetric $evidence "adPerformance" "cost"
                adRoi = Get-EvidenceMetric $evidence "adPerformance" "roi"
                externalVisitors = Get-EvidenceMetric $evidence "externalReports" "visitorCount"
                errorMessage = $errorMessage
            }
            $results.Add($row)

            Write-Host ("[{0}/{1}] date={2} scenario={3} status={4} intent={5} tools={6} durationMs={7}" -f `
                    $caseNo, ($Rounds * $dates.Count * $promptTemplates.Count), $date, $template.scenario, $taskStatus, $intent, $toolCodes.Count, $row.wallClockDurationMs)
            if ($DelayMs -gt 0) {
                Start-Sleep -Milliseconds $DelayMs
            }
        }
    }
}

New-Item -ItemType Directory -Path $outputRoot -Force | Out-Null

$totalCases = $results.Count
$passedCases = @($results | Where-Object { $_.passed }).Count
$successCases = @($results | Where-Object { $_.taskStatus -eq "SUCCESS" }).Count
$intentMatches = @($results | Where-Object { $_.intentMatched }).Count
$totalTools = ($results | Measure-Object -Property toolInvocationCount -Sum).Sum
$successfulTools = ($results | Measure-Object -Property toolSuccessCount -Sum).Sum
$avgWallClock = ($results | Measure-Object -Property wallClockDurationMs -Average).Average
$avgTaskDuration = ($results | Where-Object { $null -ne $_.taskDurationMs } | Measure-Object -Property taskDurationMs -Average).Average
$p95WallClock = 0
if ($totalCases -gt 0) {
    $sortedDurations = @($results | Sort-Object wallClockDurationMs | ForEach-Object { [double]$_.wallClockDurationMs })
    $index = [math]::Min($sortedDurations.Count - 1, [math]::Ceiling($sortedDurations.Count * 0.95) - 1)
    $p95WallClock = [math]::Round($sortedDurations[$index], 1)
}

$statusBreakdown = [ordered]@{}
foreach ($group in ($results | Group-Object -Property taskStatus | Sort-Object Name)) {
    $statusBreakdown[$group.Name] = $group.Count
}

$scenarioBreakdown = @()
foreach ($group in ($results | Group-Object -Property scenario | Sort-Object Name)) {
    $items = @($group.Group)
    $scenarioBreakdown += [pscustomobject]@{
        scenario = $group.Name
        caseCount = $items.Count
        passedCaseCount = @($items | Where-Object { $_.passed }).Count
        successRate = Get-Percent @($items | Where-Object { $_.taskStatus -eq "SUCCESS" }).Count $items.Count
        avgToolInvocationCount = [math]::Round(($items | Measure-Object -Property toolInvocationCount -Average).Average, 2)
        avgWallClockDurationMs = [math]::Round(($items | Measure-Object -Property wallClockDurationMs -Average).Average, 1)
    }
}

$summary = [ordered]@{
    generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    evaluationName = "shopops-agent-natural-language-batch-v1"
    baseUrl = $baseUrl
    dateRange = [ordered]@{
        start = $Start
        end = $End
        days = $dates.Count
    }
    rounds = $Rounds
    caseCount = $totalCases
    passedCaseCount = $passedCases
    passRate = Get-Percent $passedCases $totalCases
    successRate = Get-Percent $successCases $totalCases
    intentAccuracy = Get-Percent $intentMatches $totalCases
    toolInvocationCount = [int]$totalTools
    toolSuccessCount = [int]$successfulTools
    toolInvocationSuccessRate = Get-Percent $successfulTools $totalTools
    avgToolInvocationCount = if ($totalCases -gt 0) { [math]::Round($totalTools / $totalCases, 2) } else { 0.0 }
    avgWallClockDurationMs = if ($avgWallClock) { [math]::Round($avgWallClock, 1) } else { 0.0 }
    p95WallClockDurationMs = $p95WallClock
    avgTaskDurationMs = if ($avgTaskDuration) { [math]::Round($avgTaskDuration, 1) } else { 0.0 }
    statusBreakdown = $statusBreakdown
    scenarioBreakdown = $scenarioBreakdown
    results = $results
}

$summaryJsonPath = Join-Path $outputRoot "agent-natural-language-batch-summary.json"
$detailsCsvPath = Join-Path $outputRoot "agent-natural-language-batch-details.csv"
$summaryMdPath = Join-Path $outputRoot "agent-natural-language-batch-summary.md"

$summary | ConvertTo-Json -Depth 10 | Set-Content -Path $summaryJsonPath -Encoding UTF8

$csvHeader = @(
    "caseNo", "round", "businessDate", "scenario", "expectedIntent", "actualIntent", "intentMatched",
    "taskId", "reportId", "traceId", "taskStatus", "reportStatus", "passed",
    "wallClockDurationMs", "taskDurationMs", "toolInvocationCount", "toolSuccessCount", "toolCodes",
    "markdownLength", "gmv", "orderCount", "refundRate", "negativeCount", "productCandidateCount",
    "adCost", "adRoi", "externalVisitors", "errorMessage"
)
$csvLines = New-Object System.Collections.Generic.List[string]
$csvLines.Add(($csvHeader -join ","))
foreach ($item in $results) {
    $csvLines.Add((@($csvHeader | ForEach-Object { Escape-Csv $item.$_ }) -join ","))
}
$csvLines | Set-Content -Path $detailsCsvPath -Encoding UTF8

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# ShopOps Agent Natural Language Batch Evaluation")
$md.Add("")
$md.Add("- Generated at: $($summary.generatedAt)")
$md.Add("- Base URL: $($summary.baseUrl)")
$md.Add("- Date range: $Start to $End ($($dates.Count) days)")
$md.Add("- Rounds: $Rounds")
$md.Add("")
$md.Add("## Summary")
$md.Add("")
$md.Add("| Metric | Value |")
$md.Add("|---|---:|")
$md.Add("| Cases | $($summary.caseCount) |")
$md.Add("| Passed cases | $($summary.passedCaseCount) |")
$md.Add("| Pass rate | $($summary.passRate)% |")
$md.Add("| Success rate | $($summary.successRate)% |")
$md.Add("| Intent accuracy | $($summary.intentAccuracy)% |")
$md.Add("| Tool invocations | $($summary.toolInvocationCount) |")
$md.Add("| Tool invocation success rate | $($summary.toolInvocationSuccessRate)% |")
$md.Add("| Avg tools per task | $($summary.avgToolInvocationCount) |")
$md.Add("| Avg wall-clock duration | $($summary.avgWallClockDurationMs) ms |")
$md.Add("| P95 wall-clock duration | $($summary.p95WallClockDurationMs) ms |")
$md.Add("| Avg task duration | $($summary.avgTaskDurationMs) ms |")
$md.Add("")
$md.Add("## Scenario Breakdown")
$md.Add("")
$md.Add("| Scenario | Cases | Passed | Success Rate | Avg Tools | Avg Duration ms |")
$md.Add("|---|---:|---:|---:|---:|---:|")
foreach ($scenario in $scenarioBreakdown) {
    $md.Add("| $($scenario.scenario) | $($scenario.caseCount) | $($scenario.passedCaseCount) | $($scenario.successRate)% | $($scenario.avgToolInvocationCount) | $($scenario.avgWallClockDurationMs) |")
}
$md.Add("")
$md.Add("## Output Files")
$md.Add("")
$md.Add("- JSON summary: " + $summaryJsonPath)
$md.Add("- CSV details: " + $detailsCsvPath)
$md.Add("")
$md.Add("## Notes")
$md.Add("")
$md.Add("- This batch calls the real ShopOps natural-language task API.")
$md.Add("- The backend must be started with the configured public-data file connectors.")
$md.Add("- wallClockDurationMs is measured by this runner around the HTTP task creation and verification flow.")
$md.Add("- taskDurationMs is computed from ShopOps task startedAt and finishedAt fields when available.")
$md | Set-Content -Path $summaryMdPath -Encoding UTF8

Write-Host ""
Write-Host "Batch evaluation completed."
Write-Host "Cases: $($summary.caseCount)"
Write-Host "Pass rate: $($summary.passRate)%"
Write-Host "Success rate: $($summary.successRate)%"
Write-Host "Intent accuracy: $($summary.intentAccuracy)%"
Write-Host "Tool calls: $($summary.toolInvocationCount)"
Write-Host "Tool success rate: $($summary.toolInvocationSuccessRate)%"
Write-Host "Average wall-clock duration: $($summary.avgWallClockDurationMs) ms"
Write-Host "P95 wall-clock duration: $($summary.p95WallClockDurationMs) ms"
Write-Host "Summary: $summaryJsonPath"
Write-Host "Details: $detailsCsvPath"
