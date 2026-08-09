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
nacos_group="CHENGDD"

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
  shift
  local args=(--silent --show-error --fail --get)
  while [[ $# -gt 0 ]]; do
    args+=(--data-urlencode "$1")
    shift
  done
  if [[ -n "$nacos_namespace" ]]; then
    args+=(--data-urlencode "tenant=${nacos_namespace}")
  fi
  curl "${args[@]}" "http://${nacos_addr}${path}"
}

shared_config="$(nacos_get "/nacos/v1/cs/configs" "dataId=cdd-common-${runtime_env}.yaml" "group=${nacos_group}")"
if [[ -z "${shared_config//[[:space:]]/}" ]]; then
  echo "Shared Nacos configuration is empty: cdd-common-${runtime_env}.yaml" >&2
  exit 1
fi
echo "loaded cdd-common-${runtime_env}.yaml"

for service in "${service_modules[@]}"; do
  instances="$(nacos_get "/nacos/v1/ns/instance/list" "serviceName=${service}" "groupName=${nacos_group}")"
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
