param(
    [string]$EnvName = $(if ($env:CDD_ENV) { $env:CDD_ENV } else { 'dev' })
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$nacosAddr = if ($env:CDD_NACOS_SERVER_ADDR) { $env:CDD_NACOS_SERVER_ADDR } else { '127.0.0.1:8848' }
$nacosGroup = if ($env:CDD_NACOS_GROUP) { $env:CDD_NACOS_GROUP } else { 'CHENGDD' }
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
        return
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

    Invoke-RestMethod -Method Post -Uri "http://$nacosAddr/nacos/v1/cs/configs" -Body $body | Out-Null
    Write-Host "published $DataId"
}

Publish-ConfigFile -DataId "cdd-common-$EnvName.yaml" -FilePath (Join-Path $repoRoot "config\\nacos\\cdd-common-$EnvName.yaml")

foreach ($module in $serviceModules) {
    Publish-ConfigFile `
        -DataId "$module-$EnvName.yaml" `
        -FilePath (Join-Path $repoRoot "cdd-parent\\$module\\src\\main\\resources\\application-$EnvName.yaml")
}
