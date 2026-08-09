#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/infrastructure/local/docker-compose.yml"
nacos_addr="${CDD_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
nacos_console_addr="${CDD_NACOS_CONSOLE_ADDR:-127.0.0.1:${CDD_LOCAL_NACOS_CONSOLE_PORT:-8080}}"
nacos_group="${CDD_NACOS_GROUP:-CHENGDD}"
nacos_namespace="${CDD_NACOS_NAMESPACE:-}"
nacos_username="${CDD_NACOS_USERNAME:-}"
nacos_password="${CDD_NACOS_PASSWORD:-}"
connect_timeout="${CDD_NACOS_CONNECT_TIMEOUT_SECONDS:-2}"
request_timeout="${CDD_NACOS_REQUEST_TIMEOUT_SECONDS:-5}"
shared_data_id="cdd-common-local.yaml"
service_data_id="cdd-nacos-contract-test-local.yaml"
missing_data_id="cdd-nacos-missing-contract-test-local.yaml"
published_shared=0
published_service=0
nacos_auth_args=()

[[ "$connect_timeout" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_CONNECT_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$request_timeout" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_REQUEST_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}

nacos_curl() {
  local url="$1"
  shift
  curl --connect-timeout "$connect_timeout" --max-time "$request_timeout" "$@" "$url"
}

authenticate() {
  if [[ -z "$nacos_username" && -z "$nacos_password" ]]; then
    return 0
  fi
  if [[ -z "$nacos_username" || -z "$nacos_password" ]]; then
    echo "CDD_NACOS_USERNAME and CDD_NACOS_PASSWORD must be set together." >&2
    return 1
  fi

  local response
  response="$(nacos_curl "http://${nacos_addr}/nacos/v3/auth/user/login" \
    --silent --show-error --fail --request POST \
    --data-urlencode "username=${nacos_username}" \
    --data-urlencode "password=${nacos_password}")"
  local access_token
  access_token="$(printf '%s' "$response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
token = payload.get("accessToken")
if not isinstance(token, str) or not token.strip():
    raise SystemExit(1)
print(token)
')"
  nacos_auth_args=(-H "Authorization: Bearer ${access_token}")
}

config_request() {
  local method="$1"
  local data_id="$2"
  shift 2
  local args=(
    --silent
    --show-error
    --fail
    --request "$method"
    --data-urlencode "dataId=${data_id}"
    --data-urlencode "group=${nacos_group}"
  )
  if [[ -n "$nacos_namespace" ]]; then
    args+=(--data-urlencode "tenant=${nacos_namespace}")
  fi
  if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
    args+=("${nacos_auth_args[@]}")
  fi
  args+=("$@")

  local response
  local response_sentinel=$'\x1f'
  response="$({
    nacos_curl "http://${nacos_addr}/nacos/v1/cs/configs" "${args[@]}"
    request_status=$?
    printf '%s' "$response_sentinel"
    exit "$request_status"
  })"
  response="${response%"$response_sentinel"}"
  if [[ "$response" != "true" ]]; then
    echo "Nacos rejected ${method} for ${data_id}: expected response true, got: ${response:-<empty>}" >&2
    return 1
  fi
}

publish_config() {
  local data_id="$1"
  local content="$2"
  config_request POST "$data_id" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=${content}"
}

delete_config() {
  local data_id="$1"
  config_request DELETE "$data_id"
}

cleanup() {
  local original_status=$?
  local cleanup_status=0
  set +e
  if [[ "$published_service" -eq 1 ]]; then
    if ! delete_config "$service_data_id" >/dev/null; then
      echo "Failed to delete contract DataId: ${service_data_id}" >&2
      cleanup_status=1
    fi
  fi
  if [[ "$published_shared" -eq 1 ]]; then
    if ! delete_config "$shared_data_id" >/dev/null; then
      echo "Failed to delete contract DataId: ${shared_data_id}" >&2
      cleanup_status=1
    fi
  fi
  if [[ "$original_status" -ne 0 ]]; then
    exit "$original_status"
  fi
  exit "$cleanup_status"
}
trap cleanup EXIT

docker compose -f "$compose_file" up -d nacos

health_url="http://${nacos_console_addr}/v3/console/health/liveness"
health_deadline=$(( $(date +%s) + 180 ))
until nacos_curl "$health_url" --silent --show-error --fail >/dev/null 2>&1; do
  if (( $(date +%s) >= health_deadline )); then
    echo "Nacos did not become live within 180 seconds: ${health_url}" >&2
    exit 1
  fi
  sleep 2
done

authenticate

publish_config "$shared_data_id" $'cdd:\n  contract:\n    shared-only: from-common\n    precedence: from-common\n'
published_shared=1
publish_config "$service_data_id" $'cdd:\n  contract:\n    precedence: from-service\n    service-only: from-service\n'
published_service=1
delete_config "$missing_data_id"

set +e
CDD_ENV=local mvn -f "$repo_root/cdd-parent/pom.xml" \
  -pl cdd-common-nacos -am -Pnacos-integration verify
maven_status=$?
set -e

instance_args=(
  --silent
  --show-error
  --fail
  --get
  --data-urlencode "serviceName=cdd-nacos-contract-test"
  --data-urlencode "groupName=${nacos_group}"
)
if [[ -n "$nacos_namespace" ]]; then
  instance_args+=(--data-urlencode "namespaceId=${nacos_namespace}")
fi
if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
  instance_args+=("${nacos_auth_args[@]}")
fi

deregistered=0
deregister_deadline=$(( $(date +%s) + 30 ))
while (( $(date +%s) < deregister_deadline )); do
  instances="$(nacos_curl "http://${nacos_addr}/nacos/v1/ns/instance/list" "${instance_args[@]}")"
  hosts_count="$(printf '%s' "$instances" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
hosts = payload.get("hosts")
if not isinstance(hosts, list):
    raise SystemExit("Nacos instance response has no hosts list")
print(len(hosts))
')"
  if [[ "$hosts_count" -eq 0 ]]; then
    deregistered=1
    break
  fi
  sleep 1
done

if [[ "$deregistered" -ne 1 ]]; then
  echo "Contract service remained registered after application context shutdown." >&2
  exit 1
fi
if [[ "$maven_status" -ne 0 ]]; then
  echo "Nacos integration Maven verification failed with exit code ${maven_status}." >&2
  exit "$maven_status"
fi

echo "Nacos integration contracts passed and the contract service deregistered."
