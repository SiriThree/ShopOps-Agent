param(
    [string]$BaseUrl = "",
    [int]$Port = 8080,
    [switch]$Json,
    [switch]$SkipHttp
)

$ErrorActionPreference = "Stop"
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$workspaceRoot = Split-Path -Parent $PSScriptRoot
Set-Location $workspaceRoot

if ([string]::IsNullOrWhiteSpace($BaseUrl)) {
    $BaseUrl = "http://localhost:$Port"
}
$BaseUrl = $BaseUrl.TrimEnd("/")

$headers = @{
    "X-Tenant-Id" = "1"
    "X-Shop-Id" = "1"
    "X-User-Id" = "1"
    "X-User-Name" = "admin"
    "X-User-Roles" = "ADMIN"
}

$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Detail
    )
    $results.Add([PSCustomObject]@{
        name = $Name
        status = $Status
        detail = $Detail
    })
}

function Test-File {
    param(
        [string]$Name,
        [string]$Path
    )
    if (Test-Path $Path) {
        $size = (Get-Item $Path).Length
        Add-Result -Name $Name -Status "PASS" -Detail "$Path ($size bytes)"
    } else {
        Add-Result -Name $Name -Status "FAIL" -Detail "Missing $Path"
    }
}

function Test-Http {
    param(
        [string]$Name,
        [string]$Path,
        [scriptblock]$Validate
    )
    $url = "$BaseUrl$Path"
    try {
        $response = Invoke-WebRequest -Uri $url -Headers $headers -UseBasicParsing -TimeoutSec 5
        $body = $response.Content
        if ($response.StatusCode -lt 200 -or $response.StatusCode -ge 300) {
            Add-Result -Name $Name -Status "FAIL" -Detail "$url returned HTTP $($response.StatusCode)"
            return
        }
        if ($Validate) {
            $validation = & $Validate $body
            if ($validation -ne "OK") {
                Add-Result -Name $Name -Status "FAIL" -Detail "$url validation failed: $validation"
                return
            }
        }
        Add-Result -Name $Name -Status "PASS" -Detail "$url"
    } catch {
        Add-Result -Name $Name -Status "FAIL" -Detail "$url error: $($_.Exception.Message)"
    }
}

function Test-ApiEnvelope {
    param(
        [string]$Name,
        [string]$Path,
        [scriptblock]$ValidateData
    )
    Test-Http -Name $Name -Path $Path -Validate {
        param($Body)
        try {
            $json = $Body | ConvertFrom-Json
        } catch {
            return "response is not JSON"
        }
        if ($json.code -ne 200) {
            return "code=$($json.code)"
        }
        if ($ValidateData) {
            return & $ValidateData $json.data
        }
        return "OK"
    }
}

if (-not $Json) {
    Write-Host "ShopOps health check"
    Write-Host "Workspace: $workspaceRoot"
    Write-Host "BaseUrl: $BaseUrl"
    Write-Host ""
}

Test-File -Name "Olist order summary data" -Path "docs/demo-data/olist/order-summary-olist.json"
Test-File -Name "Olist negative comments data" -Path "docs/demo-data/olist/negative-comments-olist.json"
Test-File -Name "Olist product candidates data" -Path "docs/demo-data/olist/product-candidates-olist.json"
Test-File -Name "Feishu batch evidence" -Path "docs/evaluation/feishu-webhook-batch-summary.json"
Test-File -Name "Excel export evidence" -Path "docs/evaluation/shopops-operation-report-sample.xlsx"

if (-not $SkipHttp) {
    Test-Http -Name "Workbench page" -Path "/admin/workbench.html" -Validate {
        param($Body)
        if ($Body.Contains("/admin/assets/workbench-")) { return "OK" }
        return "missing workbench asset reference"
    }
    Test-ApiEnvelope -Name "System health API" -Path "/api/system/health" -ValidateData {
        param($Data)
        if ($null -eq $Data) { return "missing data" }
        return "OK"
    }
    Test-ApiEnvelope -Name "MCP tool catalog API" -Path "/api/tools" -ValidateData {
        param($Data)
        if ($Data.Count -lt 18) { return "expected at least 18 tools, got $($Data.Count)" }
        return "OK"
    }
    Test-ApiEnvelope -Name "Agent task list API" -Path "/api/agent/tasks?pageNum=1&pageSize=1" -ValidateData {
        param($Data)
        if ($null -eq $Data) { return "missing data" }
        return "OK"
    }
    Test-ApiEnvelope -Name "Report list API" -Path "/api/reports?pageNum=1&pageSize=1" -ValidateData {
        param($Data)
        if ($null -eq $Data) { return "missing data" }
        return "OK"
    }
}

$failed = @($results | Where-Object { $_.status -ne "PASS" })

if ($Json) {
    [PSCustomObject]@{
        generatedAt = (Get-Date).ToString("o")
        baseUrl = $BaseUrl
        passed = $failed.Count -eq 0
        total = $results.Count
        failed = $failed.Count
        results = $results
    } | ConvertTo-Json -Depth 10
} else {
    foreach ($result in $results) {
        $prefix = if ($result.status -eq "PASS") { "[PASS]" } else { "[FAIL]" }
        Write-Host "$prefix $($result.name) - $($result.detail)"
    }
    Write-Host ""
    if ($failed.Count -eq 0) {
        Write-Host "ShopOps health check passed ($($results.Count)/$($results.Count))."
    } else {
        Write-Host "ShopOps health check failed ($($failed.Count)/$($results.Count))."
        exit 1
    }
}
