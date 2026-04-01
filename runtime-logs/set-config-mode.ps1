if (-not $env:CDD_ENV) {
    $env:CDD_ENV = 'local'
}

if (-not $env:CDD_CONFIG_MODE) {
    $env:CDD_CONFIG_MODE = 'file'
}

if ($env:CDD_CONFIG_MODE -ieq 'nacos') {
    if (-not $env:CDD_NACOS_SERVER_ADDR) {
        $env:CDD_NACOS_SERVER_ADDR = '127.0.0.1:8848'
    }
    if (-not $env:CDD_NACOS_GROUP) {
        $env:CDD_NACOS_GROUP = 'CHENGDD'
    }
    if (-not $env:CDD_NACOS_NAMESPACE) {
        if ($env:CDD_ENV -ieq 'local') {
            Remove-Item Env:CDD_NACOS_NAMESPACE -ErrorAction SilentlyContinue
        } else {
            $env:CDD_NACOS_NAMESPACE = "chengdd-$($env:CDD_ENV)"
        }
    }
}
