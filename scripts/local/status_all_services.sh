#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

healthy_count=0
while IFS='|' read -r service_name _service_module service_port _launcher; do
  health_url="http://127.0.0.1:${service_port}/actuator/health"
  if curl -fsS "$health_url" >/dev/null 2>&1; then
    healthy_count=$((healthy_count + 1))
    echo "healthy ${service_name} port=${service_port}"
  else
    echo "unhealthy ${service_name} port=${service_port}"
  fi
done < <(backend_runtime_service_catalog)

echo "HTTP healthy services: ${healthy_count}/10"

nacos_state_ok=1
if [[ "$runtime_config_mode" == "nacos" ]]; then
  if [[ "$healthy_count" -gt 0 ]]; then
    if ! "$repo_root/scripts/nacos/check_nacos_state.sh" "$runtime_env" running; then
      nacos_state_ok=0
    fi
  elif ! "$repo_root/scripts/nacos/check_nacos_state.sh" "$runtime_env" stopped; then
    nacos_state_ok=0
  fi
fi

if [[ "$nacos_state_ok" -ne 1 ]]; then
  exit 1
fi

if [[ "$healthy_count" -eq 0 || "$healthy_count" -eq 10 ]]; then
  exit 0
fi

exit 1
