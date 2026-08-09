#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

gateway_port="${CDD_GATEWAY_SERVER_PORT:-8080}"
export CDD_LOCAL_NACOS_CONSOLE_PORT="${CDD_LOCAL_NACOS_CONSOLE_PORT:-18080}"
if [[ "$CDD_LOCAL_NACOS_CONSOLE_PORT" == "$gateway_port" ]]; then
  echo "CDD_LOCAL_NACOS_CONSOLE_PORT conflicts with CDD_GATEWAY_SERVER_PORT: ${gateway_port}" >&2
  exit 1
fi

runtime_infra_script="${CDD_RUNTIME_UP_INFRA_SCRIPT:-$repo_root/scripts/local/up_local_infra.sh}"
runtime_publish_script="${CDD_RUNTIME_PUBLISH_SCRIPT:-$repo_root/scripts/nacos/publish_nacos_configs.sh}"
runtime_migrate_script="${CDD_RUNTIME_MIGRATE_SCRIPT:-$repo_root/scripts/db/migrate.sh}"
runtime_launcher_dir="${CDD_RUNTIME_LAUNCHER_DIR:-$repo_root/scripts/local}"

bash "$runtime_infra_script"

if [[ "$runtime_config_mode" == "nacos" ]]; then
  bash "$runtime_publish_script" "$runtime_env"
  export CDD_NACOS_CONFIG_PUBLISHED_FOR="$runtime_env"
fi

bash "$runtime_migrate_script"

runtime_state_dir="$(backend_runtime_state_dir "$repo_root")"
runtime_log_dir="$runtime_state_dir/logs"
mkdir -p "$runtime_log_dir"

while IFS='|' read -r service_name _service_module _service_port launcher; do
  log_file="$runtime_log_dir/${service_name}.log"
  echo "Starting ${service_name}; log: ${log_file}"
  bash "$runtime_launcher_dir/$launcher" >"$log_file" 2>&1 &
  launcher_pid=$!
  record_backend_launcher_state "$repo_root" "$service_name" "$launcher" "$launcher_pid"
  if ! wait_for_runtime_service_health "$service_name" "$_service_port" "$launcher_pid"; then
    echo "Backend lifecycle failed while starting ${service_name}; see ${log_file}." >&2
    exit 1
  fi
done < <(backend_runtime_service_catalog)

echo "All ten backend services are HTTP healthy. Use scripts/local/status_all_services.sh to check health."
