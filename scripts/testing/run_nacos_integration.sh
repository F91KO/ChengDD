#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
compose_file="$repo_root/infrastructure/local/docker-compose.yml"
nacos_addr="${CDD_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
nacos_console_addr="${CDD_NACOS_CONSOLE_ADDR:-127.0.0.1:${CDD_LOCAL_NACOS_CONSOLE_PORT:-8080}}"
nacos_group="${CDD_NACOS_GROUP:-CHENGDD}"
project_namespace="${CDD_NACOS_NAMESPACE:-}"
nacos_username="${CDD_NACOS_USERNAME:-}"
nacos_password="${CDD_NACOS_PASSWORD:-}"
connect_timeout="${CDD_NACOS_CONNECT_TIMEOUT_SECONDS:-2}"
request_timeout="${CDD_NACOS_REQUEST_TIMEOUT_SECONDS:-5}"
shared_data_id="cdd-common-local.yaml"
service_data_id="cdd-nacos-contract-test-local.yaml"
generated_namespace="cdd-nacos-it-$(python3 -c 'import uuid; print(uuid.uuid4().hex)')"
nacos_namespace="${CDD_NACOS_TEST_NAMESPACE_ID:-$generated_namespace}"
namespace_cleanup_required=0
nacos_auth_args=()

[[ "$connect_timeout" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_CONNECT_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$request_timeout" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_NACOS_REQUEST_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$nacos_namespace" =~ ^cdd-nacos-it-[A-Za-z0-9-]+$ ]] || {
  echo "CDD_NACOS_TEST_NAMESPACE_ID must start with cdd-nacos-it- and contain only letters, digits, and hyphens." >&2
  exit 1
}
if [[ -n "$project_namespace" && "$nacos_namespace" == "$project_namespace" ]]; then
  echo "The integration namespace must differ from CDD_NACOS_NAMESPACE." >&2
  exit 1
fi

nacos_curl() {
  local url="$1"
  shift
  curl --connect-timeout "$connect_timeout" --max-time "$request_timeout" "$@" "$url"
}

nacos_console_curl() {
  local path="$1"
  shift
  nacos_curl "http://${nacos_console_addr}${path}" "$@"
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
  args+=(--data-urlencode "tenant=${nacos_namespace}")
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

namespace_exists() {
  local args=(
    --silent
    --show-error
    --fail
    --get
    --data-urlencode "customNamespaceId=${nacos_namespace}"
  )
  if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
    args+=("${nacos_auth_args[@]}")
  fi

  local response
  response="$(nacos_console_curl "/v3/console/core/namespace/exist" "${args[@]}")"
  printf '%s' "$response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
if payload.get("code") != 0 or not isinstance(payload.get("data"), bool):
    raise SystemExit("invalid Nacos namespace existence response")
print("true" if payload["data"] else "false")
'
}

create_test_namespace() {
  local exists
  exists="$(namespace_exists)"
  if [[ "$exists" != "false" ]]; then
    echo "Refusing to reuse existing Nacos integration namespace: ${nacos_namespace}" >&2
    return 1
  fi

  namespace_cleanup_required=1
  local args=(
    --silent
    --show-error
    --fail
    --request POST
    --data-urlencode "customNamespaceId=${nacos_namespace}"
    --data-urlencode "namespaceName=${nacos_namespace}"
    --data-urlencode "namespaceDesc=ChengDD isolated integration test"
  )
  if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
    args+=("${nacos_auth_args[@]}")
  fi

  local response
  response="$(nacos_console_curl "/v3/console/core/namespace" "${args[@]}")"
  printf '%s' "$response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
if payload.get("code") != 0 or payload.get("data") is not True:
    raise SystemExit("Nacos rejected integration namespace creation")
'
}

delete_test_namespace() {
  local exists
  if ! exists="$(namespace_exists)"; then
    echo "Failed to determine whether integration namespace exists: ${nacos_namespace}" >&2
    return 1
  fi
  if [[ "$exists" == "false" ]]; then
    return 0
  fi

  local args=(
    --silent
    --show-error
    --fail
    --request DELETE
    --get
    --data-urlencode "namespaceId=${nacos_namespace}"
  )
  if [[ ${#nacos_auth_args[@]} -gt 0 ]]; then
    args+=("${nacos_auth_args[@]}")
  fi

  local delete_response=""
  local delete_status=0
  delete_response="$(nacos_console_curl "/v3/console/core/namespace" "${args[@]}")" || delete_status=$?

  if ! exists="$(namespace_exists)"; then
    echo "Failed to verify integration namespace cleanup: ${nacos_namespace}" >&2
    return 1
  fi
  if [[ "$exists" == "false" ]]; then
    return 0
  fi
  if [[ "$delete_status" -ne 0 ]]; then
    echo "Failed to delete integration namespace ${nacos_namespace}: request exited ${delete_status}." >&2
    return 1
  fi
  if ! printf '%s' "$delete_response" | python3 -c '
import json
import sys

payload = json.load(sys.stdin)
if payload.get("code") != 0 or payload.get("data") is not True:
    raise SystemExit(1)
'; then
    echo "Nacos rejected integration namespace deletion: ${nacos_namespace}" >&2
    return 1
  fi
  echo "Integration namespace still exists after deletion: ${nacos_namespace}" >&2
  return 1
}

cleanup() {
  local original_status=$?
  local cleanup_status=0
  trap - EXIT
  set +e
  if [[ "$namespace_cleanup_required" -eq 1 ]]; then
    if ! delete_test_namespace; then
      echo "Failed to clean isolated Nacos namespace: ${nacos_namespace}" >&2
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

create_test_namespace

publish_config "$shared_data_id" $'cdd:\n  contract:\n    shared-only: from-common\n    precedence: from-common\n'
publish_config "$service_data_id" $'cdd:\n  contract:\n    precedence: from-service\n    service-only: from-service\n'

set +e
CDD_ENV=local \
CDD_NACOS_SERVER_ADDR="$nacos_addr" \
CDD_NACOS_GROUP="$nacos_group" \
CDD_NACOS_NAMESPACE="$nacos_namespace" \
CDD_NACOS_USERNAME="$nacos_username" \
CDD_NACOS_PASSWORD="$nacos_password" \
mvn -f "$repo_root/cdd-parent/pom.xml" \
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
instance_args+=(--data-urlencode "namespaceId=${nacos_namespace}")
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
