param(
    [int]$Port = 8080,
    [string]$Start = "2026-07-18",
    [string]$End = "2026-07-18"
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8
$baseUrl = "http://localhost:$Port"
$confirmText = -join ([char]0x786E, [char]0x8BA4, [char]0x901A, [char]0x8FC7)
$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

function ConvertTo-Body($value) {
    return $value | ConvertTo-Json -Depth 8
}

function Invoke-ShopOps {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [switch]$AllowFailure
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
    try {
        $response = Invoke-RestMethod @arguments
        if ($response.code -ne 200 -and -not $AllowFailure) {
            throw "API returned non-success payload: $($response | ConvertTo-Json -Depth 8)"
        }
        return $response
    } catch {
        if (-not $AllowFailure) {
            throw
        }
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            return $_.ErrorDetails.Message | ConvertFrom-Json
        }
        if ($_.Exception.Response) {
            $stream = $null
            try {
                $stream = $_.Exception.Response.GetResponseStream()
            } catch {
                $stream = $null
            }
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $text = $reader.ReadToEnd()
                if ($text) {
                    return $text | ConvertFrom-Json
                }
            }
        }
        return [pscustomobject]@{
            code = 500
            message = $_.Exception.Message
            data = $null
        }
    }
}

Write-Host "ShopOps AgentOps demo verification"
Write-Host "Base URL: $baseUrl"
Write-Host ""

Write-Host "1. Checking dashboard health..."
$dashboard = Invoke-ShopOps -Method Get -Path "/api/admin/dashboard/summary"
Write-Host "   Tasks: $($dashboard.data.taskMetrics.total), Reports: $($dashboard.data.reportTotal), Tool calls: $($dashboard.data.toolCallTotal)"

Write-Host "2. Creating daily_review task..."
$taskBody = @{
    taskType = "daily_review"
    userInput = "Generate a shop operation review and highlight refund, negative comment, and product optimization priorities."
    dateRange = @{
        start = $Start
        end = $End
    }
}
$createdTask = Invoke-ShopOps -Method Post -Path "/api/agent/tasks" -Body $taskBody
$taskId = $createdTask.data.taskId
Write-Host "   Task ID: $taskId, status: $($createdTask.data.status)"

$task = Invoke-ShopOps -Method Get -Path "/api/agent/tasks/$taskId"
$reportId = $task.data.reportId
Write-Host "   Report ID: $reportId"
if ($reportId) {
    $report = Invoke-ShopOps -Method Get -Path "/api/reports/$reportId"
    $markdown = [string]$report.data.markdown
    Write-Host "   Report markdown length: $($markdown.Length)"
}

Write-Host ""
Write-Host "3. Triggering high-risk refund tool..."
$approvalRequired = Invoke-ShopOps -Method Post -Path "/api/tools/order.refund_execute/invoke" -Body @{
    shopId = 1
    refundAmount = 1288
    reason = "AgentOps demo high risk refund approval"
}
$approvalId = $approvalRequired.data.approvalId
$toolCallLogId = $approvalRequired.data.toolCallLogId
Write-Host "   Approval ID: $approvalId, Tool call log ID: $toolCallLogId, status: $($approvalRequired.data.status)"

Write-Host "4. Verifying high-risk confirmation guard..."
$missingConfirm = Invoke-ShopOps -Method Post -Path "/api/admin/approvals/$approvalId/approve" -Body @{
    comment = "Approval without confirm text should fail"
} -AllowFailure
if ($missingConfirm.code -ne 400) {
    throw "High-risk confirmation guard did not take effect: $($missingConfirm | ConvertTo-Json -Depth 8)"
}
Write-Host "   Missing confirm rejected: $($missingConfirm.message)"

Write-Host "5. Approving with confirm text and retrying tool..."
$approved = Invoke-ShopOps -Method Post -Path "/api/admin/approvals/$approvalId/approve" -Body @{
    comment = "AgentOps demo approval granted"
    confirmText = $confirmText
}
Write-Host "   Approval status: $($approved.data.status), approver: $($approved.data.approverName)"

$retry = Invoke-ShopOps -Method Post -Path "/api/tools/order.refund_execute/invoke" -Body @{
    shopId = 1
    refundAmount = 1288
    reason = "AgentOps demo high risk refund approval"
    approvalId = $approvalId
}
Write-Host "   Retry status: $($retry.data.status), refund status: $($retry.data.data.status)"

Write-Host ""
Write-Host "6. Checking audit timeline..."
$auditDetail = Invoke-ShopOps -Method Get -Path "/api/admin/audit/timeline/APPROVAL/$approvalId"
Write-Host "   Approval audit detail event: $($auditDetail.data.event.eventType), status: $($auditDetail.data.event.eventStatus)"
$audit = Invoke-ShopOps -Method Get -Path "/api/admin/audit/timeline?source=APPROVAL&toolCode=order.refund_execute&pageNum=1&pageSize=10"
$currentApprovalEvents = @($audit.data.list | Where-Object { $_.resourceId -eq [string]$approvalId })
Write-Host "   Current approval events in timeline: $($currentApprovalEvents.Count)"
$currentApprovalEvents | Select-Object eventType, eventStatus, source, resourceId, riskLevel, createdAt | Format-Table -AutoSize

Write-Host ""
Write-Host "Demo links:"
Write-Host "   Dashboard:  $baseUrl/admin/dashboard.html"
Write-Host "   Tasks:      $baseUrl/admin/tasks.html?taskId=$taskId"
Write-Host "   Reports:    $baseUrl/admin/reports.html?reportId=$reportId"
Write-Host "   Approvals:  $baseUrl/admin/approvals.html?approvalId=$approvalId"
Write-Host "   Audit:      $baseUrl/admin/audit.html?source=APPROVAL&resourceId=$approvalId"
Write-Host "   Tool logs:  $baseUrl/admin/tools.html?toolCode=order.refund_execute"
Write-Host ""
Write-Host "AgentOps demo verification completed."
