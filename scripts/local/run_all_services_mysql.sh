#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

if [[ "$runtime_config_mode" == "nacos" ]]; then
  "$repo_root/scripts/nacos/publish_nacos_configs.sh" "$runtime_env"
  export CDD_NACOS_CONFIG_PUBLISHED_FOR="$runtime_env"
fi

runtime_log_dir="$repo_root/.local/backend-runtime/logs"
mkdir -p "$runtime_log_dir"

while IFS='|' read -r service_name _service_module _service_port launcher; do
  log_file="$runtime_log_dir/${service_name}.log"
  echo "Starting ${service_name}; log: ${log_file}"
  bash "$repo_root/scripts/local/$launcher" >"$log_file" 2>&1 &
  echo "$!" >"$runtime_log_dir/${service_name}.launcher.pid"
done < <(backend_runtime_service_catalog)

echo "All ten backend service launchers have been started. Use scripts/local/status_all_services.sh to check health."
