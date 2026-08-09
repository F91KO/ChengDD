#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
runtime_env="${1:-${CDD_ENV:-dev}}"
nacos_addr="${CDD_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
nacos_group="${CDD_NACOS_GROUP:-CHENGDD}"
nacos_namespace="${CDD_NACOS_NAMESPACE:-}"
nacos_username="${CDD_NACOS_USERNAME:-}"
nacos_password="${CDD_NACOS_PASSWORD:-}"
nacos_connect_timeout_seconds="${CDD_NACOS_CONNECT_TIMEOUT_SECONDS:-2}"
nacos_request_timeout_seconds="${CDD_NACOS_REQUEST_TIMEOUT_SECONDS:-5}"
nacos_auth_args=()
nacos_secret_dir=""
nacos_cleanup_started=0

cleanup_nacos_secrets() {
  local primary_status="${1:-$?}"
  local cleanup_status=0
  if [[ "$nacos_cleanup_started" -eq 1 ]]; then
    exit "$primary_status"
  fi
  nacos_cleanup_started=1
  trap - EXIT HUP INT TERM
  if [[ -n "$nacos_secret_dir" && -d "$nacos_secret_dir" ]]; then
    set +e
    rm -rf -- "$nacos_secret_dir"
    cleanup_status=$?
    set -e
  fi
  if [[ "$primary_status" -ne 0 ]]; then
    exit "$primary_status"
  fi
  if [[ "$cleanup_status" -ne 0 ]]; then
    exit "$cleanup_status"
  fi
  exit 0
}

handle_nacos_signal() {
  cleanup_nacos_secrets "$1"
}

trap 'cleanup_nacos_secrets "$?"' EXIT
trap 'handle_nacos_signal 129' HUP
trap 'handle_nacos_signal 130' INT
trap 'handle_nacos_signal 143' TERM

[[ "$nacos_connect_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_CONNECT_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$nacos_request_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_REQUEST_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}

nacos_curl() {
  local url="$1"
  shift
  curl --connect-timeout "$nacos_connect_timeout_seconds" --max-time "$nacos_request_timeout_seconds" "$@" "$url"
}

nacos_authenticate() {
  if [[ -z "$nacos_username" && -z "$nacos_password" ]]; then
    return 0
  fi
  if [[ -z "$nacos_username" || -z "$nacos_password" ]]; then
    echo "CDD_NACOS_USERNAME and CDD_NACOS_PASSWORD must be set together." >&2
    return 1
  fi

  local username_file password_file authorization_file
  local previous_umask
  previous_umask="$(umask)"
  umask 077
  nacos_secret_dir="$(mktemp -d "${TMPDIR:-/tmp}/chengdd-nacos-auth.XXXXXX")"
  username_file="$nacos_secret_dir/username"
  password_file="$nacos_secret_dir/password"
  authorization_file="$nacos_secret_dir/authorization"
  printf '%s' "$nacos_username" >"$username_file"
  printf '%s' "$nacos_password" >"$password_file"
  chmod 600 "$username_file" "$password_file"
  umask "$previous_umask"

  local response
  if ! response="$(nacos_curl "http://${nacos_addr}/nacos/v3/auth/user/login" --silent --show-error --fail --request POST --data-urlencode "username@${username_file}" --data-urlencode "password@${password_file}")"; then
    echo "Nacos authentication failed." >&2
    return 1
  fi
  local access_token
  if ! access_token="$(printf '%s' "$response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
token = payload.get("accessToken")
if not isinstance(token, str) or not token.strip():
    raise SystemExit(1)
print(token)
')"; then
    echo "Nacos authentication response did not contain an access token." >&2
    return 1
  fi
  printf 'Authorization: Bearer %s' "$access_token" >"$authorization_file"
  chmod 600 "$authorization_file"
  nacos_auth_args=(--header "@${authorization_file}")
  unset access_token response
}

service_modules=(
  "cdd-gateway"
  "cdd-auth-service"
  "cdd-merchant-service"
  "cdd-decoration-service"
  "cdd-product-service"
  "cdd-order-service"
  "cdd-marketing-service"
  "cdd-release-service"
  "cdd-report-service"
  "cdd-config-service"
)

config_files=(
  "cdd-common-${runtime_env}.yaml:$repo_root/config/nacos/cdd-common-${runtime_env}.yaml"
)
for module in "${service_modules[@]}"; do
  config_files+=("${module}-${runtime_env}.yaml:$repo_root/cdd-parent/${module}/src/main/resources/application-${runtime_env}.yaml")
done

if [[ "$runtime_env" != "local" && -z "${nacos_namespace//[[:space:]]/}" ]]; then
  echo "CDD_NACOS_NAMESPACE is required for non-local environment: $runtime_env" >&2
  exit 1
fi

for config_file in "${config_files[@]}"; do
  file_path="${config_file#*:}"
  if [[ ! -f "$file_path" ]]; then
    echo "Configuration source file not found: $file_path" >&2
    exit 1
  fi
done

publish_file() {
  local data_id="$1"
  local file_path="$2"

  local url="http://${nacos_addr}/nacos/v1/cs/configs"
  local args=(
    --silent
    --show-error
    --fail
    --request POST
    --data-urlencode "dataId=${data_id}"
    --data-urlencode "group=${nacos_group}"
    --data-urlencode "type=yaml"
    --data-urlencode "content@${file_path}"
  )
  if [[ -n "${nacos_namespace}" ]]; then
    args+=(--data-urlencode "tenant=${nacos_namespace}")
  fi
  local response
  local response_sentinel=$'\x1f'
  if ! response="$(
    set +e
    if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
      args+=("${nacos_auth_args[@]}")
    fi
    nacos_curl "${url}" "${args[@]}"
    curl_status=$?
    printf '%s' "$response_sentinel"
    exit "$curl_status"
  )"; then
    return 1
  fi
  response="${response%"$response_sentinel"}"
  if [[ "$response" != "true" ]]; then
    echo "Nacos rejected ${data_id}: expected response true, got: ${response:-<empty>}" >&2
    return 1
  fi
  echo "published ${data_id}"
}

nacos_authenticate

for config_file in "${config_files[@]}"; do
  data_id="${config_file%%:*}"
  file_path="${config_file#*:}"
  publish_file "$data_id" "$file_path"
done
