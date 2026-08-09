param(
    [string]$EnvName = $(if ($env:CDD_ENV) { $env:CDD_ENV } else { 'dev' })
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$nacosAddr = if ($env:CDD_NACOS_SERVER_ADDR) { $env:CDD_NACOS_SERVER_ADDR } else { '127.0.0.1:8848' }
$nacosGroup = if ($env:CDD_NACOS_GROUP) { $env:CDD_NACOS_GROUP } else { 'CHENGDD' }
$nacosNamespace = if ($env:CDD_NACOS_NAMESPACE) { $env:CDD_NACOS_NAMESPACE } else { '' }
$nacosUsername = if ($env:CDD_NACOS_USERNAME) { $env:CDD_NACOS_USERNAME } else { '' }
$nacosPassword = if ($env:CDD_NACOS_PASSWORD) { $env:CDD_NACOS_PASSWORD } else { '' }
$nacosConnectTimeoutSeconds = if ($env:CDD_NACOS_CONNECT_TIMEOUT_SECONDS) { $env:CDD_NACOS_CONNECT_TIMEOUT_SECONDS } else { '2' }
$nacosRequestTimeoutSeconds = if ($env:CDD_NACOS_REQUEST_TIMEOUT_SECONDS) { $env:CDD_NACOS_REQUEST_TIMEOUT_SECONDS } else { '5' }
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

if ($nacosConnectTimeoutSeconds -notmatch '^[1-9][0-9]*$') {
    throw 'CDD_NACOS_CONNECT_TIMEOUT_SECONDS must be a positive integer.'
}
if ($nacosRequestTimeoutSeconds -notmatch '^[1-9][0-9]*$') {
    throw 'CDD_NACOS_REQUEST_TIMEOUT_SECONDS must be a positive integer.'
}
if ([string]::IsNullOrEmpty($nacosUsername) -xor [string]::IsNullOrEmpty($nacosPassword)) {
    throw 'CDD_NACOS_USERNAME and CDD_NACOS_PASSWORD must be set together.'
}

function Invoke-NacosWebRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Body,
        [hashtable]$Headers
    )

    $requestParameters = @{
        Method = $Method
        Uri = $Uri
        TimeoutSec = [int]$nacosRequestTimeoutSeconds
    }
    if ($Body) {
        $requestParameters.Body = $Body
    }
    if ($Headers -and $Headers.Count -gt 0) {
        $requestParameters.Headers = $Headers
    }
    if ((Get-Command Invoke-WebRequest).Parameters.ContainsKey('ConnectionTimeoutSeconds')) {
        $requestParameters.ConnectionTimeoutSeconds = [int]$nacosConnectTimeoutSeconds
    }
    Invoke-WebRequest @requestParameters
}

function Get-NacosAuthorizationHeaders {
    if ([string]::IsNullOrEmpty($nacosUsername)) {
        return @{}
    }

    try {
        $response = Invoke-NacosWebRequest -Method Post -Uri "http://$nacosAddr/nacos/v3/auth/user/login" -Body @{
            username = $nacosUsername
            password = $nacosPassword
        }
        $payload = $response.Content | ConvertFrom-Json
        $accessToken = $payload.accessToken
    }
    catch {
        throw 'Nacos authentication failed.'
    }
    if ([string]::IsNullOrWhiteSpace($accessToken)) {
        throw 'Nacos authentication response did not contain an access token.'
    }
    return @{ Authorization = "Bearer $accessToken" }
}

function Publish-ConfigFile {
    param(
        [string]$DataId,
        [string]$FilePath,
        [hashtable]$AuthorizationHeaders
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

    $response = Invoke-NacosWebRequest -Method Post -Uri "http://$nacosAddr/nacos/v1/cs/configs" -Body $body -Headers $AuthorizationHeaders
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

$authorizationHeaders = Get-NacosAuthorizationHeaders
foreach ($configFile in $configFiles) {
    Publish-ConfigFile -DataId $configFile.DataId -FilePath $configFile.FilePath -AuthorizationHeaders $authorizationHeaders
}
