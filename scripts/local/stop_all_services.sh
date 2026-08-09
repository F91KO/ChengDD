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
runtime_catalog="$(backend_runtime_service_catalog)"
local_stop_deadline=$(( $(date +%s) + stop_timeout_seconds ))

owned_java_pid=""
owned_java_marker=""
owned_java_port=""
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
  owned_java_port=""

  [[ -f "$state_file" ]] || return 1
  if ! read_backend_runtime_state "$state_file"; then
    echo "Refusing malformed runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  local expected_jar_path="$repo_root/cdd-parent/${module_name}/target/${module_name}-0.1.0-SNAPSHOT.jar"
  if [[ "$RUNTIME_STATE_SERVICE_NAME" != "$service_name" || "$RUNTIME_STATE_MODULE_NAME" != "$module_name" || "$RUNTIME_STATE_JAR_PATH" != "$expected_jar_path" ]]; then
    echo "Refusing mismatched runtime state for ${service_name}: ${state_file}" >&2
    return 2
  fi
  if ! kill -0 "$RUNTIME_STATE_SERVICE_PID" >/dev/null 2>&1; then
    return 1
  fi
  if ! backend_runtime_process_matches_state "$service_name" "$module_name" "$RUNTIME_STATE_SERVICE_PORT" "$RUNTIME_STATE_SERVICE_PID" "$RUNTIME_STATE_PROCESS_START_MARKER" "$RUNTIME_STATE_JAVA_PATH" "$RUNTIME_STATE_JAR_PATH"; then
    echo "Refusing unowned runtime process for ${service_name} pid=${RUNTIME_STATE_SERVICE_PID}." >&2
    return 2
  fi
  owned_java_pid="$RUNTIME_STATE_SERVICE_PID"
  owned_java_marker="$RUNTIME_STATE_PROCESS_START_MARKER"
  owned_java_port="$RUNTIME_STATE_SERVICE_PORT"
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

service_is_confirmed_stopped() {
  local service_name="$1"
  local module_name="$2"
  local service_port="$3"
  local launcher_name="$4"
  local state_result=0
  local recorded_port="$service_port"

  load_owned_java "$service_name" "$module_name" "$service_port" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  if [[ -n "${RUNTIME_STATE_SERVICE_PORT:-}" ]]; then
    recorded_port="$RUNTIME_STATE_SERVICE_PORT"
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  state_result=0
  load_owned_launcher "$service_name" "$launcher_name" || state_result=$?
  if [[ "$state_result" -eq 0 ]]; then
    return 1
  fi
  [[ "$state_result" -eq 1 ]] || return 2

  local unmanaged_result=0
  find_unmanaged_service_process "$module_name" "$recorded_port" || unmanaged_result=$?
  if [[ "$unmanaged_result" -eq 0 ]]; then
    echo "Unmanaged matching process remains for ${service_name}; refusing termination." >&2
    return 2
  fi
  [[ "$unmanaged_result" -eq 1 ]] && return 0
  return 2
}

catalog_entries=()
while IFS= read -r entry; do
  catalog_entries+=("$entry")
done <<<"$runtime_catalog"

shutdown_target_pids=()
shutdown_target_markers=()
shutdown_target_labels=()
shutdown_target_seen='|'
shutdown_safety_problem=0

add_shutdown_target() {
  local target_pid="$1"
  local target_marker="$2"
  local target_label="$3"
  if [[ "$shutdown_target_seen" == *"|${target_pid}|"* ]]; then
    local existing_index
    for ((existing_index=0; existing_index<${#shutdown_target_pids[@]}; existing_index++)); do
      if [[ "${shutdown_target_pids[$existing_index]}" == "$target_pid" && "${shutdown_target_markers[$existing_index]}" != "$target_marker" ]]; then
        return 1
      fi
    done
    return 0
  fi
  shutdown_target_seen+="${target_pid}|"
  shutdown_target_pids+=("$target_pid")
  shutdown_target_markers+=("$target_marker")
  shutdown_target_labels+=("$target_label")
}

for ((index=${#catalog_entries[@]} - 1; index >= 0; index--)); do
  IFS='|' read -r service_name module_name service_port launcher_name <<<"${catalog_entries[$index]}"
  load_result=0
  load_owned_java "$service_name" "$module_name" "$service_port" || load_result=$?
  if [[ "$load_result" -eq 0 ]]; then
    add_shutdown_target "$owned_java_pid" "$owned_java_marker" "$service_name service" || shutdown_safety_problem=1
  elif [[ "$load_result" -ne 1 ]]; then
    shutdown_safety_problem=1
  fi

  load_result=0
  load_owned_launcher "$service_name" "$launcher_name" || load_result=$?
  if [[ "$load_result" -eq 0 ]]; then
    descendants_output="$(backend_runtime_collect_owned_descendants "$owned_launcher_pid")" || {
      echo "Cannot safely snapshot descendants for ${service_name} launcher." >&2
      shutdown_safety_problem=1
      descendants_output=""
    }
    while IFS='|' read -r descendant_pid descendant_marker; do
      [[ -n "$descendant_pid" ]] || continue
      add_shutdown_target "$descendant_pid" "$descendant_marker" "$service_name launcher child" || shutdown_safety_problem=1
    done <<<"$descendants_output"
    add_shutdown_target "$owned_launcher_pid" "$owned_launcher_marker" "$service_name launcher" || shutdown_safety_problem=1
  elif [[ "$load_result" -ne 1 ]]; then
    shutdown_safety_problem=1
  fi
done

# Phase 1: signal every proven target promptly in reverse service order.
if [[ ${#shutdown_target_pids[@]} -gt 0 ]]; then
for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
  target_pid="${shutdown_target_pids[$target_index]}"
  target_marker="${shutdown_target_markers[$target_index]}"
  if ! backend_runtime_signal_exact_process "$target_pid" "$target_marker" TERM; then
    shutdown_safety_problem=1
  fi
done

# Phase 2: one shared TERM grace, with one second reserved for KILL verification.
term_deadline=$(( $(date +%s) + term_grace_seconds ))
kill_reserve_deadline=$(( local_stop_deadline - 1 ))
(( term_deadline < kill_reserve_deadline )) || term_deadline="$kill_reserve_deadline"
while (( $(date +%s) < term_deadline )); do
  any_alive=0
  for target_pid in "${shutdown_target_pids[@]}"; do
    kill -0 "$target_pid" >/dev/null 2>&1 && any_alive=1
  done
  [[ "$any_alive" -eq 0 ]] && break
  sleep 0.1
done

# Phase 3: KILL every exact survivor, then verify against the same global deadline.
for ((target_index=0; target_index<${#shutdown_target_pids[@]}; target_index++)); do
  target_pid="${shutdown_target_pids[$target_index]}"
  target_marker="${shutdown_target_markers[$target_index]}"
  if kill -0 "$target_pid" >/dev/null 2>&1; then
    if (( $(date +%s) >= local_stop_deadline )) || ! backend_runtime_signal_exact_process "$target_pid" "$target_marker" KILL; then
      shutdown_safety_problem=1
    fi
  fi
done
while (( $(date +%s) < local_stop_deadline )); do
  survivors=0
  for target_pid in "${shutdown_target_pids[@]}"; do
    kill -0 "$target_pid" >/dev/null 2>&1 && survivors=1
  done
  [[ "$survivors" -eq 0 ]] && break
  sleep 0.1
done
for target_pid in "${shutdown_target_pids[@]}"; do
  kill -0 "$target_pid" >/dev/null 2>&1 && shutdown_safety_problem=1
done
fi

services_stopped=0
while :; do
  services_stopped=1
  while IFS='|' read -r service_name module_name service_port launcher_name; do
    if ! service_is_confirmed_stopped "$service_name" "$module_name" "$service_port" "$launcher_name"; then
      services_stopped=0
    else
      remove_backend_runtime_state "$repo_root" "$service_name"
    fi
  done <<<"$runtime_catalog"
  [[ "$services_stopped" -eq 1 ]] && break
  (( $(date +%s) >= local_stop_deadline )) && break
  sleep 1
done

if [[ "$services_stopped" -ne 1 || "$shutdown_safety_problem" -ne 0 ]]; then
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

if [[ "$services_stopped" -ne 1 || "$shutdown_safety_problem" -ne 0 || "$nacos_stopped" -ne 1 ]]; then
  exit 1
fi

echo "All backend services are stopped."
