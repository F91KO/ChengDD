#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
runtime_env="${1:-${CDD_ENV:-dev}}"
nacos_addr="${CDD_NACOS_SERVER_ADDR:-127.0.0.1:8848}"
nacos_group="${CDD_NACOS_GROUP:-CHENGDD}"
nacos_namespace="${CDD_NACOS_NAMESPACE:-}"

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
    curl "${args[@]}" "${url}"
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

for config_file in "${config_files[@]}"; do
  data_id="${config_file%%:*}"
  file_path="${config_file#*:}"
  publish_file "$data_id" "$file_path"
done
