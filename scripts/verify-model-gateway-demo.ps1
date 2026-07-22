param(
    [int]$Port = 8080,
    [string]$Start = "2026-07-18",
    [string]$End = "2026-07-18"
)

$ErrorActionPreference = "Stop"
$baseUrl = "http://localhost:$Port"
$headers = @{
    "Content-Type" = "application/json"
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Roles" = "ADMIN"
}

$body = @{
    taskType = "daily_review"
    userInput = "Use Model Gateway to generate today's shop operation review and priority actions."
    dateRange = @{
        start = $Start
        end = $End
    }
} | ConvertTo-Json -Depth 5

Write-Host "Creating daily_review task..."
$created = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/agent/tasks" -Headers $headers -Body $body
if ($created.code -ne 200) {
    throw "创建任务失败: $($created | ConvertTo-Json -Depth 8)"
}

$taskId = $created.data.taskId
Write-Host "Task ID: $taskId, status: $($created.data.status)"

$task = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/agent/tasks/$taskId" -Headers $headers
$reportId = $task.data.reportId
Write-Host "Report ID: $reportId"

if ($reportId) {
    $report = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/reports/$reportId" -Headers $headers
    Write-Host ""
    Write-Host "Report preview:"
    $markdown = [string]$report.data.markdown
    Write-Host $markdown.Substring(0, [Math]::Min(800, $markdown.Length))
}

$logs = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/admin/model-gateway/call-logs?taskId=$taskId&pageNum=1&pageSize=10" -Headers $headers
Write-Host ""
Write-Host "Model call logs:"
$logs.data.list | Select-Object callId, providerCode, modelName, promptCode, status, latencyMs, totalTokens | Format-Table -AutoSize

Write-Host "Model call total: $($logs.data.total)"
