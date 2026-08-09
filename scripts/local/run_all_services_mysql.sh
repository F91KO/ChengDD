#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

runtime_catalog="$(backend_runtime_service_catalog)"
gateway_port="$(backend_runtime_service_port gateway)"
runtime_infra_script="${CDD_RUNTIME_UP_INFRA_SCRIPT:-$repo_root/scripts/local/up_local_infra.sh}"
runtime_publish_script="${CDD_RUNTIME_PUBLISH_SCRIPT:-$repo_root/scripts/nacos/publish_nacos_configs.sh}"
runtime_migrate_script="${CDD_RUNTIME_MIGRATE_SCRIPT:-$repo_root/scripts/db/migrate.sh}"
runtime_launcher_dir="${CDD_RUNTIME_LAUNCHER_DIR:-$repo_root/scripts/local}"
runtime_state_dir="$(backend_runtime_state_dir "$repo_root")"

health_timeout_seconds="${CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS:-60}"
cleanup_reserve_seconds="${CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS:-5}"
[[ "$health_timeout_seconds" =~ ^[1-9][0-9]{0,4}$ ]] || { echo "CDD_RUNTIME_HEALTH_TIMEOUT_SECONDS must be a positive integer." >&2; exit 1; }
[[ "$cleanup_reserve_seconds" =~ ^[1-9][0-9]{0,4}$ ]] || { echo "CDD_RUNTIME_STARTUP_CLEANUP_RESERVE_SECONDS must be a positive integer." >&2; exit 1; }
if (( cleanup_reserve_seconds >= health_timeout_seconds )); then
  cleanup_reserve_seconds=$(( health_timeout_seconds > 1 ? health_timeout_seconds - 1 : 1 ))
fi
export CDD_LOCAL_NACOS_CONSOLE_PORT="${CDD_LOCAL_NACOS_CONSOLE_PORT:-18080}"
if [[ "$CDD_LOCAL_NACOS_CONSOLE_PORT" == "$gateway_port" ]]; then
  echo "CDD_LOCAL_NACOS_CONSOLE_PORT conflicts with CDD_GATEWAY_SERVER_PORT: ${gateway_port}" >&2
  exit 1
fi

stale_runtime_files=()
validate_existing_runtime_state() {
  local service_name="$1"
  local module_name="$2"
  local launcher_name="$3"
  local service_state launcher_state expected_jar_path
  service_state="$(backend_runtime_state_file "$repo_root" "$service_name")"
  launcher_state="$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
  expected_jar_path="$repo_root/cdd-parent/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar"

  if [[ -f "$service_state" ]]; then
    if ! read_backend_runtime_state "$service_state"; then
      echo "Refusing malformed existing service state: ${service_state}" >&2
      return 1
    fi
    if [[ "$RUNTIME_STATE_SERVICE_NAME" != "$service_name" || "$RUNTIME_STATE_MODULE_NAME" != "$module_name" || "$RUNTIME_STATE_JAR_PATH" != "$expected_jar_path" ]]; then
      echo "Refusing mismatched existing service state: ${service_state}" >&2
      return 1
    fi
    if kill -0 "$RUNTIME_STATE_SERVICE_PID" >/dev/null 2>&1; then
      if backend_runtime_process_matches_state "$service_name" "$module_name" "$RUNTIME_STATE_SERVICE_PORT" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" "$RUNTIME_STATE_JAVA_PATH" "$RUNTIME_STATE_JAR_PATH"; then
        echo "Refusing to replace live existing ${service_name} service pid=${RUNTIME_STATE_SERVICE_PID} port=${RUNTIME_STATE_SERVICE_PORT}." >&2
      else
        echo "Refusing unprovable existing ${service_name} service pid=${RUNTIME_STATE_SERVICE_PID}." >&2
      fi
      return 1
    fi
    stale_runtime_files+=("$service_state")
  fi

  if [[ -f "$launcher_state" ]]; then
    if ! read_backend_runtime_launcher_state "$launcher_state"; then
      echo "Refusing malformed existing launcher state: ${launcher_state}" >&2
      return 1
    fi
    if [[ "$RUNTIME_LAUNCHER_SERVICE_NAME" != "$service_name" || "$RUNTIME_LAUNCHER_NAME" != "$launcher_name" ]]; then
      echo "Refusing mismatched existing launcher state: ${launcher_state}" >&2
      return 1
    fi
    if kill -0 "$RUNTIME_LAUNCHER_PID" >/dev/null 2>&1; then
      if backend_runtime_launcher_matches_state "$service_name" "$launcher_name" "$RUNTIME_LAUNCHER_PID" "$RUNTIME_LAUNCHER_START_MARKER"; then
        echo "Refusing to replace live existing ${service_name} launcher pid=${RUNTIME_LAUNCHER_PID}." >&2
      else
        echo "Refusing unprovable existing ${service_name} launcher pid=${RUNTIME_LAUNCHER_PID}." >&2
      fi
      return 1
    fi
    stale_runtime_files+=("$launcher_state")
  fi
}

while IFS='|' read -r service_name module_name _service_port launcher_name; do
  validate_existing_runtime_state "$service_name" "$module_name" "$launcher_name" || exit 1
done <<<"$runtime_catalog"
if [[ ${#stale_runtime_files[@]} -gt 0 ]]; then
  for stale_runtime_file in "${stale_runtime_files[@]}"; do
    rm -f "$stale_runtime_file"
  done
fi

while IFS='|' read -r service_name _service_module service_port _launcher; do
  if ! backend_runtime_assert_port_available "$service_port"; then
    echo "Backend preflight refused ${service_name} port ${service_port}." >&2
    exit 1
  fi
done <<<"$runtime_catalog"

bash "$runtime_infra_script"

if [[ "$runtime_config_mode" == "nacos" ]]; then
  bash "$runtime_publish_script" "$runtime_env"
  export CDD_NACOS_CONFIG_PUBLISHED_FOR="$runtime_env"
fi

bash "$runtime_migrate_script"

runtime_log_dir="$runtime_state_dir/logs"
mkdir -p "$runtime_log_dir"

while IFS='|' read -r service_name _service_module _service_port launcher; do
  log_file="$runtime_log_dir/${service_name}.log"
  echo "Starting ${service_name}; log: ${log_file}"
  bash "$runtime_launcher_dir/$launcher" >"$log_file" 2>&1 &
  launcher_pid=$!
  launcher_deadline=$(( $(date +%s) + health_timeout_seconds ))
  readiness_deadline=$(( launcher_deadline - cleanup_reserve_seconds ))
  if ! record_backend_launcher_state "$repo_root" "$service_name" "$launcher" "$launcher_pid"; then
    kill -KILL "$launcher_pid" >/dev/null 2>&1 || true
    wait "$launcher_pid" >/dev/null 2>&1 || true
    exit 1
  fi
  if ! wait_for_runtime_service_health "$repo_root" "$service_name" "$_service_module" "$_service_port" "$launcher_pid" "$readiness_deadline"; then
    echo "Backend lifecycle failed while starting ${service_name}; see ${log_file}." >&2
    launcher_state_file="$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
    launcher_cleanup_ok=1
    if ! read_backend_runtime_launcher_state "$launcher_state_file"; then
      echo "Cannot read exact launcher state for ${service_name}." >&2
      launcher_cleanup_ok=0
    elif [[ "$RUNTIME_LAUNCHER_PID" != "$launcher_pid" ]]; then
      echo "Launcher PID state changed for ${service_name}." >&2
      launcher_cleanup_ok=0
    elif ! backend_runtime_launcher_matches_state "$service_name" "$launcher" "$launcher_pid" "$RUNTIME_LAUNCHER_START_MARKER"; then
      echo "Launcher ownership proof failed for ${service_name}." >&2
      launcher_cleanup_ok=0
    elif ! backend_runtime_terminate_owned_tree "$launcher_pid" "$RUNTIME_LAUNCHER_START_MARKER" "$launcher_deadline" "$service_name launcher"; then
      echo "Owned launcher tree cleanup exceeded its deadline for ${service_name}." >&2
      launcher_cleanup_ok=0
    fi
    if [[ "$launcher_cleanup_ok" -ne 1 ]]; then
      echo "Failed to clean up exact owned launcher tree for ${service_name}." >&2
      exit 1
    fi
    rm -f "$launcher_state_file"
    exit 1
  fi
done <<<"$runtime_catalog"

echo "All ten backend services are HTTP healthy. Use scripts/local/status_all_services.sh to check health."
