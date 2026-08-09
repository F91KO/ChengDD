param(
    [string]$EnvName = $(if ($env:CDD_ENV) { $env:CDD_ENV } else { 'dev' })
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$nacosAddr = if ($env:CDD_NACOS_SERVER_ADDR) { $env:CDD_NACOS_SERVER_ADDR } else { '127.0.0.1:8848' }
$nacosGroup = 'CHENGDD'
$nacosNamespace = if ($env:CDD_NACOS_NAMESPACE) { $env:CDD_NACOS_NAMESPACE } else { '' }
$serviceModules = @(
    'cdd-gateway',
    'cdd-auth-service',
    'cdd-merchant-service',
    'cdd-decoration-service',
    'cdd-product-service',
    'cdd-order-service',
    'cdd-marketing-service',
    'cdd-release-service',
    'cdd-report-service',
    'cdd-config-service'
)

function Publish-ConfigFile {
    param(
        [string]$DataId,
        [string]$FilePath
    )

    if (-not (Test-Path -LiteralPath $FilePath)) {
        throw "Configuration source file not found: $FilePath"
    }

    $body = @{
        dataId = $DataId
        group = $nacosGroup
        type = 'yaml'
        content = Get-Content -LiteralPath $FilePath -Raw
    }
    if ($nacosNamespace) {
        $body.tenant = $nacosNamespace
    }

    $response = Invoke-WebRequest -Method Post -Uri "http://$nacosAddr/nacos/v1/cs/configs" -Body $body
    if ($response.Content -cne 'true') {
        throw "Nacos rejected $DataId: expected response true, got: $($response.Content)"
    }
    Write-Host "published $DataId"
}

$configFiles = @(
    @{ DataId = "cdd-common-$EnvName.yaml"; FilePath = (Join-Path $repoRoot "config\\nacos\\cdd-common-$EnvName.yaml") }
)

foreach ($module in $serviceModules) {
    $configFiles += @{ DataId = "$module-$EnvName.yaml"; FilePath = (Join-Path $repoRoot "cdd-parent\\$module\\src\\main\\resources\\application-$EnvName.yaml") }
}

if ($EnvName -ne 'local' -and [string]::IsNullOrWhiteSpace($nacosNamespace)) {
    throw "CDD_NACOS_NAMESPACE is required for non-local environment: $EnvName"
}

foreach ($configFile in $configFiles) {
    if (-not (Test-Path -LiteralPath $configFile.FilePath)) {
        throw "Configuration source file not found: $($configFile.FilePath)"
    }
}

foreach ($configFile in $configFiles) {
    Publish-ConfigFile -DataId $configFile.DataId -FilePath $configFile.FilePath
}
