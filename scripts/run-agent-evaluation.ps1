param(
    [string]$Module = "shopops-admin"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
$moduleRoot = Join-Path $workspaceRoot $Module
$evaluationDir = Join-Path $moduleRoot "target\evaluation"

Write-Host "ShopOps agent evaluation runner"
Write-Host "Workspace: $workspaceRoot"
Write-Host "Module:    $Module"
Write-Host ""

Push-Location $workspaceRoot
try {
    $testSelector = "AgentEvaluationIntegrationTest,AgentEvaluationModelIntegrationTest,AgentEvaluationDegradedIntegrationTest"
    & mvn -pl $Module "-Dtest=$testSelector" test
    if ($LASTEXITCODE -ne 0) {
        throw "Maven test run failed with exit code $LASTEXITCODE"
    }

    $summarySpecs = @(
        @{ key = "core"; path = (Join-Path $evaluationDir "agent-eval-summary.json"); label = "Core" },
        @{ key = "model"; path = (Join-Path $evaluationDir "agent-eval-model-summary.json"); label = "Model" },
        @{ key = "degraded"; path = (Join-Path $evaluationDir "agent-eval-degraded-summary.json"); label = "Degraded" }
    )

    $suiteSummaries = @()
    foreach ($spec in $summarySpecs) {
        if (-not (Test-Path $spec.path)) {
            throw "Missing evaluation summary: $($spec.path)"
        }
        $suiteSummaries += [pscustomobject]@{
            key = $spec.key
            label = $spec.label
            data = Get-Content -Path $spec.path -Raw | ConvertFrom-Json
        }
    }

    $allResults = @()
    foreach ($suite in $suiteSummaries) {
        foreach ($result in $suite.data.results) {
            $allResults += [pscustomobject]@{
                suite = $suite.label
                caseId = $result.caseId
                scenario = $result.scenario
                actualStatus = $result.actualStatus
                passed = [bool]$result.passed
                actualApprovalCreated = [bool]$result.actualApprovalCreated
                expectApprovalCreated = $result.expectApprovalCreated
                configMatched = [bool]$result.configMatched
                actualDegraded = [bool]$result.actualDegraded
                durationMs = [double]$result.durationMs
                toolInvocationCount = [int]$result.toolInvocationCount
                toolSuccessCount = [int]$result.toolSuccessCount
                mismatches = @($result.mismatches)
            }
        }
    }

    $caseCount = $allResults.Count
    $passedCaseCount = @($allResults | Where-Object { $_.passed }).Count
    $successCount = @($allResults | Where-Object { $_.actualStatus -eq "SUCCESS" }).Count
    $degradedCount = @($allResults | Where-Object { $_.actualStatus -eq "DEGRADED" }).Count
    $completionCount = @($allResults | Where-Object { @("SUCCESS", "DEGRADED", "APPROVAL_REQUIRED") -contains $_.actualStatus }).Count
    $totalDuration = ($allResults | Measure-Object -Property durationMs -Sum).Sum
    $totalToolInvocations = ($allResults | Measure-Object -Property toolInvocationCount -Sum).Sum
    $totalToolSuccess = ($allResults | Measure-Object -Property toolSuccessCount -Sum).Sum

    $approvalCases = @($allResults | Where-Object { $null -ne $_.expectApprovalCreated })
    $approvalMatched = @($approvalCases | Where-Object { $_.expectApprovalCreated -eq $_.actualApprovalCreated }).Count

    $configCases = @($allResults | Where-Object { $_.configMatched -ne $null })
    $configMatched = @($configCases | Where-Object { $_.configMatched }).Count

    $statusBreakdown = [ordered]@{}
    foreach ($group in ($allResults | Group-Object -Property actualStatus | Sort-Object Name)) {
        $statusBreakdown[$group.Name] = $group.Count
    }

    $suiteBreakdown = @()
    foreach ($suite in $suiteSummaries) {
        $results = @($allResults | Where-Object { $_.suite -eq $suite.label })
        $suiteBreakdown += [pscustomobject]@{
            suite = $suite.label
            caseCount = $results.Count
            passedCaseCount = @($results | Where-Object { $_.passed }).Count
            successCount = @($results | Where-Object { $_.actualStatus -eq "SUCCESS" }).Count
            degradedCount = @($results | Where-Object { $_.actualStatus -eq "DEGRADED" }).Count
            approvalRequiredCount = @($results | Where-Object { $_.actualStatus -eq "APPROVAL_REQUIRED" }).Count
        }
    }

    function Get-Percent([double]$numerator, [double]$denominator) {
        if ($denominator -le 0) {
            return 0.0
        }
        return [math]::Round(($numerator * 100.0 / $denominator), 1)
    }

    $portfolioSummary = [ordered]@{
        generatedAt = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
        caseCount = $caseCount
        passedCaseCount = $passedCaseCount
        completionRate = (Get-Percent $completionCount $caseCount)
        successRate = (Get-Percent $successCount $caseCount)
        degradedCompletionRate = (Get-Percent $degradedCount $caseCount)
        avgTaskDurationMs = if ($caseCount -gt 0) { [math]::Round(($totalDuration / $caseCount), 1) } else { 0.0 }
        toolInvocationSuccessRate = (Get-Percent $totalToolSuccess $totalToolInvocations)
        approvalDecisionAccuracy = (Get-Percent $approvalMatched $approvalCases.Count)
        configEffectAccuracy = (Get-Percent $configMatched $configCases.Count)
        statusBreakdown = $statusBreakdown
        suiteBreakdown = $suiteBreakdown
        results = $allResults
    }

    $portfolioJsonPath = Join-Path $evaluationDir "agent-eval-portfolio-summary.json"
    $portfolioMdPath = Join-Path $evaluationDir "agent-eval-portfolio-summary.md"

    $portfolioSummary | ConvertTo-Json -Depth 8 | Set-Content -Path $portfolioJsonPath -Encoding UTF8

    $lines = @()
    $lines += "# ShopOps Agent Evaluation Portfolio Summary"
    $lines += ""
    $lines += "- Generated at: $($portfolioSummary.generatedAt)"
    $lines += "- Total case count: $($portfolioSummary.caseCount)"
    $lines += "- Passed case count: $($portfolioSummary.passedCaseCount)"
    $lines += "- Completion rate: $($portfolioSummary.completionRate)%"
    $lines += "- Success rate: $($portfolioSummary.successRate)%"
    $lines += "- Degraded completion rate: $($portfolioSummary.degradedCompletionRate)%"
    $lines += "- Avg task duration: $($portfolioSummary.avgTaskDurationMs) ms"
    $lines += "- Tool invocation success rate: $($portfolioSummary.toolInvocationSuccessRate)%"
    $lines += "- Approval accuracy: $($portfolioSummary.approvalDecisionAccuracy)%"
    $lines += "- Config effect accuracy: $($portfolioSummary.configEffectAccuracy)%"
    $lines += "- Status breakdown: $($portfolioSummary.statusBreakdown | ConvertTo-Json -Compress)"
    $lines += ""
    $lines += "## Suite Breakdown"
    $lines += ""
    $lines += "| suite | cases | passed | success | degraded | approval_required |"
    $lines += "|---|---:|---:|---:|---:|---:|"
    foreach ($suite in $suiteBreakdown) {
        $lines += "|$($suite.suite)|$($suite.caseCount)|$($suite.passedCaseCount)|$($suite.successCount)|$($suite.degradedCount)|$($suite.approvalRequiredCount)|"
    }
    $lines += ""
    $lines += "## Case Results"
    $lines += ""
    $lines += "| suite | caseId | scenario | status | passed | degraded | durationMs |"
    $lines += "|---|---|---|---|---:|---:|---:|"
    foreach ($result in $allResults) {
        $lines += "|$($result.suite)|$($result.caseId)|$($result.scenario)|$($result.actualStatus)|$($result.passed)|$($result.actualDegraded)|$($result.durationMs)|"
    }
    $lines += ""
    $lines += "## Mismatches"
    $lines += ""
    $mismatchRows = @($allResults | Where-Object { $_.mismatches.Count -gt 0 })
    if ($mismatchRows.Count -eq 0) {
        $lines += "- None"
    } else {
        foreach ($row in $mismatchRows) {
            $lines += "- [$($row.suite)] $($row.caseId): $([string]::Join('; ', $row.mismatches))"
        }
    }

    $lines | Set-Content -Path $portfolioMdPath -Encoding UTF8

    Write-Host ""
    Write-Host "Portfolio evaluation summary generated:"
    Write-Host "  JSON: $portfolioJsonPath"
    Write-Host "  MD:   $portfolioMdPath"
} finally {
    Pop-Location
}
