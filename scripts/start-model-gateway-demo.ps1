param(
    [string]$BaseUrl = $(if ($env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_BASE_URL) { $env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_BASE_URL } else { "http://localhost:11434/v1" }),
    [string]$ApiKey = $env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_API_KEY,
    [string]$Model = $(if ($env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_DEFAULT_MODEL) { $env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_DEFAULT_MODEL } else { "gpt-4o-mini" }),
    [int]$Port = 8080,
    [ValidateSet("memory", "jdbc")]
    [string]$Persistence = "memory",
    [string]$Profile = "",
    [switch]$AllowNoApiKey
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ApiKey) -and -not $AllowNoApiKey) {
    Write-Host "Missing SHOPOPS_MODEL_OPENAI_COMPATIBLE_API_KEY."
    Write-Host "For local OpenAI-compatible services such as Ollama, add -AllowNoApiKey."
    exit 1
}

$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_ENABLED = "true"
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_BASE_URL = $BaseUrl
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_API_KEY = $ApiKey
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_DEFAULT_MODEL = $Model
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_MAX_ATTEMPTS = "2"
$env:SHOPOPS_MODEL_OPENAI_COMPATIBLE_RETRY_BACKOFF = "500ms"

$env:SHOPOPS_MODEL_GATEWAY_PLANNER_ENABLED = "true"
$env:SHOPOPS_MODEL_GATEWAY_PLANNER_PROVIDER_CODE = "openai-compatible"
$env:SHOPOPS_MODEL_GATEWAY_PLANNER_MODEL_NAME = $Model
$env:SHOPOPS_MODEL_GATEWAY_PLANNER_PROMPT_CODE = "daily_review.plan"

$env:SHOPOPS_MODEL_GATEWAY_REPORT_ENABLED = "true"
$env:SHOPOPS_MODEL_GATEWAY_REPORT_PROVIDER_CODE = "openai-compatible"
$env:SHOPOPS_MODEL_GATEWAY_REPORT_MODEL_NAME = $Model
$env:SHOPOPS_MODEL_GATEWAY_REPORT_PROMPT_CODE = "daily_review.report"

$runArguments = @(
    "--server.port=$Port",
    "--shopops.persistence=$Persistence"
)

$mvnArguments = @("-pl", "shopops-admin", "spring-boot:run")
if (-not [string]::IsNullOrWhiteSpace($Profile)) {
    $mvnArguments += "-Dspring-boot.run.profiles=$Profile"
}
$mvnArguments += "-Dspring-boot.run.arguments=$($runArguments -join ' ')"

Write-Host "Starting ShopOps Admin with real Model Gateway mode enabled."
Write-Host "Base URL: $BaseUrl"
Write-Host "Model: $Model"
Write-Host "Port: $Port"
Write-Host "Persistence: $Persistence"

& mvn @mvnArguments
