#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
script_path="$repo_root/scripts/nacos/publish_nacos_configs.ps1"

python3 - "$script_path" <<'PY'
import pathlib
import re
import sys

source = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")

required_contract = [
    "$env:CDD_NACOS_USERNAME",
    "$env:CDD_NACOS_PASSWORD",
    "$env:CDD_NACOS_GROUP",
    "$env:CDD_NACOS_CONNECT_TIMEOUT_SECONDS",
    "$env:CDD_NACOS_REQUEST_TIMEOUT_SECONDS",
    "/nacos/v3/auth/user/login",
    "Authorization",
    "Bearer $accessToken",
    "TimeoutSec",
    "CDD_NACOS_USERNAME and CDD_NACOS_PASSWORD must be set together.",
]
for fragment in required_contract:
    assert fragment in source, f"missing PowerShell Nacos contract: {fragment}"

assert re.search(r"if \(\$response\.Content -cne 'true'\)", source), "publish must accept only exact true"
assert source.count("'cdd-") >= 10, "all service modules must remain in the publisher"

preflight = source.index("foreach ($configFile in $configFiles)")
login = source.index("$authorizationHeaders = Get-NacosAuthorizationHeaders")
publish = source.rindex("foreach ($configFile in $configFiles)")
assert preflight < login < publish, "11-file preflight must complete before login and publishing"

for forbidden in ["Write-Host $nacosUsername", "Write-Host $nacosPassword", "Write-Host $accessToken"]:
    assert forbidden not in source, f"PowerShell publisher must not log secrets: {forbidden}"
PY

if command -v pwsh >/dev/null 2>&1; then
  fixture_root="$(mktemp -d /tmp/chengdd-nacos-pwsh.XXXXXX)"
  trap 'rm -rf "$fixture_root"' EXIT
  fixture_script="$fixture_root/fixture.ps1"
  cat >"$fixture_script" <<'PS'
param([string]$Publisher)
$script:Calls = [System.Collections.Generic.List[object]]::new()

function global:Invoke-WebRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Body,
        [hashtable]$Headers,
        [int]$TimeoutSec
    )
    if ($TimeoutSec -ne 2) { throw "unexpected timeout: $TimeoutSec" }
    $script:Calls.Add([pscustomobject]@{ Method = $Method; Uri = $Uri; Body = $Body; Headers = $Headers })
    if ($Uri.EndsWith('/nacos/v3/auth/user/login')) {
        if ($Body.username -cne $env:CDD_NACOS_USERNAME -or $Body.password -cne $env:CDD_NACOS_PASSWORD) {
            throw 'credential body mismatch'
        }
        return [pscustomobject]@{ Content = '{"accessToken":"fixture-token"}' }
    }
    if ($Headers.Authorization -cne 'Bearer fixture-token') { throw 'authorization header mismatch' }
    return [pscustomobject]@{ Content = 'true' }
}

& $Publisher -EnvName local | Out-Null
if ($script:Calls.Count -ne 12) { throw "expected 12 calls, got $($script:Calls.Count)" }
if (-not $script:Calls[0].Uri.EndsWith('/nacos/v3/auth/user/login')) { throw 'login must precede publish' }
if (($script:Calls | Where-Object { $_.Uri.EndsWith('/nacos/v1/cs/configs') }).Count -ne 11) {
    throw 'expected 11 config publishes'
}
PS
  CDD_NACOS_USERNAME='fixture user+/?&%' \
  CDD_NACOS_PASSWORD='p@ss &=/+?%' \
  CDD_NACOS_CONNECT_TIMEOUT_SECONDS=1 \
  CDD_NACOS_REQUEST_TIMEOUT_SECONDS=2 \
    pwsh -NoProfile -File "$fixture_script" -Publisher "$script_path"
  echo "nacos PowerShell authentication behavior checks passed"
else
  echo "pwsh unavailable; nacos PowerShell authentication static contract checks passed"
fi
