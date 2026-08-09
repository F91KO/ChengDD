#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
source "$repo_root/scripts/local/backend_runtime_guard.sh"

configure_backend_runtime

stop_timeout_seconds="${CDD_RUNTIME_STOP_TIMEOUT_SECONDS:-30}"
term_grace_seconds="${CDD_RUNTIME_TERM_GRACE_SECONDS:-5}"
[[ "$stop_timeout_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_RUNTIME_STOP_TIMEOUT_SECONDS must be a positive integer." >&2
  exit 1
}
[[ "$term_grace_seconds" =~ ^[1-9][0-9]*$ ]] || {
  echo "CDD_RUNTIME_TERM_GRACE_SECONDS must be a positive integer." >&2
  exit 1
}

owned_java_pid=""
owned_java_marker=""
owned_launcher_pid=""
owned_launcher_marker=""

load_owned_java() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local state_file
  state_file="$(backend_runtime_state_file "$repo_root" "$service_name")"
  owned_java_pid=""
  owned_java_marker=""

  [[ -f "$state_file" ]] || return 1
  if ! read_backend_runtime_state "$state_file"; then
    echo "Refusing malformed runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if [[ "$RUNTIME_STATE_SERVICE_NAME" != "$service_name" || "$RUNTIME_STATE_MODULE_NAME" != "$module_name" || "$RUNTIME_STATE_SERVICE_PORT" != "$service_port" ]]; then
    echo "Refusing mismatched runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if ! kill -0 "$RUNTIME_STATE_SERVICE_PID" >/dev/null 2>&1; then
    return 1
  fi
  if ! backend_runtime_process_matches_state "$service_name" "$module_name" "$service_port" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER"; then
    echo "Refusing unowned runtime process for ${service_name} pid=${RUNTIME_STATE_SERVICE_PID}." >&2
    return 2
  fi
  owned_java_pid="$RUNTIME_STATE_SERVICE_PID"
  owned_java_marker="$RUNTIME_STATE_PROCESS_START_MARKER"
  return 0
}

load_owned_launcher() {
  local service_name="$1"
  local launcher_name="$2"
  local state_file
  state_file="$(backend_runtime_launcher_state_file "$repo_root" "$service_name")"
  owned_launcher_pid=""
  owned_launcher_marker=""

  [[ -f "$state_file" ]] || return 1
  if ! read_backend_runtime_launcher_state "$state_file"; then
    echo "Refusing malformed launcher state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if [[ "$RUNTIME_LAUNCHER_SERVICE_NAME" != "$service_name" || "$RUNTIME_LAUNCHER_NAME" != "$launcher_name" ]]; then
    echo "Refusing mismatched launcher state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if ! kill -0 "$RUNTIME_LAUNCHER_PID" >/dev/null 2>&1; then
    return 1
  fi
  if ! backend_runtime_launcher_matches_state "$service_name" "$launcher_name" "$RUNTIME_LAUNCHER_PID" "$RUNTIME_LAUNCHER_START_MARKER"; then
    echo "Refusing unowned launcher process for ${service_name} pid=${RUNTIME_LAUNCHER_PID}." >&2
    return 2
  fi
  owned_launcher_pid="$RUNTIME_LAUNCHER_PID"
  owned_launcher_marker="$RUNTIME_LAUNCHER_START_MARKER"
  return 0
}

find_unmanaged_service_process() {
  local module_name="$1"
  local service_port="$2"
  command -v ps >/dev/null 2>&1 || return 2
  local process_list
  if ! process_list="$(ps -ax -o pid= -o command= 2>/dev/null)"; then
    return 2
  fi
  if printf '%s\n' "$process_list" | awk -v module="$module_name" -v port="$service_port" '
    index($0, module "-0.1.0-SNAPSHOT.jar") && index($0, "--server.port=" port) { found=1 }
    END { exit found ? 0 : 1 }
  '; then
    return 0
  fi
  return 1
}

signal_owned_process() {
  local service_pid="$1"
  local start_marker="$2"
  local signal_name="$3"
  kill -0 "$service_pid" >/dev/null 2>&1 || return 0
  [[ "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1
  kill "-$signal_name" "$service_pid" >/dev/null 2>&1
}

terminate_owned_process() {
  local service_pid="$1"
  local start_marker="$2"
  local process_label="$3"
  echo "Stopping owned ${process_label} pid=${service_pid}"
  signal_owned_process "$service_pid" "$start_marker" TERM || return 1

  local deadline=$(( $(date +%s) + term_grace_seconds ))
  while kill -0 "$service_pid" >/dev/null 2>&1 && (( $(date +%s) < deadline )); do
    [[ "$(backend_runtime_process_start_marker "$service_pid")" == "$start_marker" ]] || return 1
    sleep 1
  done
  if ! kill -0 "$service_pid" >/dev/null 2>&1; then
    return 0
  fi
  echo "Force stopping owned ${process_label} pid=${service_pid}"
  signal_owned_process "$service_pid" "$start_marker" KILL || return 1
  sleep 1
  ! kill -0 "$service_pid" >/dev/null 2>&1
}

collect_owned_descendants() {
  local parent_pid="$1"
  command -v pgrep >/dev/null 2>&1 || return 1
  local child_pid child_marker
  local children
  local pgrep_status=0
  children="$(pgrep -P "$parent_pid" 2>/dev/null)" || pgrep_status=$?
  (( pgrep_status == 0 || pgrep_status == 1 )) || return 1
  while IFS= read -r child_pid; do
    [[ -n "$child_pid" ]] || continue
    [[ "$child_pid" =~ ^[0-9]+$ ]] || return 1
    child_marker="$(backend_runtime_process_start_marker "$child_pid")"
    [[ -n "$child_marker" ]] || return 1
    printf '%s|%s\n' "$child_pid" "$child_marker"
    collect_owned_descendants "$child_pid"
  done <<<"$children"
}

terminate_owned_launcher_tree() {
  local launcher_pid="$1"
  local launcher_marker="$2"
  local process_label="$3"
  local descendants_output
  if ! descendants_output="$(collect_owned_descendants "$launcher_pid")"; then
    echo "Cannot safely inspect child processes for ${process_label}; refusing termination." >&2
    return 1
  fi

  local descendant_pid descendant_marker
  local descendants=()
  while IFS='|' read -r descendant_pid descendant_marker; do
    [[ -n "$descendant_pid" ]] || continue
    descendants+=("${descendant_pid}|${descendant_marker}")
  done <<<"$descendants_output"

  echo "Stopping owned ${process_label} pid=${launcher_pid}"
  for descendant in "${descendants[@]}"; do
    IFS='|' read -r descendant_pid descendant_marker <<<"$descendant"
    [[ "$(backend_runtime_process_start_marker "$descendant_pid")" == "$descendant_marker" ]] || return 1
    kill -TERM "$descendant_pid" >/dev/null 2>&1 || true
  done
  signal_owned_process "$launcher_pid" "$launcher_marker" TERM || return 1

  local deadline=$(( $(date +%s) + term_grace_seconds ))
  while kill -0 "$launcher_pid" >/dev/null 2>&1 && (( $(date +%s) < deadline )); do
    [[ "$(backend_runtime_process_start_marker "$launcher_pid")" == "$launcher_marker" ]] || return 1
    sleep 1
  done

  for descendant in "${descendants[@]}"; do
    IFS='|' read -r descendant_pid descendant_marker <<<"$descendant"
    if kill -0 "$descendant_pid" >/dev/null 2>&1 && [[ "$(backend_runtime_process_start_marker "$descendant_pid")" == "$descendant_marker" ]]; then
      kill -KILL "$descendant_pid" >/dev/null 2>&1 || true
    fi
  done
  if kill -0 "$launcher_pid" >/dev/null 2>&1; then
    signal_owned_process "$launcher_pid" "$launcher_marker" KILL || return 1
  fi
  sleep 1
  ! kill -0 "$launcher_pid" >/dev/null 2>&1
}

stop_service() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local launcher_name="$4"
  local ownership_problem=0

  if load_owned_java "$service_name" "$module_name" "$service_port"; then
    terminate_owned_process "$owned_java_pid" "$owned_java_marker" "$service_name service" || ownership_problem=1
  else
    [[ "$?" -eq 1 ]] || ownership_problem=1
  fi
  if load_owned_launcher "$service_name" "$launcher_name"; then
    terminate_owned_launcher_tree "$owned_launcher_pid" "$owned_launcher_marker" "$service_name launcher" || ownership_problem=1
  else
    [[ "$?" -eq 1 ]] || ownership_problem=1
  fi
  return "$ownership_problem"
}

service_is_confirmed_stopped() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local launcher_name="$4"
  local state_result=0

  load_owned_java "$service_name" "$module_name" "$service_port" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  state_result=0
  load_owned_launcher "$service_name" "$launcher_name" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  if find_unmanaged_service_process "$module_name" "$service_port"; then
    echo "Unmanaged matching process remains for ${service_name}; refusing termination." >&2
    return 2
  fi
  [[ "$?" -eq 1 ]] || return 2
  return 0
}

catalog_entries=()
while IFS= read -r entry; do
  catalog_entries+=("$entry")
done < <(backend_runtime_service_catalog)

for ((index=${#catalog_entries[@]} - 1; index >= 0; index--)); do
  IFS='|' read -r service_name module_name service_port launcher_name <<<"${catalog_entries[$index]}"
  stop_service "$service_name" "$module_name" "$service_port" "$launcher_name" || true
done

services_stopped=0
local_stop_deadline=$(( $(date +%s) + stop_timeout_seconds ))
while :; do
  services_stopped=1
  while IFS='|' read -r service_name module_name service_port launcher_name; do
    if ! service_is_confirmed_stopped "$service_name" "$module_name" "$service_port" "$launcher_name"; then
      services_stopped=0
    else
      remove_backend_runtime_state "$repo_root" "$service_name"
    fi
  done < <(backend_runtime_service_catalog)
  [[ "$services_stopped" -eq 1 ]] && break
  (( $(date +%s) >= local_stop_deadline )) && break
  sleep 1
done

if [[ "$services_stopped" -ne 1 ]]; then
  echo "Timed out waiting for owned backend processes to stop safely." >&2
fi

nacos_stopped=1
if [[ "$runtime_config_mode" == "nacos" ]]; then
  nacos_stopped=0
  nacos_stop_deadline=$(( $(date +%s) + stop_timeout_seconds ))
  nacos_checker_script="${CDD_RUNTIME_NACOS_CHECK_SCRIPT:-$repo_root/scripts/nacos/check_nacos_state.sh}"
  while (( $(date +%s) < nacos_stop_deadline )); do
    if CDD_NACOS_DEADLINE_EPOCH="$nacos_stop_deadline" bash "$nacos_checker_script" "$runtime_env" stopped; then
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
