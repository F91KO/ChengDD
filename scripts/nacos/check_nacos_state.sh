#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: $0 <env> [running|stopped]" >&2
  exit 1
fi

runtime_env="$1"
expected_state="${2:-running}"
case "$expected_state" in
  running|stopped) ;;
  *)
    echo "Expected state must be running or stopped: $expected_state" >&2
    exit 1
    ;;
esac

nacos_addr="${CDD_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
nacos_namespace="${CDD_NACOS_NAMESPACE:-}"
nacos_group="${CDD_NACOS_GROUP:-CHENGDD}"
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
  local request_timeout="$nacos_request_timeout_seconds"
  if [[ -n "${CDD_NACOS_DEADLINE_EPOCH:-}" ]]; then
    [[ "$CDD_NACOS_DEADLINE_EPOCH" =~ ^[0-9]+$ ]] || return 1
    local remaining=$(( CDD_NACOS_DEADLINE_EPOCH - $(date +%s) ))
    (( remaining > 0 )) || return 1
    (( remaining < request_timeout )) && request_timeout="$remaining"
  fi
  curl --connect-timeout "$nacos_connect_timeout_seconds" --max-time "$request_timeout" "$@" "$url"
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

nacos_get() {
  local path="$1"
  local namespace_parameter="$2"
  shift 2
  local args=(--silent --show-error --fail --get)
  while [[ $# -gt 0 ]]; do
    args+=(--data-urlencode "$1")
    shift
  done
  if [[ -n "$nacos_namespace" && -n "$namespace_parameter" ]]; then
    args+=(--data-urlencode "${namespace_parameter}=${nacos_namespace}")
  fi
  if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
    args+=("${nacos_auth_args[@]}")
  fi
  nacos_curl "http://${nacos_addr}${path}" "${args[@]}"
}

nacos_authenticate

shared_config="$(nacos_get "/nacos/v1/cs/configs" tenant "dataId=cdd-common-${runtime_env}.yaml" "group=${nacos_group}")"
if [[ -z "${shared_config//[[:space:]]/}" ]]; then
  echo "Shared Nacos configuration is empty: cdd-common-${runtime_env}.yaml" >&2
  exit 1
fi
echo "loaded cdd-common-${runtime_env}.yaml"

for service in "${service_modules[@]}"; do
  instances="$(nacos_get "/nacos/v1/ns/instance/list" namespaceId "serviceName=${service}" "groupName=${nacos_group}")"
  hosts_count="$(printf '%s' "$instances" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
hosts = payload.get("hosts")
if not isinstance(hosts, list):
    raise SystemExit("Nacos instance response has no hosts list")
print(len(hosts))
')"

  if [[ "$expected_state" == "running" ]]; then
    echo "registered ${service} hosts=${hosts_count}"
    if [[ "$hosts_count" -eq 0 ]]; then
      echo "Service is not registered: ${service}" >&2
      exit 1
    fi
  else
    echo "deregistered ${service}"
    if [[ "$hosts_count" -gt 0 ]]; then
      echo "Service is still registered: ${service} hosts=${hosts_count}" >&2
      exit 1
    fi
  fi
done
