#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

service_port_is_listening() {
  local service_port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    return 1
  fi
  lsof -tiTCP:"$service_port" -sTCP:LISTEN >/dev/null 2>&1
}

process_is_running() {
  local process_id="$1"
  [[ -n "$process_id" ]] && kill -0 "$process_id" >/dev/null 2>&1
}

terminate_process_tree() {
  local process_id="$1"
  local child_process_id

  if command -v pgrep >/dev/null 2>&1; then
    while IFS= read -r child_process_id; do
      [[ -n "$child_process_id" ]] || continue
      terminate_process_tree "$child_process_id"
    done < <(pgrep -P "$process_id" 2>/dev/null || true)
  fi
  kill "$process_id" >/dev/null 2>&1 || true
}

service_is_running() {
  local service_name="$1"
  local service_port="$2"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  local launcher_pid_file="$repo_root/.local/backend-runtime/logs/${service_name}.launcher.pid"
  local process_id=""

  if [[ -f "$launcher_pid_file" ]]; then
    process_id="$(<"$launcher_pid_file")"
    if process_is_running "$process_id"; then
      return 0
    fi
  fi
  if [[ -f "$state_file" ]]; then
    unset SERVICE_PID
    source "$state_file"
    if process_is_running "${SERVICE_PID:-}"; then
      return 0
    fi
  fi
  service_port_is_listening "$service_port"
}

stop_service() {
  local service_name="$1"
  local service_port="$2"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  local launcher_pid_file="$repo_root/.local/backend-runtime/logs/${service_name}.launcher.pid"
  local launcher_pid=""
  local service_pid=""

  if [[ -f "$launcher_pid_file" ]]; then
    launcher_pid="$(<"$launcher_pid_file")"
    if process_is_running "$launcher_pid"; then
      echo "Stopping ${service_name} launcher pid=${launcher_pid}"
      terminate_process_tree "$launcher_pid"
    fi
  fi

  if [[ -f "$state_file" ]]; then
    unset SERVICE_PID
    source "$state_file"
    service_pid="${SERVICE_PID:-}"
  fi

  if [[ -n "$service_pid" ]] && kill -0 "$service_pid" >/dev/null 2>&1; then
    echo "Stopping ${service_name} pid=${service_pid}"
    kill "$service_pid" >/dev/null 2>&1 || true
  fi

  if command -v lsof >/dev/null 2>&1; then
    local listening_pid
    while IFS= read -r listening_pid; do
      [[ -n "$listening_pid" ]] || continue
      echo "Stopping ${service_name} port=${service_port} pid=${listening_pid}"
      kill "$listening_pid" >/dev/null 2>&1 || true
    done < <(lsof -tiTCP:"$service_port" -sTCP:LISTEN 2>/dev/null || true)
  fi
}

catalog_entries=()
while IFS= read -r entry; do
  catalog_entries+=("$entry")
done < <(backend_runtime_service_catalog)

for ((index=${#catalog_entries[@]} - 1; index >= 0; index--)); do
  IFS='|' read -r service_name _service_module service_port _launcher <<<"${catalog_entries[$index]}"
  stop_service "$service_name" "$service_port"
done

services_stopped=0
for _ in $(seq 1 30); do
  services_stopped=1
  while IFS='|' read -r service_name _service_module service_port _launcher; do
    if service_is_running "$service_name" "$service_port"; then
      services_stopped=0
      break
    fi
  done < <(backend_runtime_service_catalog)
  [[ "$services_stopped" -eq 1 ]] && break
  sleep 1
done

if [[ "$services_stopped" -ne 1 ]]; then
  echo "Timed out waiting for backend services to stop." >&2
fi

while IFS='|' read -r service_name _service_module service_port _launcher; do
  if ! service_is_running "$service_name" "$service_port"; then
    remove_backend_runtime_state "$repo_root" "$service_name"
    rm -f "$repo_root/.local/backend-runtime/logs/${service_name}.launcher.pid"
  fi
done < <(backend_runtime_service_catalog)

nacos_stopped=1
if [[ "$runtime_config_mode" == "nacos" ]]; then
  nacos_stopped=0
  for _ in $(seq 1 30); do
    if "$repo_root/scripts/nacos/check_nacos_state.sh" "$runtime_env" stopped; then
      nacos_stopped=1
      break
    fi
    sleep 1
  done
  if [[ "$nacos_stopped" -ne 1 ]]; then
    echo "Timed out waiting for Nacos instances to deregister." >&2
  fi
fi

if [[ "$services_stopped" -ne 1 || "$nacos_stopped" -ne 1 ]]; then
  exit 1
fi

echo "All backend services are stopped."
