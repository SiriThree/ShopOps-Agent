param(
    [switch]$SkipCompile
)

$ErrorActionPreference = "Stop"
$workspaceRoot = Split-Path -Parent $PSScriptRoot
$mainClass = "com.sirithree.shopops.admin.mcp.stdio.ShopOpsMcpStdioServerApplication"

function Import-DotEnv {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return
    }
    foreach ($line in Get-Content $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed -split "=", 2
        if ($parts.Length -ne 2) {
            continue
        }
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($value.Length -ge 2) {
            $first = $value.Substring(0, 1)
            $last = $value.Substring($value.Length - 1, 1)
            if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
                $value = $value.Substring(1, $value.Length - 2)
            }
        }
        if ($key -match "^[A-Za-z_][A-Za-z0-9_]*$" -and [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($key, "Process"))) {
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
}

Set-Location $workspaceRoot
Import-DotEnv -Path (Join-Path $workspaceRoot ".env")

$mavenArguments = @(
    "-q",
    "-pl",
    "shopops-admin"
)

if (-not $SkipCompile) {
    $mavenArguments += "-DskipTests"
    $mavenArguments += "compile"
}

$mavenArguments += "spring-boot:run"
$mavenArguments += "-Dspring-boot.run.main-class=$mainClass"
$mavenArguments += "-Dspring-boot.run.arguments=--shopops.persistence=memory --shopops.flyway.enabled=false --spring.flyway.enabled=false"

& mvn @mavenArguments
exit $LASTEXITCODE
