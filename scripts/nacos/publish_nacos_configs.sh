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

publish_file() {
  local data_id="$1"
  local file_path="$2"
  [[ -f "$file_path" ]] || return 0

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
  curl "${args[@]}" "${url}" >/dev/null
  echo "published ${data_id}"
}

publish_file "cdd-common-${runtime_env}.yaml" "$repo_root/config/nacos/cdd-common-${runtime_env}.yaml"

for module in "${service_modules[@]}"; do
  publish_file "${module}-${runtime_env}.yaml" "$repo_root/cdd-parent/${module}/src/main/resources/application-${runtime_env}.yaml"
done
